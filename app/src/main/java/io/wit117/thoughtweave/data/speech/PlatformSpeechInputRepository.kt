package io.wit117.thoughtweave.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.domain.model.SpeechError
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * The only class that imports `android.speech`. Uses the system's on-device recognizer
 * (`createOnDeviceSpeechRecognizer`, API 31), never the network one, so audio stays on the device.
 */
class PlatformSpeechInputRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechInputRepository {

    override fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    /** Callers check [isSupported] first; the mic is hidden otherwise. */
    override fun session(languageTag: String): SpeechSession =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PlatformSpeechSession(context, languageTag)
        } else {
            throw UnsupportedOperationException("On-device recognition needs Android 12")
        }
}

@RequiresApi(Build.VERSION_CODES.S)
private class PlatformSpeechSession(
    private val context: Context,
    private val languageTag: String,
) : SpeechSession {

    /** Main thread only, like every SpeechRecognizer call. */
    private var recognizer: SpeechRecognizer? = null

    override val events: Flow<SpeechEvent> = callbackFlow {
        val created = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = created
        created.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                topResult(partialResults)?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = topResult(results)?.takeIf { it.isNotBlank() }
                trySend(if (text != null) SpeechEvent.Final(text) else SpeechEvent.Failed(SpeechError.NO_SPEECH))
                close()
            }

            override fun onError(error: Int) {
                trySend(SpeechEvent.Failed(speechError(error)))
                close()
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        created.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
        )
        awaitClose {
            recognizer = null
            created.cancel()
            created.destroy()
        }
    }.flowOn(Dispatchers.Main)

    override fun stop() {
        recognizer?.stopListening()
    }
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
