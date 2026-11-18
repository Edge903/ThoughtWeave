package io.wit117.thoughtweave.data.speech

import io.wit117.thoughtweave.domain.model.SpeechLanguageState

/** The language lists of `android.speech.RecognitionSupport`, kept free of Android types for unit tests. */
class SpeechSupportLists(
    val installed: List<String>,
    val pending: List<String>,
    val supported: List<String>,
) {
    /**
     * Matches the full tag ("ko-KR"), ignoring case and "_" versus "-": the recognizer is asked for
     * exactly this tag, so another region of the same language does not count.
     */
    fun stateOf(tag: String): SpeechLanguageState = when {
        installed.containsTag(tag) -> SpeechLanguageState.INSTALLED
        pending.containsTag(tag) -> SpeechLanguageState.DOWNLOADING
        supported.containsTag(tag) -> SpeechLanguageState.DOWNLOADABLE
        else -> SpeechLanguageState.UNSUPPORTED
    }
}

private fun normalized(tag: String) = tag.trim().replace('_', '-').lowercase()

private fun List<String>.containsTag(tag: String) = any { normalized(it) == normalized(tag) }
