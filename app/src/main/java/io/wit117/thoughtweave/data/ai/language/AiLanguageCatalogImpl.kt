package io.wit117.thoughtweave.data.ai.language

import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.repository.AiLanguageCatalog
import javax.inject.Inject

/** A memo counts as a non-Latin language when at least this share of its letters is in that script. */
private const val SCRIPT_SHARE = 0.2

class AiLanguageCatalogImpl @Inject constructor() : AiLanguageCatalog {

    override val inputLanguages: List<AppLanguage> =
        LanguageProfiles.all.filter { it.dateRules != null }.map { it.language }

    override val outputLanguages: List<AppLanguage> =
        LanguageProfiles.all.filter { it.prompts != null }.map { it.language }

    override val translationTargets: List<AppLanguage> =
        outputLanguages.filter { output -> LanguageProfiles.verifiedTranslations.any { it.second == output.tag } }

    override fun isVerifiedTranslation(input: AppLanguage, output: AppLanguage): Boolean =
        input.tag to output.tag in LanguageProfiles.verifiedTranslations

    override fun detect(text: String, preferredLatin: AppLanguage?): AppLanguage? {
        val letters = text.count { it.isLetter() }
        if (letters == 0) return null
        val candidates = LanguageProfiles.all.filter { it.dateRules != null }
        val nonLatin = candidates.filterNot { it.usesLatinScript }
            .map { profile -> profile to text.count(profile.isScriptChar).toDouble() / letters }
            .filter { (_, share) -> share >= SCRIPT_SHARE }
            .maxByOrNull { (_, share) -> share }
        if (nonLatin != null) return nonLatin.first.language
        val latin = candidates.filter { it.usesLatinScript }.map { it.language }
        return preferredLatin?.takeIf { it in latin } ?: latin.firstOrNull()
    }
}
