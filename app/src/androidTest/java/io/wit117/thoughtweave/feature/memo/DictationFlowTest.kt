package io.wit117.thoughtweave.feature.memo

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.data.di.SpeechInputTestOverride
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import io.wit117.thoughtweave.domain.repository.SpeechSession
import io.wit117.thoughtweave.feature.aimodel.ModelStateRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

/**
 * Scripted dictation, set before launch: emulator speech cannot be controlled. Each session recognizes
 * [pieces] in turn, like a session that continues through a pause.
 */
class SpeechInputRule(
    private val supported: Boolean,
    private val pieces: List<String> = listOf("buy milk", "and eggs"),
) : ExternalResource() {
    val tags = mutableListOf<String>()

    override fun before() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            Manifest.permission.RECORD_AUDIO,
        )
        SpeechInputTestOverride.repository = object : SpeechInputRepository {
            override fun isSupported() = supported
            override fun canManageLanguages() = supported
            override suspend fun languageStates(languageTags: List<String>): Map<String, SpeechLanguageState> =
                languageTags.associateWith { SpeechLanguageState.INSTALLED }
            override fun download(languageTag: String): Flow<SpeechDownloadEvent> = emptyFlow()
            override fun session(languageTag: String): SpeechSession {
                tags += languageTag
                return object : SpeechSession {
                    override val events: Flow<SpeechEvent> = flowOf(
                        *pieces.flatMap { listOf(SpeechEvent.Partial(it.substringBefore(' ')), SpeechEvent.Recognized(it)) }
                            .toTypedArray(),
                    )
                    override fun stop() = Unit
                }
            }
        }
    }

    override fun after() {
        SpeechInputTestOverride.repository = null
    }
}

@RunWith(AndroidJUnit4::class)
class DictationFlowTest {
    @get:Rule(order = 0)
    val modelRule = ModelStateRule()

    @get:Rule(order = 1)
    val speechRule = SpeechInputRule(supported = true)

    @get:Rule(order = 2)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun dictatedTextIsInsertedAtTheCursorAndNotSaved() {
        rule.onNode(hasText(str(R.string.fab_new_memo)) and hasClickAction()).performClick()
        waitFor(hasSetTextAction())
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("Shopping:")

        rule.onNodeWithContentDescription(str(R.string.action_dictate)).performClick()

        waitFor(hasText("Shopping: buy milk and eggs") and hasSetTextAction())
        assertTrue(speechRule.tags.single().contains('-'))
        // Dictation only edits the draft: the Save button is still there to press.
        rule.onNode(hasText(str(R.string.action_save)) and hasClickAction()).assertExists()
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

@RunWith(AndroidJUnit4::class)
class DictationUnsupportedTest {
    @get:Rule(order = 0)
    val modelRule = ModelStateRule()

    @get:Rule(order = 1)
    val speechRule = SpeechInputRule(supported = false)

    @get:Rule(order = 2)
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun micIsHiddenWithoutOnDeviceRecognition() {
        rule.onNode(hasText(rule.activity.getString(R.string.fab_new_memo)) and hasClickAction()).performClick()
        rule.waitUntil(5_000L) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription(rule.activity.getString(R.string.action_dictate))).assertDoesNotExist()
    }
}
