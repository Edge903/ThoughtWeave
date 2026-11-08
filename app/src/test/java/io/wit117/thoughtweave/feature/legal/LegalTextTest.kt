package io.wit117.thoughtweave.feature.legal

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalTextTest {
    @Test
    fun joinsHardWrappedLinesWithinAParagraph() {
        assertEquals(
            listOf(LegalParagraph("first line second line"), LegalParagraph("next")),
            parseLegalParagraphs("  first line\n   second line\n\n\nnext\n"),
        )
    }

    @Test
    fun keepsNestingOfListItems() {
        assertEquals(
            listOf(0, 1, 2),
            parseLegalParagraphs("- a\n\n  - b\n\n    - c").map { it.indentLevel },
        )
    }

    @Test
    fun indentedPlainParagraphsAreNotTreatedAsNested() {
        assertEquals(0, parseLegalParagraphs("    Apache paragraph").single().indentLevel)
    }
}
