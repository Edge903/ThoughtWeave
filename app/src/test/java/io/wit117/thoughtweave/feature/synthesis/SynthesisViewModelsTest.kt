package io.wit117.thoughtweave.feature.synthesis

import androidx.lifecycle.SavedStateHandle
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.MemoSynthesisException
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import io.wit117.thoughtweave.feature.memo.FakeMemoRepository
import io.wit117.thoughtweave.feature.memo.editor.MemoDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSynthesis(
    override val isAvailable: Boolean = true,
    var result: () -> Flow<String> = { flowOf("sum", "mary") },
) : MemoSynthesisRepository {
    var received: List<Memo>? = null

    override fun summarize(memos: List<Memo>): Flow<String> {
        received = memos
        return result()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SynthesisViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val memos = FakeMemoRepository()
    private val handle = SavedStateHandle()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun selection(ai: Boolean = true) = SynthesisViewModel(memos, FakeSynthesis(ai), handle)

    private fun SynthesisViewModel.loaded() = uiState.value as SynthesisUiState.Loaded

    private fun result(ids: List<Long>, synthesis: MemoSynthesisRepository = FakeSynthesis()) =
        SynthesisResultViewModel(ids, memos, synthesis, handle)

    @Test
    fun needsTwoSelectedMemosAndAi() = runTest(dispatcher) {
        val a = memos.add("a")
        val b = memos.add("b")
        val viewModel = selection()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        viewModel.toggleSelection(a.id)
        assertFalse(viewModel.loaded().canSynthesize)
        viewModel.toggleSelection(b.id)
        assertTrue(viewModel.loaded().canSynthesize)
        assertEquals(listOf(a.id, b.id), viewModel.loaded().selectedIds)

        viewModel.toggleSelection(a.id)
        assertEquals(listOf(b.id), viewModel.loaded().selectedIds)
    }

    @Test
    fun unavailableAiKeepsActionDisabled() = runTest(dispatcher) {
        val a = memos.add("a")
        val b = memos.add("b")
        val viewModel = selection(ai = false)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        viewModel.toggleSelection(a.id)
        viewModel.toggleSelection(b.id)

        assertFalse(viewModel.loaded().canSynthesize)
    }

    @Test
    fun deletedMemoLeavesSelection_andClearEmptiesIt() = runTest(dispatcher) {
        val a = memos.add("a")
        val b = memos.add("b")
        val viewModel = selection()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.toggleSelection(a.id)
        viewModel.toggleSelection(b.id)

        memos.deleteMemo(a.id)
        assertEquals(listOf(b.id), viewModel.loaded().selectedIds)

        viewModel.clearSelection()
        assertTrue(viewModel.loaded().selectedIds.isEmpty())
    }

    @Test
    fun streamsChunksThenBecomesEditable() {
        val chunks = Channel<String>(Channel.UNLIMITED)
        val a = memos.add("a")
        val viewModel = result(listOf(a.id), FakeSynthesis(result = { chunks.receiveAsFlow() }))

        chunks.trySend("sum")
        assertEquals(SynthesisPhase.GENERATING, viewModel.uiState.value.phase)
        assertEquals("sum", viewModel.uiState.value.draft.content)
        assertFalse(viewModel.uiState.value.isDirty)
        assertFalse(viewModel.uiState.value.canSave)

        chunks.trySend("mary")
        chunks.close()
        assertEquals(SynthesisPhase.READY, viewModel.uiState.value.phase)
        assertEquals("summary", viewModel.uiState.value.draft.content)
        assertTrue(viewModel.uiState.value.isDirty)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun passesSourcesInSelectionOrder() {
        val a = memos.add("a")
        val b = memos.add("b")
        val synthesis = FakeSynthesis()

        result(listOf(b.id, a.id), synthesis)

        assertEquals(listOf(b.id, a.id), synthesis.received?.map { it.id })
    }

    @Test
    fun saveStoresEditedResultWithSources() = runTest(dispatcher) {
        val a = memos.add("a")
        val b = memos.add("b")
        val viewModel = result(listOf(a.id, b.id))
        viewModel.onTitleChange("title")
        viewModel.onContentChange("edited")

        viewModel.save()

        assertEquals(SynthesisResultEffect.Saved, viewModel.effects.first())
        assertEquals(listOf(a.id, b.id), memos.lastSynthesisSources)
        assertEquals("edited", memos.memos.value.last().content)
    }

    @Test
    fun saveFailureKeepsResult() = runTest(dispatcher) {
        val a = memos.add("a")
        val viewModel = result(listOf(a.id))
        memos.failWrites = true

        viewModel.save()

        assertEquals(SynthesisResultEffect.SaveFailed, viewModel.effects.first())
        assertEquals("summary", viewModel.uiState.value.draft.content)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun generationFailureCanBeRetried() {
        val a = memos.add("a")
        val synthesis = FakeSynthesis(result = { flow { throw MemoSynthesisException("failed") } })
        val viewModel = result(listOf(a.id), synthesis)
        assertEquals(SynthesisPhase.FAILED, viewModel.uiState.value.phase)

        synthesis.result = { flowOf("ok") }
        viewModel.retry()

        assertEquals(SynthesisPhase.READY, viewModel.uiState.value.phase)
        assertEquals("ok", viewModel.uiState.value.draft.content)
    }

    @Test
    fun blankResultAndMissingSourceAreFailures() {
        val a = memos.add("a")
        assertEquals(
            SynthesisPhase.FAILED,
            result(listOf(a.id), FakeSynthesis(result = { flowOf("  ") })).uiState.value.phase,
        )
        assertEquals(SynthesisPhase.FAILED, result(listOf(99)).uiState.value.phase)
    }

    @Test
    fun finishedResultIsRestoredWithoutRegenerating() {
        val a = memos.add("a")
        result(listOf(a.id)).onContentChange("edited")
        val synthesis = FakeSynthesis()

        val restored = result(listOf(a.id), synthesis).uiState.value

        assertEquals(SynthesisPhase.READY, restored.phase)
        assertEquals(MemoDraft("", "edited"), restored.draft)
        assertEquals(null, synthesis.received)
    }
}
