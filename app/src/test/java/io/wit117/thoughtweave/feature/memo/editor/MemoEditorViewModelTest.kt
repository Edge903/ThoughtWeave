package io.wit117.thoughtweave.feature.memo.editor

import androidx.lifecycle.SavedStateHandle
import io.wit117.thoughtweave.feature.memo.FakeMemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoEditorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeMemoRepository()
    private val handle = SavedStateHandle()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(memoId: Long? = null) = MemoEditorViewModel(memoId, repository, handle)

    @Test
    fun newMemoCannotBeSavedUntilItHasContent() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTitleChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onContentChange("body")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun savingNewMemoCreatesItAndCloses() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onContentChange("body")

        viewModel.save()

        assertEquals(MemoEditorEffect.Close, viewModel.effects.first())
        assertEquals("", repository.memos.value.single().title)
        assertEquals("body", repository.memos.value.single().content)
    }

    @Test
    fun loadsExistingMemoClean() {
        val memo = repository.add("title", "body")

        val state = viewModel(memo.id).uiState.value

        assertTrue(state.isExisting)
        assertEquals(MemoDraft("title", "body"), state.draft)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
    }

    @Test
    fun savingExistingMemoStaysAndClearsDirty() = runTest(dispatcher) {
        val memo = repository.add("title", "body")
        val viewModel = viewModel(memo.id)
        viewModel.onTitleChange("  new  ")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.save()

        assertEquals(MemoEditorEffect.Saved, viewModel.effects.first())
        assertEquals("new", repository.memos.value.single().title)
        assertEquals("new", viewModel.uiState.value.draft.title)
        assertFalse(viewModel.uiState.value.isDirty)
        assertNull(handle.get<String>("draft_title"))
    }

    @Test
    fun saveAndOpenExtractionTargetsTheSavedMemo() = runTest(dispatcher) {
        val memo = repository.add("title", "body")
        val viewModel = viewModel(memo.id)
        viewModel.onContentChange("edited")

        viewModel.saveAndOpenExtraction()

        assertEquals(MemoEditorEffect.OpenExtraction(memo.id), viewModel.effects.first())
        assertEquals("edited", repository.memos.value.single().content)
    }

    @Test
    fun saveFailureKeepsTheDraftDirty() = runTest(dispatcher) {
        val memo = repository.add("title", "body")
        val viewModel = viewModel(memo.id)
        viewModel.onContentChange("edited")
        repository.failWrites = true

        viewModel.save()

        assertEquals(MemoEditorEffect.SaveFailed, viewModel.effects.first())
        val state = viewModel.uiState.value
        assertEquals("edited", state.draft.content)
        assertTrue(state.isDirty)
        assertFalse(state.isBusy)
    }

    @Test
    fun deleteRemovesMemoAndCloses() = runTest(dispatcher) {
        val memo = repository.add("title", "body")
        val viewModel = viewModel(memo.id)

        viewModel.delete()

        assertEquals(MemoEditorEffect.Close, viewModel.effects.first())
        assertTrue(repository.memos.value.isEmpty())
    }

    @Test
    fun missingMemoIsUnavailable() {
        assertTrue(viewModel(42).uiState.value.isUnavailable)
    }

    @Test
    fun restoresUnsavedDraftOverStoredMemo() {
        val memo = repository.add("title", "body")
        viewModel(memo.id).onContentChange("unsaved")

        val restored = viewModel(memo.id).uiState.value

        assertEquals("unsaved", restored.draft.content)
        assertEquals("body", restored.original.content)
        assertTrue(restored.isDirty)
    }

    @Test
    fun veryLongDraftIsNotPersisted() {
        val viewModel = viewModel()
        viewModel.onContentChange("short")
        viewModel.onContentChange("x".repeat(50_001))

        assertNull(handle.get<String>("draft_content"))
    }
}
