package io.wit117.thoughtweave.domain.model

/**
 * What one dictation session reports: [Partial] text while listening and a [Recognized] piece whenever one is
 * done. A session can recognize several pieces (it keeps listening through pauses) and ends when its flow
 * completes, possibly after a [Failed].
 */
sealed interface SpeechEvent {
    /** Text of the piece being recognized; replaces the previous partial text. */
    data class Partial(val text: String) : SpeechEvent

    /** A finished piece of text, to insert now. Never blank. */
    data class Recognized(val text: String) : SpeechEvent

    data class Failed(val error: SpeechError) : SpeechEvent
}

enum class SpeechError {
    /** Nothing was heard, or nothing could be recognized. Not reported once something was recognized. */
    NO_SPEECH,

    /** The language is not supported or not installed for on-device recognition. */
    LANGUAGE_UNAVAILABLE,

    PERMISSION_DENIED,

    /** Another app or session is using the recognizer. */
    BUSY,

    OTHER,
}

/** Whether the phone's on-device recognizer can recognize a language. */
enum class SpeechLanguageState {
    INSTALLED,

    /** The system is downloading it or has scheduled the download. */
    DOWNLOADING,

    /** Supported, not installed: a download can be requested. */
    DOWNLOADABLE,

    UNSUPPORTED,
}

/** Progress of a speech-language download, which the system performs. */
sealed interface SpeechDownloadEvent {
    /** Android 13 reports nothing after the request; the language state shows the outcome. */
    data object Requested : SpeechDownloadEvent

    /** Percent, 0–100. */
    data class Progress(val percent: Int) : SpeechDownloadEvent

    /** The system will download later, for example on Wi-Fi. No further updates follow. */
    data object Scheduled : SpeechDownloadEvent

    data object Done : SpeechDownloadEvent

    data object Failed : SpeechDownloadEvent
}
