package io.wit117.thoughtweave.feature.memo

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import java.util.UUID
import io.wit117.thoughtweave.feature.aimodel.ModelStateRule
import io.wit117.thoughtweave.feature.aimodel.setAgreementAccepted
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S01, S02-A/B and S05 through the real app and database. Titles are unique because the database
 * is shared by all tests in the process; new memos sort to the top of the list.
 */
@RunWith(AndroidJUnit4::class)
class MemoFlowTest {
    @get:Rule(order = 0)
    val modelRule = ModelStateRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = setAgreementAccepted(true)

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitUntilGone(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }

    private fun clickText(@StringRes id: Int) =
        rule.onNode(hasText(str(id)) and hasClickAction()).performClick()

    private fun titleField() = rule.onAllNodes(hasSetTextAction())[0]
    private fun contentField() = rule.onAllNodes(hasSetTextAction())[1]

    private fun createMemo(): String {
        val title = "memo-${UUID.randomUUID()}"
        clickText(R.string.fab_new_memo)
        waitFor(hasSetTextAction())
        titleField().performTextInput(title)
        contentField().performTextInput("body")
        clickText(R.string.action_save)
        // The editor's title field also holds this text, so exclude it to wait for the list.
        waitFor(hasText(title) and !hasSetTextAction())
        return title
    }

    private fun openMemo(title: String) {
        rule.onNode(hasText(title) and hasClickAction()).performClick()
        waitFor(hasText(title) and hasSetTextAction())
    }

    @Test
    fun createEditGuardAndDelete() {
        val title = createMemo()
        openMemo(title)

        contentField().performTextReplacement("edited")
        rule.onNodeWithContentDescription(str(R.string.action_back)).performClick()
        waitFor(hasText(str(R.string.dialog_discard_title)))
        clickText(R.string.action_keep_editing)

        clickText(R.string.action_save)
        waitFor(hasText(str(R.string.memo_saved)))

        rule.onNodeWithContentDescription(str(R.string.action_delete)).performClick()
        waitFor(hasText(str(R.string.dialog_delete_memo_title)))
        clickText(R.string.action_delete)

        waitFor(hasText(str(R.string.title_memo_list)))
        waitUntilGone(hasText(title))
    }

    @Test
    fun extractionFromDirtyMemoSavesFirst() {
        val title = createMemo()
        openMemo(title)

        contentField().performTextReplacement("edited")
        clickText(R.string.fab_extract_schedule)
        waitFor(hasText(str(R.string.dialog_save_first_title)))
        clickText(R.string.action_save_and_continue)

        // The extraction screen arrives in a later phase and says so explicitly.
        waitFor(hasText(str(R.string.not_implemented_title)))
    }

    @Test
    fun overflowOpensLicensesAndDocumentText() {
        rule.onNodeWithContentDescription(str(R.string.action_more)).performClick()
        waitFor(hasText(str(R.string.title_licenses)) and hasClickAction())
        clickText(R.string.title_licenses)

        waitFor(hasText(str(R.string.license_gemma_terms)) and hasClickAction())
        clickText(R.string.license_gemma_terms)
        waitFor(hasText("Last modified: April 1, 2026"))

        rule.onNodeWithContentDescription(str(R.string.action_back)).performClick()
        waitFor(hasText(str(R.string.license_other_open_source)))
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
