package io.wit117.thoughtweave.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.ai.FakeMemoSynthesisRepository
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoSynthesisModule {
    @Binds
    abstract fun bindMemoSynthesisRepository(impl: FakeMemoSynthesisRepository): MemoSynthesisRepository
}
