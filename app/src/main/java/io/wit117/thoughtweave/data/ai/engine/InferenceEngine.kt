package io.wit117.thoughtweave.data.ai.engine

import kotlinx.coroutines.flow.Flow

class InferenceException(
    message: String,
    cause: Throwable? = null,
    val isInputTooLong: Boolean = false,
) : Exception(message, cause)

interface InferenceEngine {
    /** Streams the model's reply. The flow fails with [InferenceException] if none can be produced. */
    fun generate(prompt: String, temperature: Double): Flow<String>
}
