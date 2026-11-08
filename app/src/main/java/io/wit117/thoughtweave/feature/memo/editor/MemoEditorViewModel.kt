package io.wit117.thoughtweave.feature.memo.editor

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.repository.MemoRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoEditorUiState(
    val draft: MemoDraft = MemoDraft(),
    val original: MemoDraft = MemoDraft(),
    val isExisting: Boolean = false,
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val isUnavailable: Boolean = false,
) {
    val isDirty: Boolean get() = draft != original
    val canSave: Boolean get() = !isLoading && !isBusy && isDirty && draft.hasContent
}

sealed interface MemoEditorEffect {
    /** The screen is finished: a new memo was created or the memo was deleted. */
    data object Close : MemoEditorEffect
    data object Saved : MemoEditorEffect
    data class OpenExtraction(val memoId: Long) : MemoEditorEffect
    data object SaveFailed : MemoEditorEffect
    data object DeleteFailed : MemoEditorEffect
}

/** S02-A (`memoId == null`) and S02-B. */
@HiltViewModel(assistedFactory = MemoEditorViewModel.Factory::class)
class MemoEditorViewModel @AssistedInject constructor(
    @Assisted private val memoId: Long?,
    private val memoRepository: MemoRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(memoId: Long?): MemoEditorViewModel
    }

    private val _uiState = MutableStateFlow(
        MemoEditorUiState(
            draft = restoredDraft() ?: MemoDraft(),
            isExisting = memoId != null,
            isLoading = memoId != null,
        ),
    )
    val uiState: StateFlow<MemoEditorUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<MemoEditorEffect>(Channel.BUFFERED)
    val effects: Flow<MemoEditorEffect> = effectChannel.receiveAsFlow()

    init {
        if (memoId != null) load(memoId)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val memo = try {
                memoRepository.getMemo(id)
            } catch (_: SQLiteException) {
                null
            }
            if (memo == null) {
                _uiState.update { it.copy(isLoading = false, isUnavailable = true) }
                return@launch
            }
            val stored = MemoDraft(memo.title, memo.content)
            _uiState.update {
                it.copy(draft = restoredDraft() ?: stored, original = stored, isLoading = false)
            }
        }
    }

    fun onTitleChange(title: String) = updateDraft { it.copy(title = title) }

    fun onContentChange(content: String) = updateDraft { it.copy(content = content) }

    private fun updateDraft(transform: (MemoDraft) -> MemoDraft) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
        persistDraft(_uiState.value.draft)
    }

    fun save() = save(openExtractionAfterwards = false)

    /** S02-B: "save first" path of schedule extraction. */
    fun saveAndOpenExtraction() = save(openExtractionAfterwards = true)

    private fun save(openExtractionAfterwards: Boolean) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isBusy = true) }
        val submitted = state.draft
        viewModelScope.launch {
            val effect = try {
                write(submitted, openExtractionAfterwards)
            } catch (_: SQLiteException) {
                _uiState.update { it.copy(isBusy = false) }
                MemoEditorEffect.SaveFailed
            }
            effect?.let { effectChannel.send(it) }
        }
    }

    /** @return the effect to emit, or null if the memo no longer exists. */
    private suspend fun write(
        submitted: MemoDraft,
        openExtractionAfterwards: Boolean,
    ): MemoEditorEffect? {
        if (memoId == null) {
            memoRepository.createMemo(submitted.title, submitted.content)
            markSaved(submitted)
            return MemoEditorEffect.Close
        }
        if (!memoRepository.updateMemo(memoId, submitted.title, submitted.content)) {
            _uiState.update { it.copy(isBusy = false, isUnavailable = true) }
            return null
        }
        markSaved(submitted)
        return if (openExtractionAfterwards) {
            MemoEditorEffect.OpenExtraction(memoId)
        } else {
            MemoEditorEffect.Saved
        }
    }

    /** Text typed while the write was running stays in the draft and remains unsaved. */
    private fun markSaved(submitted: MemoDraft) {
        val stored = submitted.copy(title = submitted.title.trim())
        _uiState.update {
            it.copy(
                original = stored,
                draft = if (it.draft == submitted) stored else it.draft,
                isBusy = false,
            )
        }
        val current = _uiState.value
        if (current.isDirty) persistDraft(current.draft) else clearPersistedDraft()
    }

    fun delete() {
        val id = memoId ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                memoRepository.deleteMemo(id)
                effectChannel.send(MemoEditorEffect.Close)
            } catch (_: SQLiteException) {
                _uiState.update { it.copy(isBusy = false) }
                effectChannel.send(MemoEditorEffect.DeleteFailed)
            }
        }
    }

    private fun restoredDraft(): MemoDraft? {
        val title = savedStateHandle.get<String>(KEY_TITLE) ?: return null
        val content = savedStateHandle.get<String>(KEY_CONTENT) ?: return null
        return MemoDraft(title, content)
    }

    /** Saved state crosses Binder, so very long drafts are left out instead of risking a crash. */
    private fun persistDraft(draft: MemoDraft) {
        if (draft.title.length + draft.content.length > MAX_PERSISTED_DRAFT_CHARS) {
            clearPersistedDraft()
        } else {
            savedStateHandle[KEY_TITLE] = draft.title
            savedStateHandle[KEY_CONTENT] = draft.content
        }
    }

    private fun clearPersistedDraft() {
        savedStateHandle.remove<String>(KEY_TITLE)
        savedStateHandle.remove<String>(KEY_CONTENT)
    }

    private companion object {
        const val KEY_TITLE = "draft_title"
        const val KEY_CONTENT = "draft_content"
        const val MAX_PERSISTED_DRAFT_CHARS = 50_000
    }
}
