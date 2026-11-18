package io.wit117.thoughtweave.domain.model

/** What one dictation session reports, in order: any number of [Partial], then [Final] or [Failed]. */
sealed interface SpeechEvent {
    /** Text recognized so far; replaces the previous partial text. */
    data class Partial(val text: String) : SpeechEvent

    /** The recognized text of the whole session. Never blank; blank results arrive as [Failed]. */
    data class Final(val text: String) : SpeechEvent

    data class Failed(val error: SpeechError) : SpeechEvent
}

enum class SpeechError {
    /** Nothing was heard, or nothing could be recognized. */
    NO_SPEECH,

    /** The language is not supported or not installed for on-device recognition. */
    LANGUAGE_UNAVAILABLE,

    PERMISSION_DENIED,

    /** Another app or session is using the recognizer. */
    BUSY,

    OTHER,
}
