package io.wit117.thoughtweave

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.data.ai.engine.GemmaInferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.engine.PromptExample
import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.data.ai.model.ModelFileProvider
import io.wit117.thoughtweave.data.ai.parse.factList
import io.wit117.thoughtweave.data.ai.parse.withoutInventedYears
import io.wit117.thoughtweave.data.ai.prompt.SUMMARY_TEMPERATURE
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
import io.wit117.thoughtweave.data.repository.MemoSynthesisRepositoryImpl
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val BENCH_MODEL_PATH = "/data/local/tmp/gemma3-1b-it-int4.litertlm"
private const val SETS_PATH = "/data/local/tmp/summary-bench/sets.json"

/**
 * Bench-only prompts for configuration C (fact extraction per memo). Not used by the app. The example avoids
 * relative dates, which the model copied from the app's former summary example ("다음 주 수요일").
 */
private val EXTRACTION = mapOf(
    "ko" to Pair(
        "아래 메모 하나에 있는 사실을 한 줄에 하나씩 \"- \"로 시작해 나열하세요. 모든 날짜, 시간, 숫자, 금액, 이름, " +
            "장소, 할 일, 질문을 메모에 쓰인 그대로 옮기세요. 질문은 질문으로 남기고 답하지 마세요. " +
            "메모에 없는 내용이나 연도는 추가하지 마세요. 목록만 출력하세요.",
        PromptExample(
            input = "메모:\n팀 점심\n3.4 수요일 점심은 어디로 갈까? 국수집도 괜찮을 듯. 비용은 20만원 이내로.",
            output = "- 3.4 수요일 팀 점심 장소는 어디로 갈까?\n- 국수집이 후보\n- 점심 비용은 20만원 이내",
        ),
    ),
    "en" to Pair(
        "List the facts in the memo below, one per line, each starting with \"- \". Copy every date, time, " +
            "number, amount, name, place, to-do and question exactly as written. Keep questions as questions and " +
            "never answer them. Add nothing that is not in the memo, including years. Output only the list.",
        PromptExample(
            input = "Memo:\nTeam lunch\nWhere should we go for lunch on 3/4 (Wed)? Maybe the noodle place. Keep it under 200 dollars.",
            output = "- Where should the team go for lunch on 3/4 (Wed)?\n- The noodle place is an option\n- Lunch must stay under 200 dollars",
        ),
    ),
)

/**
 * Summary benchmark (tools/summary-bench, CLAUDE.md Inference). Instrumentation argument `config`:
 * - `A` (default): the app's synthesis path; the AI part (before "원문 항목") is scored.
 * - `C`: Gemma lists each memo's facts, then the app's summary prompt merges those lists.
 * - `D`: the code's sentence list ([factList]) of each memo, as bullets, is merged by the app's summary prompt.
 * Skipped unless the model and `sets.json` are in /data/local/tmp. Argument `runs` (default 1; Gemma's runs were
 * identical). Output: `<external files>/summary-bench/<config>-gemma.json`.
 */
@RunWith(AndroidJUnit4::class)
class RealModelSummaryBench {
    private suspend fun Flow<String>.text() = withTimeout(600_000) { fold("") { a, b -> a + b } }

    @Test
    fun summarizeEverySet(): Unit = runBlocking {
        assumeTrue("Push the model to $BENCH_MODEL_PATH", File(BENCH_MODEL_PATH).isFile)
        assumeTrue("Push tools/summary-bench/sets.json to $SETS_PATH", File(SETS_PATH).isFile)
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = arguments.getString("config") ?: "A"
        val runs = arguments.getString("runs")?.toInt() ?: 1
        val engine = GemmaInferenceEngine(
            context,
            object : ModelFileProvider {
                override fun verifiedModelFile() = File(BENCH_MODEL_PATH)
                override suspend fun onEngineInitFailed() = Unit
            },
            CoroutineScope(Dispatchers.Default),
        )
        val repository = MemoSynthesisRepositoryImpl(engine)

        suspend fun summarize(memos: List<Memo>, language: AppLanguage): Pair<String, String?> {
            val profile = LanguageProfiles.forLanguage(language)
            val source = memos.joinToString("\n") { it.title + "\n" + it.content }
            return when (config) {
                "A" -> repository.aiPart(
                    repository.summarize(memos, AiLanguagePlan(language, language)).text(), language,
                ) to null
                "C" -> {
                    val (system, example) = EXTRACTION.getValue(language.tag)
                    val lists = memos.map { memo ->
                        val prompt = InferencePrompt(
                            text = "${profile.prompts!!.memoHeading}\n${memo.title}\n${memo.content}",
                            temperature = SUMMARY_TEMPERATURE,
                            systemInstruction = system,
                            examples = listOf(example),
                        )
                        memo.copy(content = engine.generate(prompt).withoutInventedYears(source).text().trim())
                    }
                    val merged = engine.generate(summaryPrompt(lists, profile, profile)).withoutInventedYears(source).text()
                    merged to lists.joinToString("\n\n") { it.title + "\n" + it.content }
                }
                "D" -> {
                    val lists = memos.map { memo ->
                        memo.copy(content = factList(listOf(memo)).joinToString("\n") { "- $it" })
                    }
                    engine.generate(summaryPrompt(lists, profile, profile)).withoutInventedYears(source).text() to null
                }
                else -> error("Unknown config $config")
            }
        }

        val sets = JSONObject(File(SETS_PATH).readText()).getJSONArray("sets")
        val results = JSONArray()
        for (i in 0 until sets.length()) {
            val set = sets.getJSONObject(i)
            val language = checkNotNull(AppLanguages.forTag(set.getString("language")))
            val memoJson = set.getJSONArray("memos")
            val memos = (0 until memoJson.length()).map { m ->
                val memo = memoJson.getJSONObject(m)
                Memo(m + 1L, memo.getString("title"), memo.getString("content"), Instant.EPOCH, Instant.EPOCH, false, MemoOrigin.MANUAL)
            }
            repeat(runs) { run ->
                val start = SystemClock.elapsedRealtime()
                val result = runCatching { summarize(memos, language) }
                val seconds = (SystemClock.elapsedRealtime() - start) / 1000.0
                val (output, intermediate) = result.getOrElse { "ERROR $it" to null }
                Log.i("SUMMARY_BENCH", "$config ${set.getString("id")} run ${run + 1}/$runs ${seconds}s\n$output")
                results.put(
                    JSONObject()
                        .put("set", set.getString("id"))
                        .put("run", run + 1)
                        .put("seconds", seconds)
                        .put("output", output)
                        .apply { if (intermediate != null) put("intermediate", intermediate) },
                )
            }
        }
        val label = when (config) {
            "A" -> "A-gemma (app synthesis path, AI part, emulator CPU)"
            "C" -> "C-gemma (Gemma facts per memo, then app merge, emulator CPU)"
            else -> "D-gemma (code sentence list per memo, then app merge, emulator CPU)"
        }
        val out = File(context.getExternalFilesDir(null), "summary-bench/$config-gemma.json")
        out.parentFile!!.mkdirs()
        out.writeText(JSONObject().put("config", label).put("runs", results).toString(2))
        Log.i("SUMMARY_BENCH", "wrote ${out.absolutePath}")
    }
}
