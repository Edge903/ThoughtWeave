package io.wit117.thoughtweave.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.speech.PlatformSpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession

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
            override fun isSupported(): Boolean = (SpeechInputTestOverride.repository ?: real).isSupported()

            override fun session(languageTag: String): SpeechSession =
                (SpeechInputTestOverride.repository ?: real).session(languageTag)
        }
}
