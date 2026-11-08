package io.wit117.thoughtweave.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.ai.delivery.ImportedModelDelivery
import io.wit117.thoughtweave.data.ai.delivery.ModelDelivery
import io.wit117.thoughtweave.data.ai.engine.GemmaInferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferenceEngine
import kotlinx.coroutines.flow.Flow

/** Lets instrumented tests supply replies. Unset in the running app, and absent from release. */
object InferenceEngineTestOverride {
    @Volatile
    var engine: InferenceEngine? = null
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BuildTypeAiModule {
    @Binds
    abstract fun bindModelDelivery(impl: ImportedModelDelivery): ModelDelivery

    companion object {
        @Provides
        fun provideInferenceEngine(real: GemmaInferenceEngine): InferenceEngine = object : InferenceEngine {
            override fun generate(prompt: String, temperature: Double): Flow<String> =
                (InferenceEngineTestOverride.engine ?: real).generate(prompt, temperature)
        }
    }
}
