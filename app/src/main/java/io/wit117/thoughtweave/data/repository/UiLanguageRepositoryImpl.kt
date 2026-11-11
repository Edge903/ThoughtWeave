package io.wit117.thoughtweave.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.repository.UiLanguageRepository
import javax.inject.Inject

/**
 * Per-app language through AppCompat: Android 13+ stores it in the system (also editable in system
 * settings); Android 12 and below use AppCompat's `autoStoreLocales` (see AndroidManifest.xml).
 */
class UiLanguageRepositoryImpl @Inject constructor() : UiLanguageRepository {

    override fun current(): AppLanguage? =
        AppLanguages.forTag(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(','))

    override fun set(language: AppLanguage?) {
        AppCompatDelegate.setApplicationLocales(
            language?.let { LocaleListCompat.forLanguageTags(it.tag) } ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }
}
