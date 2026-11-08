package io.wit117.thoughtweave.feature.synthesis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import io.wit117.thoughtweave.domain.repository.MemoRepository
import io.wit117.thoughtweave.domain.usecase.AiAvailabilityUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Synthesis needs at least this many selected memos (S03 FAB rule). */
const val MIN_MEMOS_FOR_SYNTHESIS = 2

sealed interface SynthesisUiState {
    data object Loading : SynthesisUiState
    data object Failed : SynthesisUiState

    /** [selectedIds] is in selection order and only holds memos that still exist. */
    data class Loaded(
        val memos: List<Memo>,
        val selectedIds: List<Long>,
        val aiAvailability: AiAvailability,
    ) : SynthesisUiState {
        val canSynthesize: Boolean
            get() = aiAvailability.isReady && selectedIds.size >= MIN_MEMOS_FOR_SYNTHESIS
    }
}

/** S03 – AI 메모 통합. */
@HiltViewModel
class SynthesisViewModel @Inject constructor(
    memoRepository: MemoRepository,
    aiAvailability: AiAvailabilityUseCase,
    private val agreementRepository: AgreementRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selection = savedStateHandle.getStateFlow(KEY_SELECTION, LongArray(0))

    val uiState: StateFlow<SynthesisUiState> =
        combine(memoRepository.observeMemos(), selection, aiAvailability()) { memos, selected, availability ->
            val existing = memos.mapTo(HashSet()) { it.id }
            SynthesisUiState.Loaded(
                memos = memos,
                selectedIds = selected.filter { it in existing },
                aiAvailability = availability,
            ) as SynthesisUiState
        }
            .catch { emit(SynthesisUiState.Failed) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SynthesisUiState.Loading,
            )

    fun acceptAgreement() = agreementRepository.accept()

    fun toggleSelection(memoId: Long) {
        val current = selection.value.toList()
        val updated = if (memoId in current) current - memoId else current + memoId
        savedStateHandle[KEY_SELECTION] = updated.toLongArray()
    }

    /** Called only after S02-C saved successfully; cancel, back and failure keep the selection. */
    fun clearSelection() {
        savedStateHandle[KEY_SELECTION] = LongArray(0)
    }

    private companion object {
        const val KEY_SELECTION = "selection"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
