package io.wit117.thoughtweave.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.data.di.SpeechInputTestOverride
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

/** Korean is downloadable; its download reports progress and then installs it. */
class ScriptedLanguages : ExternalResource(), SpeechInputRepository {
    @Volatile
    var korean = SpeechLanguageState.DOWNLOADABLE
    val requested = mutableListOf<String>()

    override fun before() {
        SpeechInputTestOverride.repository = this
    }

    override fun after() {
        SpeechInputTestOverride.repository = null
    }

    override fun isSupported() = true
    override fun canManageLanguages() = true
    override suspend fun languageStates(languageTags: List<String>) = languageTags.associateWith {
        if (it == AppLanguages.KOREAN.speechTag) korean else SpeechLanguageState.INSTALLED
    }

    override fun download(languageTag: String): Flow<SpeechDownloadEvent> = flow {
        requested += languageTag
        emit(SpeechDownloadEvent.Progress(50))
        korean = SpeechLanguageState.INSTALLED
        emit(SpeechDownloadEvent.Done)
    }

    override fun session(languageTag: String): SpeechSession = error("not used")
}

@RunWith(AndroidJUnit4::class)
class VoiceSettingsFlowTest {
    @get:Rule(order = 0)
    val languages = ScriptedLanguages()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(10_000L) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun downloadableLanguageCanBeDownloaded() {
        waitFor(hasContentDescription(str(R.string.action_more)))
        rule.onNode(hasContentDescription(str(R.string.action_more))).performClick()
        waitFor(hasText(str(R.string.title_settings)) and hasClickAction())
        rule.onNode(hasText(str(R.string.title_settings)) and hasClickAction()).performClick()

        waitFor(hasText(str(R.string.voice_language_downloadable)))
        rule.onNode(hasText(str(R.string.action_download)) and hasClickAction()).performScrollTo().performClick()

        // Done re-reads the states, so the row ends as installed and the button is gone.
        rule.waitUntil(10_000L) {
            rule.onAllNodes(hasText(str(R.string.voice_language_installed))).fetchSemanticsNodes().size == 2
        }
        rule.onNode(hasText(str(R.string.action_download))).assertDoesNotExist()
        assertEquals(listOf(AppLanguages.KOREAN.speechTag), languages.requested)
    }
}
