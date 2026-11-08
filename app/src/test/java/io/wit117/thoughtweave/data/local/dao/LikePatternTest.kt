package io.wit117.thoughtweave.data.local.dao

import org.junit.Assert.assertEquals
import org.junit.Test

class LikePatternTest {
    @Test
    fun wrapsPlainTextInWildcards() {
        assertEquals("%회의%", toLikeContainsPattern("회의"))
    }

    @Test
    fun escapesLikeMetacharacters() {
        assertEquals("""%50\%\_off%""", toLikeContainsPattern("50%_off"))
    }

    @Test
    fun escapesTheEscapeCharacterFirst() {
        assertEquals("""%a\\\%%""", toLikeContainsPattern("""a\%"""))
    }
}
