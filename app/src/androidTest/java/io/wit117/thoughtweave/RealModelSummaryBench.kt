package io.wit117.thoughtweave

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.data.ai.engine.GemmaInferenceEngine
import io.wit117.thoughtweave.data.ai.model.ModelFileProvider
import io.wit117.thoughtweave.data.repository.MemoSynthesisRepositoryImpl
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Baseline A of the Summarization 200M evaluation (docs/TODO.md, tools/summary-bench): runs the app's own
 * synthesis path on every benchmark set and writes the AI part (before "원문 항목") as a result file for
 * `score.py`. Skipped unless the model and `sets.json` are in /data/local/tmp. Runs per set: instrumentation
 * argument `runs` (default 3). Output: `<external files>/summary-bench/A-gemma.json`.
 */
@RunWith(AndroidJUnit4::class)
class RealModelSummaryBench {
    @Test
    fun summarizeEverySet(): Unit = runBlocking {
        assumeTrue("Push the model to $BENCH_MODEL_PATH", File(BENCH_MODEL_PATH).isFile)
        assumeTrue("Push tools/summary-bench/sets.json to $SETS_PATH", File(SETS_PATH).isFile)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val runs = InstrumentationRegistry.getArguments().getString("runs")?.toInt() ?: 3
        val engine = GemmaInferenceEngine(
            context,
            object : ModelFileProvider {
                override fun verifiedModelFile() = File(BENCH_MODEL_PATH)
                override suspend fun onEngineInitFailed() = Unit
            },
            CoroutineScope(Dispatchers.Default),
        )
        val repository = MemoSynthesisRepositoryImpl(engine)
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
                val full = runCatching {
                    withTimeout(600_000) {
                        repository.summarize(memos, AiLanguagePlan(language, language)).fold("") { a, b -> a + b }
                    }
                }
                val seconds = (SystemClock.elapsedRealtime() - start) / 1000.0
                val output = full.fold({ repository.aiPart(it, language) }, { "ERROR $it" })
                Log.i("SUMMARY_BENCH", "${set.getString("id")} run ${run + 1}/$runs ${seconds}s\n$output")
                results.put(
                    JSONObject()
                        .put("set", set.getString("id"))
                        .put("run", run + 1)
                        .put("seconds", seconds)
                        .put("output", output),
                )
            }
        }
        val out = File(context.getExternalFilesDir(null), "summary-bench/A-gemma.json")
        out.parentFile!!.mkdirs()
        out.writeText(JSONObject().put("config", "A-gemma (app synthesis path, AI part, emulator CPU)").put("runs", results).toString(2))
        Log.i("SUMMARY_BENCH", "wrote ${out.absolutePath}")
    }
}
