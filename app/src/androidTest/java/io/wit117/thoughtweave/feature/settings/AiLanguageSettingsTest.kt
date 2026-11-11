package io.wit117.thoughtweave.feature.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.data.repository.LANGUAGE_PREFS_NAME
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** S06: AI input language is saved; the output language row is hidden while no translation is verified. */
@RunWith(AndroidJUnit4::class)
class AiLanguageSettingsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val prefs
        get() = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun inputLanguageIsSavedAndOutputRowIsHidden() {
        rule.onNode(hasContentDescription(str(R.string.action_more))).performClick()
        waitFor(hasText(str(R.string.title_settings)) and hasClickAction())
        rule.onNode(hasText(str(R.string.title_settings)) and hasClickAction()).performClick()

        waitFor(hasText(str(R.string.settings_ai_input_language)))
        assertEquals(0, rule.onAllNodes(hasText(str(R.string.settings_ai_output_language))).fetchSemanticsNodes().size)

        rule.onNode(hasText(str(R.string.settings_ai_input_language)) and hasClickAction()).performClick()
        waitFor(hasText("한국어") and hasClickAction())
        rule.onNode(hasText("한국어") and hasClickAction()).performClick()

        waitFor(hasText("한국어"))
        assertEquals("ko", prefs.getString("input_language", null))
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
