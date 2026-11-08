package io.wit117.thoughtweave.feature.memo.list

import androidx.lifecycle.SavedStateHandle
import io.wit117.thoughtweave.feature.memo.FakeMemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeMemoRepository()
    private lateinit var viewModel: MemoListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = MemoListViewModel(repository, SavedStateHandle())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.subscribe() {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
    }

    private fun titles() =
        (viewModel.uiState.value as MemoListUiState.Loaded).memos.map { it.title }

    @Test
    fun showsMemosAndFiltersWhileSearching() = runTest(dispatcher) {
        repository.add("회의")
        repository.add("장보기")
        subscribe()
        assertEquals(listOf("회의", "장보기"), titles())

        viewModel.openSearch()
        assertEquals("", viewModel.searchQuery.value)
        viewModel.onSearchQueryChange("회의")
        assertEquals(listOf("회의"), titles())

        viewModel.closeSearch()
        assertNull(viewModel.searchQuery.value)
        assertEquals(listOf("회의", "장보기"), titles())
    }

    @Test
    fun togglePinFlipsTheFlag() = runTest(dispatcher) {
        val memo = repository.add("a")
        subscribe()

        viewModel.togglePin(memo)

        assertTrue(repository.memos.value.single().isPinned)
    }

    @Test
    fun pinFailureIsReported() = runTest(dispatcher) {
        val memo = repository.add("a")
        repository.failWrites = true

        viewModel.togglePin(memo)

        assertEquals(Unit, viewModel.pinFailures.first())
    }

    @Test
    fun readFailureBecomesFailedState() = runTest(dispatcher) {
        repository.failObserve = true
        subscribe()

        assertEquals(MemoListUiState.Failed, viewModel.uiState.value)
    }
}
