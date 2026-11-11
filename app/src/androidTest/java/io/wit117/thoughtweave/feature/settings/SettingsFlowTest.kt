package io.wit117.thoughtweave.feature.settings

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.domain.model.AppLanguages
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S06: switching the app language from inside the app, and back to the system language. Works whatever
 * the device language is: it switches to whichever supported language differs from it.
 */
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The device language, as the app shows it when following the system. */
    private val systemLocale: Locale = Resources.getSystem().configuration.locales[0]

    /** A supported language other than the device's, to switch to. */
    private val target = if (systemLocale.language == "ko") AppLanguages.ENGLISH else AppLanguages.KOREAN
    private val targetLocale = Locale.forLanguageTag(target.tag)

    /** Strings in a given language, independent of the language currently applied. */
    private fun str(@StringRes id: Int, locale: Locale): String {
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config).getString(id)
    }

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    private fun resetLanguage() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Before
    fun setUp() = resetLanguage()

    @After
    fun tearDown() = resetLanguage()

    @Test
    fun switchLanguageAndBackToSystem() {
        val system = systemLocale

        waitFor(hasContentDescription(str(R.string.action_more, system)))
        rule.onNode(hasContentDescription(str(R.string.action_more, system))).performClick()
        waitFor(hasText(str(R.string.title_settings, system)) and hasClickAction())
        rule.onNode(hasText(str(R.string.title_settings, system)) and hasClickAction()).performClick()

        waitFor(hasText(str(R.string.settings_ui_language, system)))
        rule.onNode(hasText(str(R.string.settings_ui_language, system)) and hasClickAction()).performClick()
        waitFor(hasText(target.nativeName) and hasClickAction())
        rule.onNode(hasText(target.nativeName) and hasClickAction()).performClick()

        // The screen is recreated in the chosen language and stays on S06.
        waitFor(hasText(str(R.string.settings_ui_language, targetLocale)))
        assertEquals(target.tag, LocaleManagerCompat.getApplicationLocales(context).toLanguageTags())

        rule.onNode(hasText(str(R.string.settings_ui_language, targetLocale)) and hasClickAction()).performClick()
        waitFor(hasText(str(R.string.language_system, targetLocale)) and hasClickAction())
        rule.onNode(hasText(str(R.string.language_system, targetLocale)) and hasClickAction()).performClick()

        waitFor(hasText(str(R.string.settings_ui_language, system)))
        assertEquals("", LocaleManagerCompat.getApplicationLocales(context).toLanguageTags())
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
