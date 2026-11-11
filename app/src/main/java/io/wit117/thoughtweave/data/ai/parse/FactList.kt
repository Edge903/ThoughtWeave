package io.wit117.thoughtweave.data.ai.parse

import io.wit117.thoughtweave.domain.model.Memo

/** Abbreviations whose period does not end a sentence. */
private const val ABBREVIATIONS = "Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec|Mr|Mrs|Ms|Dr|St|No|vs|etc"

/** A sentence ends at ".", "!" or "?" followed by a space, unless the period closes an abbreviation. */
private val SENTENCE_END = Regex("(?<!\\b(?:$ABBREVIATIONS)\\.)(?<=[.!?])\\s+")

private val SPACES = Regex("\\s+")

/** Two items count as the same when they differ only in spacing, case or a trailing period. */
private fun normalized(item: String) = item.lowercase().replace(SPACES, " ").trimEnd('.', '。', ' ')

/**
 * Every line and sentence of [memos], in their original wording and order, without duplicates. Built
 * by code, never by the model, so nothing can be lost or reworded; it follows the AI text in a summary.
 */
fun factList(memos: List<Memo>): List<String> {
    val seen = mutableSetOf<String>()
    return memos.flatMap { memo ->
        memo.content.lines()
            .flatMap { it.split(SENTENCE_END) }
            .map { it.trim().removePrefix("- ").removePrefix("• ").trim() }
            .filter { it.isNotEmpty() }
    }.filter { seen.add(normalized(it)) }
}
