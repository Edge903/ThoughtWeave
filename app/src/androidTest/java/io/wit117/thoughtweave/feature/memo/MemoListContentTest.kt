package io.wit117.thoughtweave.feature.memo

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.theme.ThoughtWeaveTheme
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import io.wit117.thoughtweave.feature.memo.list.MemoListContent
import io.wit117.thoughtweave.feature.memo.list.MemoListUiState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** S01 states that do not depend on stored data. */
@RunWith(AndroidJUnit4::class)
class MemoListContentTest {
    @get:Rule
    val rule = createComposeRule()

    private fun str(@StringRes id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val memo = Memo(
        id = 7,
        title = "",
        content = "body text",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        isPinned = false,
        origin = MemoOrigin.MANUAL,
    )

    private fun show(
        state: MemoListUiState,
        current: () -> MemoListUiState = { state },
        query: String? = null,
        onOpen: (Long) -> Unit = {},
        onPin: (Memo) -> Unit = {},
    ) = rule.setContent {
        ThoughtWeaveTheme {
            MemoListContent(
                uiState = current(),
                searchQuery = query,
                snackbarHostState = SnackbarHostState(),
                onCreateMemo = {},
                onOpenMemo = onOpen,
                onOpenLicenses = {},
                onTogglePin = onPin,
                onOpenSearch = {},
                onSearchQueryChange = {},
                onCloseSearch = {},
            )
        }
    }

    @Test
    fun emptyTitleIsDisplayedAsUntitled_andCardOpensMemo() {
        var opened: Long? = null
        show(MemoListUiState.Loaded(listOf(memo)), onOpen = { opened = it })

        rule.onNodeWithText(str(R.string.memo_untitled)).assertIsDisplayed().performClick()

        assertEquals(7L, opened)
    }

    @Test
    fun starTogglesPinWithoutOpeningMemo() {
        var pinned: Memo? = null
        var opened = false
        show(MemoListUiState.Loaded(listOf(memo)), onOpen = { opened = true }, onPin = { pinned = it })

        rule.onNodeWithContentDescription(str(R.string.action_pin)).performClick()

        assertEquals(memo, pinned)
        assertEquals(false, opened)
    }

    @Test
    fun memoAddedAboveTheFirstItemIsVisible() {
        val initial = MemoListUiState.Loaded(listOf(memo))
        val holder = mutableStateOf<MemoListUiState>(initial)
        show(initial, current = { holder.value })
        rule.waitForIdle()

        holder.value = MemoListUiState.Loaded(listOf(memo.copy(id = 8, title = "newest"), memo))

        rule.onNodeWithText("newest").assertIsDisplayed()
    }

    @Test
    fun pinnedMemoOffersUnpin() {
        show(MemoListUiState.Loaded(listOf(memo.copy(isPinned = true))))

        rule.onNodeWithContentDescription(str(R.string.action_unpin)).assertIsDisplayed()
    }

    @Test
    fun emptyListShowsFirstMemoMessage() {
        show(MemoListUiState.Loaded(emptyList()))

        rule.onNodeWithText(str(R.string.empty_memos_title)).assertIsDisplayed()
    }

    @Test
    fun emptySearchShowsNoMatchMessage() {
        show(MemoListUiState.Loaded(emptyList()), query = "x")

        rule.onNodeWithText(str(R.string.empty_search_title)).assertIsDisplayed()
    }
}
