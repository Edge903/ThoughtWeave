package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

/** Voice dictation, recognized on the device only. Audio never leaves the device. */
interface SpeechInputRepository {
    /** False below Android 12 or when the device has no on-device recognizer; the mic is then hidden. */
    fun isSupported(): Boolean

    /**
     * Prepares a session for [languageTag] (BCP 47, such as "ko-KR"). Listening starts when
     * [SpeechSession.events] is collected and ends when the flow completes or its collector is cancelled.
     */
    fun session(languageTag: String): SpeechSession
}

interface SpeechSession {
    /** Collect once. Completes after a [SpeechEvent.Final] or [SpeechEvent.Failed]. */
    val events: Flow<SpeechEvent>

    /** Stops listening and asks for the result of what was heard so far. */
    fun stop()
}
