package io.wit117.thoughtweave.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting
import io.wit117.thoughtweave.domain.repository.LanguageSettingsRepository
import javax.inject.Inject

const val LANGUAGE_PREFS_NAME = "language_settings"
private const val KEY_INPUT = "input_language"
private const val KEY_OUTPUT = "output_language"
private const val AUTO = "auto"
private const val SAME_AS_INPUT = "same"

/** Included in backup. A stored language that is no longer supported falls back to the default. */
class LanguageSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : LanguageSettingsRepository {

    private val prefs = context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)

    override fun current(): LanguageSettings = LanguageSettings(
        input = AppLanguages.forTag(prefs.getString(KEY_INPUT, AUTO))
            ?.let { InputLanguageSetting.Specific(it) } ?: InputLanguageSetting.Auto,
        output = AppLanguages.forTag(prefs.getString(KEY_OUTPUT, SAME_AS_INPUT))
            ?.let { OutputLanguageSetting.Specific(it) } ?: OutputLanguageSetting.SameAsInput,
    )

    override fun setInput(setting: InputLanguageSetting) = prefs.edit {
        putString(KEY_INPUT, (setting as? InputLanguageSetting.Specific)?.language?.tag ?: AUTO)
    }

    override fun setOutput(setting: OutputLanguageSetting) = prefs.edit {
        putString(KEY_OUTPUT, (setting as? OutputLanguageSetting.Specific)?.language?.tag ?: SAME_AS_INPUT)
    }
}
