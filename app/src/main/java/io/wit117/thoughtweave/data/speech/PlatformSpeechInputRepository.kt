package io.wit117.thoughtweave.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechError
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** How long a dictation session waits through silence before it ends (segmented sessions, Android 13+). */
private const val SILENCE_TO_END_MILLIS = 10_000L

/** The recognizer normally answers a support check at once; this only guards against no answer. */
private const val SUPPORT_CHECK_TIMEOUT_MILLIS = 5_000L

/**
 * The only class that imports `android.speech`. Uses the system's on-device recognizer
 * (`createOnDeviceSpeechRecognizer`, API 31), never the network one, so audio stays on the device.
 * Every SpeechRecognizer call runs on the main thread, as the platform requires.
 */
class PlatformSpeechInputRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechInputRepository {

    override fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun canManageLanguages(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSupported()

    override suspend fun languageStates(languageTags: List<String>): Map<String, SpeechLanguageState>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !isSupported()) return null
        val lists = withContext(Dispatchers.Main) {
            withTimeoutOrNull(SUPPORT_CHECK_TIMEOUT_MILLIS) { checkSupport() }
        } ?: return null
        return languageTags.associateWith(lists::stateOf)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun checkSupport(): SpeechSupportLists? = suspendCancellableCoroutine { continuation ->
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer.setRecognitionListener(IgnoringListener)
        var destroyed = false
        fun finish(result: SpeechSupportLists?) {
            if (destroyed) return
            destroyed = true
            recognizer.destroy()
            if (continuation.isActive) continuation.resume(result)
        }
        recognizer.checkRecognitionSupport(
            recognizeIntent(languageTag = null),
            context.mainExecutor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(recognitionSupport: RecognitionSupport) = finish(
                    SpeechSupportLists(
                        installed = recognitionSupport.installedOnDeviceLanguages,
                        pending = recognitionSupport.pendingOnDeviceLanguages,
                        supported = recognitionSupport.supportedOnDeviceLanguages,
                    ),
                )

                override fun onError(error: Int) = finish(null)
            },
        )
        continuation.invokeOnCancellation { context.mainExecutor.execute { finish(null) } }
    }

    override fun download(languageTag: String): Flow<SpeechDownloadEvent> = when {
        !isSupported() -> flowOf(SpeechDownloadEvent.Failed)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> downloadWithProgress(languageTag)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> downloadRequestOnly(languageTag)
        else -> emptyFlow()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun downloadWithProgress(languageTag: String): Flow<SpeechDownloadEvent> = callbackFlow {
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer.setRecognitionListener(IgnoringListener)
        recognizer.triggerModelDownload(
            recognizeIntent(languageTag),
            context.mainExecutor,
            object : ModelDownloadListener {
                override fun onProgress(completedPercent: Int) {
                    trySend(SpeechDownloadEvent.Progress(completedPercent.coerceIn(0, 100)))
                }

                override fun onSuccess() {
                    trySend(SpeechDownloadEvent.Done)
                    close()
                }

                override fun onScheduled() {
                    trySend(SpeechDownloadEvent.Scheduled)
                    close()
                }

                override fun onError(error: Int) {
                    // Some recognizers download without reporting progress; the language state shows the result.
                    trySend(
                        if (error == SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS) {
                            SpeechDownloadEvent.Requested
                        } else {
                            SpeechDownloadEvent.Failed
                        },
                    )
                    if (error != SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS) close()
                }
            },
        )
        awaitClose { recognizer.destroy() }
    }.flowOn(Dispatchers.Main)

    /** Android 13 has no download listener. The recognizer is kept until the caller stops collecting. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun downloadRequestOnly(languageTag: String): Flow<SpeechDownloadEvent> = callbackFlow {
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer.setRecognitionListener(IgnoringListener)
        recognizer.triggerModelDownload(recognizeIntent(languageTag))
        trySend(SpeechDownloadEvent.Requested)
        awaitClose { recognizer.destroy() }
    }.flowOn(Dispatchers.Main)

    /** Callers check [isSupported] first; the mic is hidden otherwise. */
    override fun session(languageTag: String): SpeechSession =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PlatformSpeechSession(context, languageTag)
        } else {
            throw UnsupportedOperationException("On-device recognition needs Android 12")
        }
}

/** A plain dictation request. Without a language the recognizer's own default applies (support checks). */
private fun recognizeIntent(languageTag: String?): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .apply { if (languageTag != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag) }

/**
 * Keeps listening through pauses. On Android 13+ it asks for a segmented session, which returns each piece
 * through `onSegmentResults` and ends after [SILENCE_TO_END_MILLIS] of silence. A recognizer that ignores
 * that request (or Android 12) returns `onResults` after each utterance instead, and listening restarts
 * until the user stops or a restart hears nothing.
 */
@RequiresApi(Build.VERSION_CODES.S)
private class PlatformSpeechSession(
    private val context: Context,
    private val languageTag: String,
) : SpeechSession {

    /** Main thread only, like every SpeechRecognizer call. */
    private var recognizer: SpeechRecognizer? = null
    private var stopRequested = false

    private fun dictationIntent(): Intent = recognizeIntent(languageTag)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(
                    RecognizerIntent.EXTRA_SEGMENTED_SESSION,
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                )
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TO_END_MILLIS)
            }
        }

    override val events: Flow<SpeechEvent> = callbackFlow {
        val created = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = created
        var recognizedAny = false

        fun recognized(bundle: Bundle?): Boolean {
            val text = topResult(bundle)?.takeIf { it.isNotBlank() } ?: return false
            recognizedAny = true
            trySend(SpeechEvent.Recognized(text))
            return true
        }

        /** Silence after some speech, or a stop, is a normal end rather than an error. */
        fun endQuietlyOrFail(error: SpeechError) {
            if (error != SpeechError.NO_SPEECH || !(recognizedAny || stopRequested)) {
                trySend(SpeechEvent.Failed(error))
            }
            close()
        }

        created.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                topResult(partialResults)?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onSegmentResults(segmentResults: Bundle) {
                recognized(segmentResults)
            }

            override fun onEndOfSegmentedSession() = endQuietlyOrFail(SpeechError.NO_SPEECH)

            override fun onResults(results: Bundle?) {
                when {
                    !recognized(results) -> endQuietlyOrFail(SpeechError.NO_SPEECH)
                    stopRequested -> close()
                    !isClosedForSend -> created.startListening(dictationIntent())
                }
            }

            override fun onError(error: Int) = endQuietlyOrFail(speechError(error))

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        created.startListening(dictationIntent())
        awaitClose {
            recognizer = null
            created.cancel()
            created.destroy()
        }
    }.flowOn(Dispatchers.Main)

    override fun stop() {
        stopRequested = true
        recognizer?.stopListening()
    }
}

/** For recognizers used only to check support or download: the platform wants a listener set first. */
private object IgnoringListener : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onError(error: Int) = Unit
    override fun onResults(results: Bundle?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

private fun topResult(bundle: Bundle?): String? =
    bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

private fun speechError(error: Int): SpeechError = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_SPEECH
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        SpeechError.LANGUAGE_UNAVAILABLE
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION_DENIED
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> SpeechError.BUSY
    else -> SpeechError.OTHER
}
