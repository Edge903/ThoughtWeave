package io.wit117.thoughtweave.feature.dictation

import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

/** A session whose events the test sends by hand. */
internal class ScriptedSession : SpeechSession {
    val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    var stopped = false
    override val events: Flow<SpeechEvent> = channel.consumeAsFlow()
    override fun stop() {
        stopped = true
    }
}

internal class FakeSpeechInput(
    private val supported: Boolean = true,
    private val manageable: Boolean = true,
    /** Null: the support check fails. */
    var states: Map<String, SpeechLanguageState>? = emptyMap(),
) : SpeechInputRepository {
    val tags = mutableListOf<String>()
    var last: ScriptedSession? = null
    val downloads = mutableMapOf<String, Channel<SpeechDownloadEvent>>()
    var stateChecks = 0

    override fun isSupported() = supported
    override fun canManageLanguages() = supported && manageable

    override suspend fun languageStates(languageTags: List<String>): Map<String, SpeechLanguageState>? {
        stateChecks++
        return states?.filterKeys { it in languageTags }
    }

    override fun download(languageTag: String): Flow<SpeechDownloadEvent> =
        Channel<SpeechDownloadEvent>(Channel.UNLIMITED).also { downloads[languageTag] = it }.consumeAsFlow()

    override fun session(languageTag: String): SpeechSession {
        tags += languageTag
        return ScriptedSession().also { last = it }
    }
}
