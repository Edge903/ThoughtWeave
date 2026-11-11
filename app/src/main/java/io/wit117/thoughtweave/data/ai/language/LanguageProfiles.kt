package io.wit117.thoughtweave.data.ai.language

import io.wit117.thoughtweave.domain.model.AppLanguage

/** The AI side of every supported language. Register new profiles here. */
object LanguageProfiles {
    val all: List<LanguageProfile> = listOf(EnglishProfile, KoreanProfile)

    /**
     * (input, output) pairs that passed the real-model translation test (docs/ADDING_A_LANGUAGE.md).
     * Any other pair keeps the output in the input language and tells the user so. While this is empty,
     * S06 hides the output language setting.
     *
     * en↔ko failed on Gemma 3 1B (2026-11-11): changed weekdays and dates ("Friday" → "다음 주 화요일"),
     * dropped facts, answered questions, and garbled titles.
     */
    val verifiedTranslations: Set<Pair<String, String>> = emptySet()

    fun of(language: AppLanguage): LanguageProfile? = all.firstOrNull { it.language == language }

    /** The profile for [language], or English if it has none. */
    fun forLanguage(language: AppLanguage): LanguageProfile = of(language) ?: EnglishProfile
}
