package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting
import io.wit117.thoughtweave.domain.repository.AiLanguageCatalog
import io.wit117.thoughtweave.domain.repository.LanguageSettingsRepository
import io.wit117.thoughtweave.domain.repository.UiLanguageRepository
import java.util.Locale
import javax.inject.Inject

/** Picks the input and output language of one AI request from the settings and the memo text. */
class ResolveAiLanguageUseCase @Inject constructor(
    private val settings: LanguageSettingsRepository,
    private val catalog: AiLanguageCatalog,
    private val uiLanguage: UiLanguageRepository,
) {
    operator fun invoke(text: String): AiLanguagePlan {
        val current = settings.current()
        val appLanguage = uiLanguage.current() ?: AppLanguages.forTag(Locale.getDefault().toLanguageTag())

        val input = (current.input as? InputLanguageSetting.Specific)?.language
            ?.takeIf { it in catalog.inputLanguages }
            ?: catalog.detect(text, appLanguage)
            ?: appLanguage?.takeIf { it in catalog.inputLanguages }
            ?: catalog.inputLanguages.first()

        val sameAsInput = input.takeIf { it in catalog.outputLanguages } ?: catalog.outputLanguages.first()
        val requested: AppLanguage? = (current.output as? OutputLanguageSetting.Specific)?.language
            ?.takeIf { it in catalog.outputLanguages && it != input }
        return when {
            requested == null -> AiLanguagePlan(input, sameAsInput)
            catalog.isVerifiedTranslation(input, requested) -> AiLanguagePlan(input, requested)
            else -> AiLanguagePlan(input, sameAsInput, unavailableOutput = requested)
        }
    }
}
