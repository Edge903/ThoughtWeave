package io.wit117.thoughtweave.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun matchesByLanguageIgnoringRegion() {
        assertEquals(AppLanguages.KOREAN, AppLanguages.forTag("ko-KR"))
        assertEquals(AppLanguages.ENGLISH, AppLanguages.forTag("en-US"))
        assertEquals(AppLanguages.ENGLISH, AppLanguages.forTag("en"))
    }

    @Test
    fun unsupportedOrEmptyMeansNoLanguage() {
        assertNull(AppLanguages.forTag("fr-FR"))
        assertNull(AppLanguages.forTag(""))
        assertNull(AppLanguages.forTag(null))
    }

    @Test
    fun tagsAreUnique() {
        assertEquals(AppLanguages.all.size, AppLanguages.all.map { it.tag }.toSet().size)
    }
}
