package io.wit117.thoughtweave.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.speech.PlatformSpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class BuildTypeSpeechModule {
    @Binds
    abstract fun bindSpeechInputRepository(impl: PlatformSpeechInputRepository): SpeechInputRepository
}
