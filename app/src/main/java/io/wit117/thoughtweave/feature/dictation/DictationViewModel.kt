package io.wit117.thoughtweave.feature.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.SpeechError
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import io.wit117.thoughtweave.domain.usecase.ResolveAiLanguageUseCase
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictationUiState(
    /** False below Android 12 or without an on-device recognizer: the mic is hidden. */
    val isSupported: Boolean,
    val isListening: Boolean = false,
    /** The piece being recognized, shown while listening; cleared once the piece is inserted. */
    val partialText: String = "",
)

sealed interface DictationEffect {
    /** Recognized text, to insert at the content field's cursor. */
    data class Insert(val text: String) : DictationEffect

    /** [canDownload]: the phone can download speech languages, in S06. */
    data class Failed(val error: SpeechError, val language: AppLanguage, val canDownload: Boolean) : DictationEffect
}

/**
 * Voice dictation for S02-A/B. One session at a time; it keeps listening through pauses until the user
 * stops or stays silent, inserting each recognized piece. Nothing is saved automatically.
 */
@HiltViewModel
class DictationViewModel @Inject constructor(
    private val speech: SpeechInputRepository,
    private val resolveLanguage: ResolveAiLanguageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictationUiState(isSupported = speech.isSupported()))
    val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<DictationEffect>(Channel.BUFFERED)
    val effects: Flow<DictationEffect> = effectChannel.receiveAsFlow()

    private var session: SpeechSession? = null
    private var job: Job? = null

    /**
     * Listens in the AI input language when one is set, otherwise the app language: there is no
     * text yet to detect a language from.
     */
    fun start() {
        if (!_uiState.value.isSupported || job?.isActive == true) return
        val language = resolveLanguage("").input
        val started = speech.session(language.speechTag)
        session = started
        _uiState.update { it.copy(isListening = true, partialText = "") }
        job = viewModelScope.launch {
            try {
                started.events.collect { event ->
                    when (event) {
                        is SpeechEvent.Partial -> _uiState.update { it.copy(partialText = event.text) }
                        is SpeechEvent.Recognized -> {
                            _uiState.update { it.copy(partialText = "") }
                            effectChannel.send(DictationEffect.Insert(event.text))
                        }
                        is SpeechEvent.Failed -> effectChannel.send(
                            DictationEffect.Failed(event.error, language, speech.canManageLanguages()),
                        )
                    }
                }
            } finally {
                session = null
                _uiState.update { it.copy(isListening = false, partialText = "") }
            }
        }
    }

    /** Stops listening; what was heard so far is still inserted. */
    fun stop() {
        session?.stop()
    }
}
