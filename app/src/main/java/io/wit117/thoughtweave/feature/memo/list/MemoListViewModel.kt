package io.wit117.thoughtweave.feature.memo.list

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MemoListUiState {
    data object Loading : MemoListUiState
    data class Loaded(val memos: List<Memo>) : MemoListUiState
    data object Failed : MemoListUiState
}

/** S01 – 메모 목록. */
@HiltViewModel
class MemoListViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Null while the search field is closed. Separate from [uiState] to keep typing synchronous. */
    val searchQuery: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_QUERY, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MemoListUiState> = searchQuery
        .flatMapLatest { memoRepository.observeMemos(it.orEmpty()) }
        .map<List<Memo>, MemoListUiState> { MemoListUiState.Loaded(it) }
        .catch { emit(MemoListUiState.Failed) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MemoListUiState.Loading,
        )

    private val pinFailureChannel = Channel<Unit>(Channel.BUFFERED)
    val pinFailures: Flow<Unit> = pinFailureChannel.receiveAsFlow()

    fun openSearch() {
        if (searchQuery.value == null) savedStateHandle[KEY_QUERY] = ""
    }

    fun onSearchQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    fun closeSearch() {
        savedStateHandle[KEY_QUERY] = null
    }

    fun togglePin(memo: Memo) {
        viewModelScope.launch {
            try {
                memoRepository.setPinned(memo.id, !memo.isPinned)
            } catch (_: SQLiteException) {
                pinFailureChannel.send(Unit)
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
