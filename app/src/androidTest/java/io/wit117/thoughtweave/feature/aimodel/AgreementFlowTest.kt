package io.wit117.thoughtweave.feature.aimodel

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import java.util.UUID
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** POP04, the S03 consent banner, S02-B gating and withdrawal in S05. */
@RunWith(AndroidJUnit4::class)
class AgreementFlowTest {
    @get:Rule(order = 0)
    val modelRule = ModelStateRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = setAgreementAccepted(false)

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitUntilGone(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }

    private fun clickable(@StringRes id: Int) = rule.onNode(hasText(str(id)) and hasClickAction())

    private val draftMarker = hasText("STATUS: DRAFT", substring = true)

    private fun acceptConsentDialog() {
        waitFor(hasText(str(R.string.title_ai_consent)))
        waitFor(draftMarker)
        clickable(R.string.action_agree).assertIsNotEnabled()
        clickable(R.string.consent_checkbox).performClick()
        clickable(R.string.action_agree).assertIsEnabled().performClick()
    }

    @Test
    fun s03BannerOpensConsentAndAcceptingClearsIt() {
        clickable(R.string.nav_ai_merge).performClick()
        waitFor(hasText(str(R.string.ai_consent_banner_message)))

        clickable(R.string.action_review_agreement).performClick()
        acceptConsentDialog()

        waitUntilGone(hasText(str(R.string.ai_consent_banner_message)))
    }

    @Test
    fun cancellingConsentKeepsBanner() {
        clickable(R.string.nav_ai_merge).performClick()
        waitFor(hasText(str(R.string.ai_consent_banner_message)))

        clickable(R.string.action_review_agreement).performClick()
        waitFor(hasText(str(R.string.title_ai_consent)))
        clickable(R.string.action_cancel).performClick()

        waitUntilGone(hasText(str(R.string.title_ai_consent)))
        waitFor(hasText(str(R.string.ai_consent_banner_message)))
    }

    @Test
    fun extractionAsksForConsentThenContinues() {
        val title = "memo-${UUID.randomUUID()}"
        clickable(R.string.fab_new_memo).performClick()
        waitFor(hasSetTextAction())
        rule.onAllNodes(hasSetTextAction())[0].performTextInput(title)
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("Due 12/3")
        clickable(R.string.action_save).performClick()
        waitFor(hasText(title) and !hasSetTextAction())
        rule.onNode(hasText(title) and hasClickAction()).performClick()
        waitFor(hasText(title) and hasSetTextAction())

        clickable(R.string.fab_extract_schedule).performClick()
        acceptConsentDialog()

        waitFor(hasText("Extracted schedule") and hasSetTextAction())
    }

    @Test
    fun withdrawInS05RemovesConsentAndAgreementIsReadable() {
        setAgreementAccepted(true)
        rule.onNodeWithContentDescription(str(R.string.action_more)).performClick()
        waitFor(hasText(str(R.string.title_licenses)) and hasClickAction())
        clickable(R.string.title_licenses).performClick()
        waitFor(hasText(str(R.string.action_withdraw_consent)) and hasClickAction())

        clickable(R.string.license_agreement).performClick()
        waitFor(draftMarker)
        rule.onNodeWithContentDescription(str(R.string.action_back)).performClick()

        waitFor(hasText(str(R.string.action_withdraw_consent)) and hasClickAction())
        clickable(R.string.action_withdraw_consent).performClick()
        waitFor(hasText(str(R.string.dialog_withdraw_title)))
        clickable(R.string.action_withdraw).performClick()

        waitUntilGone(hasText(str(R.string.action_withdraw_consent)))
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
