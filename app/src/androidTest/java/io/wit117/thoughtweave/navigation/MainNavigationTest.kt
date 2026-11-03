package io.wit117.thoughtweave.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Phase 0: bottom navigation, per-screen titles and context-sensitive FAB behavior. */
@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int) = rule.activity.getString(id)

    /** Bottom-bar items are the clickable nodes carrying the tab label. */
    private fun tab(@StringRes labelId: Int) =
        rule.onNode(hasText(str(labelId)) and hasClickAction())

    /** Screen titles are plain text; tab labels and FABs with the same words are clickable. */
    private fun title(@StringRes titleId: Int) =
        rule.onNode(hasText(str(titleId)) and !hasClickAction())

    private fun fab(@StringRes textId: Int) =
        rule.onNode(hasText(str(textId)) and hasClickAction())

    @Test
    fun startsOnMemoListWithNewMemoFab() {
        title(R.string.title_memo_list).assertExists()
        tab(R.string.nav_memos).assertIsSelected()
        fab(R.string.fab_new_memo).assertIsEnabled()
    }

    @Test
    fun aiMergeFabIsDisabledWithoutSelection() {
        tab(R.string.nav_ai_merge).performClick()
        title(R.string.title_ai_merge).assertExists()
        tab(R.string.nav_ai_merge).assertIsSelected()
        fab(R.string.fab_ai_synthesize).assertIsNotEnabled()
    }

    @Test
    fun newMemoFabOpensEditorWithoutBottomBar_andBackReturns() {
        fab(R.string.fab_new_memo).performClick()
        title(R.string.title_new_memo).assertExists()
        tab(R.string.nav_memos).assertDoesNotExist()

        rule.onNodeWithContentDescription(str(R.string.action_back)).performClick()
        title(R.string.title_memo_list).assertExists()
        tab(R.string.nav_memos).assertExists()
    }

    @Test
    fun addScheduleFabOpensManualEditor_andBackReturnsToSchedules() {
        tab(R.string.nav_schedules).performClick()
        title(R.string.title_schedules).assertExists()

        fab(R.string.fab_add_schedule).performClick()
        title(R.string.title_new_schedule).assertExists()
        tab(R.string.nav_schedules).assertDoesNotExist()

        rule.onNodeWithContentDescription(str(R.string.action_back)).performClick()
        title(R.string.title_schedules).assertExists()
        tab(R.string.nav_schedules).assertIsSelected()
    }

    @Test
    fun switchingTabsBackToMemosRestoresStartScreen() {
        tab(R.string.nav_schedules).performClick()
        tab(R.string.nav_ai_merge).performClick()
        tab(R.string.nav_memos).performClick()
        title(R.string.title_memo_list).assertExists()
        tab(R.string.nav_memos).assertIsSelected()
    }
}
