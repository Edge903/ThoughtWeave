package io.wit117.thoughtweave.data.ai.prompt

import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.engine.PromptExample
import io.wit117.thoughtweave.data.ai.language.EnglishProfile
import io.wit117.thoughtweave.data.ai.language.LanguageProfile
import io.wit117.thoughtweave.data.ai.language.PromptPack
import io.wit117.thoughtweave.domain.model.Memo

const val SUMMARY_TEMPERATURE = 0.2
const val TITLE_TEMPERATURE = 0.2

/** A title needs only the start of the memo; dates are read from the full text separately. */
private const val MAX_TITLE_SOURCE_CHARS = 2_000

/** Gemma 3 1B writes in the language of its instructions, so the output language's prompts are used. */
private fun LanguageProfile.pack(): PromptPack = prompts ?: checkNotNull(EnglishProfile.prompts)

/**
 * The memos are labelled in their own [input] language; instructions and the example's reply are in the
 * [output] language. When they differ, the example pairs the input language's memo with the output
 * language's reply, which is why every language's example must have the same content.
 */
fun summaryPrompt(memos: List<Memo>, input: LanguageProfile, output: LanguageProfile): InferencePrompt {
    val inputPack = input.pack()
    val outputPack = output.pack()
    val isTranslation = input.language != output.language
    val text = memos.withIndex().joinToString("\n\n") { (index, memo) ->
        buildString {
            appendLine("${inputPack.memoLabel} ${index + 1}:")
            if (memo.title.isNotBlank()) appendLine(memo.title)
            append(memo.content)
        }
    }
    return InferencePrompt(
        text = text,
        temperature = SUMMARY_TEMPERATURE,
        systemInstruction = if (isTranslation) {
            outputPack.summarySystem + " " + outputPack.writeOnlyInThisLanguage
        } else {
            outputPack.summarySystem
        },
        examples = listOf(PromptExample(inputPack.summaryExample.input, outputPack.summaryExample.output)),
    )
}

fun scheduleTitlePrompt(title: String, content: String, input: LanguageProfile, output: LanguageProfile): String {
    val pack = output.pack()
    val source = "$title\n$content".trim().take(MAX_TITLE_SOURCE_CHARS)
    return buildString {
        appendLine(pack.titleInstruction)
        if (input.language != output.language) appendLine(pack.writeOnlyInThisLanguage)
        appendLine()
        appendLine(pack.memoHeading)
        appendLine(source)
    }
}
