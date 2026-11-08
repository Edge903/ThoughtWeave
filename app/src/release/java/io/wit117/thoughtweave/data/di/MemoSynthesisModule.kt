package io.wit117.thoughtweave.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.ai.UnavailableMemoSynthesisRepository
import io.wit117.thoughtweave.data.ai.delivery.ModelDelivery
import io.wit117.thoughtweave.data.ai.delivery.PlayFastFollowDelivery
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoSynthesisModule {
    @Binds
    abstract fun bindMemoSynthesisRepository(impl: UnavailableMemoSynthesisRepository): MemoSynthesisRepository

    @Binds
    abstract fun bindModelDelivery(impl: PlayFastFollowDelivery): ModelDelivery
}
