package io.wit117.thoughtweave.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.speech.PlatformSpeechInputRepository
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import kotlinx.coroutines.flow.Flow

/** Lets instrumented tests script dictation. Unset in the running app, and absent from release. */
object SpeechInputTestOverride {
    @Volatile
    var repository: SpeechInputRepository? = null
}

@Module
@InstallIn(SingletonComponent::class)
object BuildTypeSpeechModule {
    @Provides
    fun provideSpeechInputRepository(real: PlatformSpeechInputRepository): SpeechInputRepository =
        object : SpeechInputRepository {
            private val current get() = SpeechInputTestOverride.repository ?: real

            override fun isSupported(): Boolean = current.isSupported()

            override fun canManageLanguages(): Boolean = current.canManageLanguages()

            override suspend fun languageStates(languageTags: List<String>): Map<String, SpeechLanguageState>? =
                current.languageStates(languageTags)

            override fun download(languageTag: String): Flow<SpeechDownloadEvent> = current.download(languageTag)

            override fun session(languageTag: String): SpeechSession = current.session(languageTag)
        }
}
