package io.wit117.thoughtweave.domain.model

sealed interface InputLanguageSetting {
    /** Detect from the memo's script, falling back to the app language. */
    data object Auto : InputLanguageSetting
    data class Specific(val language: AppLanguage) : InputLanguageSetting
}

sealed interface OutputLanguageSetting {
    data object SameAsInput : OutputLanguageSetting
    data class Specific(val language: AppLanguage) : OutputLanguageSetting
}

data class LanguageSettings(
    val input: InputLanguageSetting = InputLanguageSetting.Auto,
    val output: OutputLanguageSetting = OutputLanguageSetting.SameAsInput,
)

/**
 * The languages for one AI request. [unavailableOutput] is the requested output language when that
 * translation is not supported, in which case [output] falls back to [input].
 */
data class AiLanguagePlan(
    val input: AppLanguage,
    val output: AppLanguage,
    val unavailableOutput: AppLanguage? = null,
)
