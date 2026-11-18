package io.wit117.thoughtweave.feature.dictation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DictationInsertionTest {
    private fun insert(text: String, cursor: Int, dictated: String, end: Int = cursor) =
        TextFieldValue(text, TextRange(cursor, end)).withDictation(dictated)

    @Test
    fun emptyFieldGetsTheWordsOnly() {
        val result = insert("", 0, "  buy milk ")
        assertEquals("buy milk", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun spaceIsAddedAfterExistingWords() {
        val result = insert("Shopping:", 9, "buy milk")
        assertEquals("Shopping: buy milk", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun noExtraSpaceAfterWhitespaceOrNewline() {
        assertEquals("a buy", insert("a ", 2, "buy").text)
        assertEquals("a\nbuy", insert("a\n", 2, "buy").text)
    }

    @Test
    fun spacesOnBothSidesInTheMiddleOfText() {
        val result = insert("12월3일", 3, "오후 3시")
        assertEquals("12월 오후 3시 3일", result.text)
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun selectionIsReplaced() {
        val result = insert("meet Tom today", 5, "Jane", end = 8)
        assertEquals("meet Jane today", result.text)
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun blankDictationChangesNothing() {
        val value = TextFieldValue("memo", TextRange(2))
        assertEquals(value, value.withDictation("  "))
    }

    @Test
    fun nextDictationContinuesWithASpace() {
        val first = insert("", 0, "first")
        assertEquals("first second", first.withDictation("second").text)
    }
}
