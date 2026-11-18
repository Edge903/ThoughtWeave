package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import kotlinx.coroutines.flow.Flow

/** Voice dictation, recognized on the device only. Audio never leaves the device. */
interface SpeechInputRepository {
    /** False below Android 12 or when the device has no on-device recognizer; the mic is then hidden. */
    fun isSupported(): Boolean

    /** True where language states can be read and downloads requested (Android 13 and later). */
    fun canManageLanguages(): Boolean

    /**
     * The state of each of [languageTags] (BCP 47, such as "ko-KR"), or null when the recognizer cannot
     * tell (Android 12, or it does not answer).
     */
    suspend fun languageStates(languageTags: List<String>): Map<String, SpeechLanguageState>?

    /**
     * Asks the system to download on-device recognition for [languageTag]. The system may ask the user to
     * approve it. Completes after [SpeechDownloadEvent.Done], [SpeechDownloadEvent.Scheduled] or
     * [SpeechDownloadEvent.Failed]; after [SpeechDownloadEvent.Requested] it stays open until cancelled.
     */
    fun download(languageTag: String): Flow<SpeechDownloadEvent>

    /**
     * Prepares a session for [languageTag]. Listening starts when [SpeechSession.events] is collected and
     * ends when the flow completes (after a long silence or [SpeechSession.stop]) or its collector is cancelled.
     */
    fun session(languageTag: String): SpeechSession
}

interface SpeechSession {
    /** Collect once. */
    val events: Flow<SpeechEvent>

    /** Stops listening; what was heard so far is still recognized and reported. */
    fun stop()
}
