package io.wit117.thoughtweave

import io.wit117.thoughtweave.data.ai.language.AiLanguageCatalogImpl
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting
import io.wit117.thoughtweave.domain.repository.LanguageSettingsRepository
import io.wit117.thoughtweave.domain.repository.UiLanguageRepository
import io.wit117.thoughtweave.domain.usecase.ResolveAiLanguageUseCase

internal class FakeLanguageSettings(var settings: LanguageSettings = LanguageSettings()) : LanguageSettingsRepository {
    override fun current() = settings
    override fun setInput(setting: InputLanguageSetting) {
        settings = settings.copy(input = setting)
    }

    override fun setOutput(setting: OutputLanguageSetting) {
        settings = settings.copy(output = setting)
    }
}

internal class FakeUiLanguage(var language: AppLanguage? = null) : UiLanguageRepository {
    override fun current() = language
    override fun set(language: AppLanguage?) {
        this.language = language
    }
}

/** The real catalog of registered profiles, with default settings. */
internal fun testLanguageResolver(
    settings: FakeLanguageSettings = FakeLanguageSettings(),
    ui: FakeUiLanguage = FakeUiLanguage(),
) = ResolveAiLanguageUseCase(settings, AiLanguageCatalogImpl(), ui)
