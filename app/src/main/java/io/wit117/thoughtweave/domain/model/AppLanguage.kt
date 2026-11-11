package io.wit117.thoughtweave.domain.model

import java.util.Locale

/**
 * A language the app supports. [nativeName] is shown untranslated in pickers, as is conventional,
 * so it lives here rather than in string resources.
 */
data class AppLanguage(val tag: String, val nativeName: String)

/**
 * The single list of supported languages. Adding a language starts here; L2 adds its AI parts and
 * L3 a contract test that checks every entry is complete (see docs/TODO.md).
 */
object AppLanguages {
    val ENGLISH = AppLanguage("en", "English")
    val KOREAN = AppLanguage("ko", "한국어")

    val all: List<AppLanguage> = listOf(ENGLISH, KOREAN)

    /** Matches by language only, so "ko-KR" or "en-US" from system settings find "ko" or "en". */
    fun forTag(tag: String?): AppLanguage? {
        if (tag.isNullOrBlank()) return null
        val language = Locale.forLanguageTag(tag).language
        return all.firstOrNull { Locale.forLanguageTag(it.tag).language == language }
    }
}
