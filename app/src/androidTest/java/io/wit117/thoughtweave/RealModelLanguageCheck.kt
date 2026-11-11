package io.wit117.thoughtweave

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.data.ai.engine.GemmaInferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.language.LanguageProfile
import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.data.ai.model.ModelFileProvider
import io.wit117.thoughtweave.data.ai.parse.withoutInventedYears
import io.wit117.thoughtweave.data.ai.prompt.TITLE_TEMPERATURE
import io.wit117.thoughtweave.data.ai.prompt.scheduleTitlePrompt
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Where the model is pushed for this check: `adb push <model> /data/local/tmp/`. */
private const val MODEL_PATH = "/data/local/tmp/gemma3-1b-it-int4.litertlm"

private fun memo(title: String, content: String) =
    Memo(1, title, content, Instant.EPOCH, Instant.EPOCH, false, MemoOrigin.MANUAL)

/**
 * Sample memos per language: two summary sets (one with questions) and one title memo. Each language
 * needs the same facts so results can be compared across languages.
 */
private val SAMPLES: Map<String, Pair<List<List<Memo>>, String>> = mapOf(
    "en" to (
        listOf(
            listOf(
                memo("Launch", "The new app launches on December 15. Kim owns the marketing material."),
                memo("Checklist", "Prepare the store listing and get a legal review before launch."),
            ),
            listOf(
                memo("Gift", "What should I buy for mom's birthday? Flowers or a scarf. Budget 50,000 won."),
                memo("Dinner", "Book a table for Saturday evening. Korean or Western food?"),
            ),
        ) to "Dentist appointment tomorrow at 3pm"
        ),
    "ko" to (
        listOf(
            listOf(
                memo("출시", "신규 앱은 12월 15일에 출시한다. 김대리가 마케팅 자료를 담당한다."),
                memo("체크리스트", "출시 전에 스토어 등록 정보를 준비하고 법률 검토를 받는다."),
            ),
            listOf(
                memo("선물", "엄마 생신 선물로 뭐가 좋을까? 꽃이나 스카프. 예산 5만원."),
                memo("저녁", "토요일 저녁 식당 예약. 한식이 나을까 양식이 나을까?"),
            ),
        ) to "내일 오후 3시 치과 예약"
        ),
)

/**
 * Manual check on the real model, for adding a language or changing the model (docs/ADDING_A_LANGUAGE.md).
 * Skipped unless the model is at [MODEL_PATH]. Run only this class and read the `LANGUAGE_CHECK` log:
 * every summary must keep all facts and dates, keep questions open, and be in the output language.
 */
@RunWith(AndroidJUnit4::class)
class RealModelLanguageCheck {
    private suspend fun Flow<String>.text() = withTimeout(180_000) { fold("") { a, b -> a + b } }

    @Test
    fun printEveryLanguageAndPair() = runBlocking {
        assumeTrue("Push the model to $MODEL_PATH to run this check", File(MODEL_PATH).isFile)
        val engine = GemmaInferenceEngine(
            InstrumentationRegistry.getInstrumentation().targetContext,
            object : ModelFileProvider {
                override fun verifiedModelFile() = File(MODEL_PATH)
                override suspend fun onEngineInitFailed() = Unit
            },
            CoroutineScope(Dispatchers.Default),
        )
        val withSamples = LanguageProfiles.all.filter { it.language.tag in SAMPLES }
        for (input in withSamples.filter { it.dateRules != null }) {
            for (output in withSamples.filter { it.prompts != null }) {
                check(engine, input, output)
            }
        }
    }

    private suspend fun check(engine: GemmaInferenceEngine, input: LanguageProfile, output: LanguageProfile) {
        val pair = "${input.language.tag}->${output.language.tag}"
        val (summarySets, titleMemo) = SAMPLES.getValue(input.language.tag)
        summarySets.forEachIndexed { index, memos ->
            val source = memos.joinToString("\n") { it.title + "\n" + it.content }
            val reply = runCatching {
                engine.generate(summaryPrompt(memos, input, output)).withoutInventedYears(source).text()
            }
            Log.i("LANGUAGE_CHECK", "$pair summary #$index\n  memos: $source\n  reply: ${reply.getOrElse { "ERROR $it" }}")
        }
        val title = runCatching {
            engine.generate(InferencePrompt(scheduleTitlePrompt("", titleMemo, input, output), TITLE_TEMPERATURE)).text()
        }
        Log.i("LANGUAGE_CHECK", "$pair title\n  memo: $titleMemo\n  reply: ${title.getOrElse { "ERROR $it" }}")
    }
}
