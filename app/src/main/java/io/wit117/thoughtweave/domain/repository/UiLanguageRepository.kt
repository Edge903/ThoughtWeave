package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.AppLanguage

interface UiLanguageRepository {
    /** The language chosen in the app, or null when the app follows the system language. */
    fun current(): AppLanguage?

    /** Applies [language], or the system language for null. The current screen is recreated. */
    fun set(language: AppLanguage?)
}
