package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting

interface LanguageSettingsRepository {
    fun current(): LanguageSettings
    fun setInput(setting: InputLanguageSetting)
    fun setOutput(setting: OutputLanguageSetting)
}

/** Which languages the AI features support, from the registered language profiles. */
interface AiLanguageCatalog {
    val inputLanguages: List<AppLanguage>
    val outputLanguages: List<AppLanguage>

    /** Output languages that at least one verified translation pair can produce; empty hides the setting. */
    val translationTargets: List<AppLanguage>

    fun isVerifiedTranslation(input: AppLanguage, output: AppLanguage): Boolean

    /**
     * The memo's language by script. Latin-script languages cannot be told apart, so Latin text yields
     * [preferredLatin] when it is a Latin-script input language, otherwise the first one. Null if [text]
     * has no letters.
     */
    fun detect(text: String, preferredLatin: AppLanguage?): AppLanguage?
}
