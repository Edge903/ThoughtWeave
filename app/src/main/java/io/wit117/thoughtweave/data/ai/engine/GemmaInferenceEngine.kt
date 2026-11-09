package io.wit117.thoughtweave.data.ai.engine

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.ModelInfo
import com.google.ai.edge.litertlm.ModelType
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.data.ai.model.ModelFileProvider
import io.wit117.thoughtweave.data.di.ApplicationScope
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "GemmaInferenceEngine"
private const val TOP_K = 40
private const val TOP_P = 0.95
private const val IDLE_RELEASE_MILLIS = 60_000L

/** Repetitive input made the model run for minutes on CPU, so replies are capped. */
private const val MAX_OUTPUT_TOKENS = 512

/** The only class that imports LiteRT-LM. Runs one request at a time and frees memory when idle. */
@Singleton
class GemmaInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelFiles: ModelFileProvider,
    @ApplicationScope private val scope: CoroutineScope,
) : InferenceEngine {

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var idleRelease: Job? = null

    override fun generate(prompt: InferencePrompt): Flow<String> = flow {
        mutex.withLock {
            idleRelease?.cancel()
            try {
                val current = engine ?: load().also { engine = it }
                val config = ConversationConfig(
                    systemInstruction = prompt.systemInstruction?.let { Contents.of(it) },
                    initialMessages = prompt.examples.flatMap {
                        listOf(Message.user(it.input), Message.model(it.output))
                    },
                    samplerConfig = SamplerConfig(topK = TOP_K, topP = TOP_P, temperature = prompt.temperature),
                    maxOutputToken = MAX_OUTPUT_TOKENS,
                )
                current.createConversation(config).use { conversation ->
                    conversation.sendMessageAsync(prompt.text).collect { emit(it.toString()) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: InferenceException) {
                throw e
            } catch (e: Exception) {
                val isTooLong = e.message?.contains("too long", ignoreCase = true) == true
                throw InferenceException("Generation failed", e, isTooLong)
            } finally {
                scheduleRelease()
            }
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun load(): Engine {
        val file = modelFiles.verifiedModelFile() ?: throw InferenceException("The model is not ready")
        for (backend in candidateBackends()) {
            val loaded = tryLoad(file, backend)
            if (loaded != null) {
                Log.i(TAG, "Loaded on ${backend.name}")
                return loaded
            }
        }
        modelFiles.onEngineInitFailed()
        throw InferenceException("The engine could not be initialized")
    }

    /** The x86_64 emulator has no usable GPU path. Real devices try GPU, then fall back to CPU. */
    private fun candidateBackends(): List<Backend> =
        if (Build.SUPPORTED_ABIS.first().startsWith("x86")) {
            listOf(Backend.CPU())
        } else {
            listOf(Backend.GPU(), Backend.CPU())
        }

    private fun tryLoad(file: File, backend: Backend): Engine? {
        var candidate: Engine? = null
        return try {
            logAndCheckModel(file)
            candidate = Engine(
                EngineConfig(modelPath = file.path, backend = backend, cacheDir = context.cacheDir.path),
            )
            candidate.initialize()
            candidate
        } catch (e: Exception) {
            Log.w(TAG, "Could not load on ${backend.name}", e)
            candidate?.close()
            null
        } catch (e: LinkageError) {
            Log.w(TAG, "Native library could not be loaded", e)
            null
        }
    }

    /** `ModelInfo` is undocumented, so only a definite non-LLM answer rejects the file. */
    private fun logAndCheckModel(file: File) {
        ModelInfo.from(file.path).use { info ->
            Log.i(TAG, "Model type ${info.modelType}, context ${info.maxContextTokens()} tokens")
            check(info.modelType == ModelType.LLM || info.modelType == ModelType.UNKNOWN) {
                "Not a text generation model"
            }
        }
    }

    private fun scheduleRelease() {
        idleRelease = scope.launch {
            delay(IDLE_RELEASE_MILLIS)
            mutex.withLock {
                engine?.close()
                engine = null
            }
        }
    }
}
