package io.wit117.thoughtweave.feature.dictation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Inserts dictated [text] at the cursor, replacing any selection, with a space on each side where it
 * would otherwise touch other text. The cursor ends right after the inserted words.
 */
fun TextFieldValue.withDictation(text: String): TextFieldValue {
    val words = text.trim()
    if (words.isEmpty()) return this
    val before = this.text.substring(0, selection.min)
    val after = this.text.substring(selection.max)
    val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
    val trail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
    val cursor = before.length + lead.length + words.length
    return TextFieldValue(before + lead + words + trail + after, TextRange(cursor))
}
