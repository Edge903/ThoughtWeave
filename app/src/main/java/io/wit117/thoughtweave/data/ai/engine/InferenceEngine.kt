package io.wit117.thoughtweave.data.ai.engine

import kotlinx.coroutines.flow.Flow

class InferenceException(
    message: String,
    cause: Throwable? = null,
    val isInputTooLong: Boolean = false,
) : Exception(message, cause)

/** A completed exchange shown to the model before the real input. */
data class PromptExample(val input: String, val output: String)

data class InferencePrompt(
    val text: String,
    val temperature: Double,
    val systemInstruction: String? = null,
    val examples: List<PromptExample> = emptyList(),
)

interface InferenceEngine {
    /** Streams the model's reply. The flow fails with [InferenceException] if none can be produced. */
    fun generate(prompt: InferencePrompt): Flow<String>
}
