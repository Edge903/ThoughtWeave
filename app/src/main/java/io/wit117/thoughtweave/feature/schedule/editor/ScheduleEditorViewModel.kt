package io.wit117.thoughtweave.feature.schedule.editor

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import io.wit117.thoughtweave.domain.usecase.SaveScheduleResult
import io.wit117.thoughtweave.domain.usecase.SaveScheduleUseCase
import io.wit117.thoughtweave.domain.usecase.ScheduleValidation
import io.wit117.thoughtweave.domain.usecase.ScheduleValidationError
import io.wit117.thoughtweave.domain.usecase.ValidateScheduleDraftUseCase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the shared editor starts. Phase 5c adds the AI-extracted case. */
sealed interface ScheduleEditorInit {
    data object Manual : ScheduleEditorInit
    data class Existing(val scheduleId: Long) : ScheduleEditorInit
}

data class ScheduleEditorUiState(
    val fields: ScheduleFields = ScheduleFields(),
    val original: ScheduleFields = ScheduleFields(),
    val isExisting: Boolean = false,
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val isUnavailable: Boolean = false,
    /** Stays empty until the first save attempt, then follows each edit. */
    val errors: Set<ScheduleValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = fields != original
    val canSave: Boolean get() = !isLoading && !isBusy && isDirty
}

sealed interface ScheduleEditorEffect {
    data object Close : ScheduleEditorEffect
    data object SaveFailed : ScheduleEditorEffect
    data object DeleteFailed : ScheduleEditorEffect
}

/** POP02 (`Existing`) and POP03 manual mode (`Manual`). */
@HiltViewModel(assistedFactory = ScheduleEditorViewModel.Factory::class)
class ScheduleEditorViewModel @AssistedInject constructor(
    @Assisted init: ScheduleEditorInit,
    private val repository: ScheduleRepository,
    private val saveSchedule: SaveScheduleUseCase,
    private val validate: ValidateScheduleDraftUseCase,
    private val zone: ZoneId,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(init: ScheduleEditorInit): ScheduleEditorViewModel
    }

    private val store = ScheduleFieldsStore(savedStateHandle)
    private val scheduleId = (init as? ScheduleEditorInit.Existing)?.scheduleId
    private var memoId: Long? = null
    private var hasAttemptedSave = false

    private val _uiState = MutableStateFlow(
        ScheduleEditorUiState(
            fields = store.restore() ?: ScheduleFields(),
            isExisting = scheduleId != null,
            isLoading = scheduleId != null,
        ),
    )
    val uiState: StateFlow<ScheduleEditorUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<ScheduleEditorEffect>(Channel.BUFFERED)
    val effects: Flow<ScheduleEditorEffect> = effectChannel.receiveAsFlow()

    init {
        scheduleId?.let(::load)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val schedule = try {
                repository.getSchedule(id)
            } catch (_: SQLiteException) {
                null
            }
            if (schedule == null) {
                _uiState.update { it.copy(isLoading = false, isUnavailable = true) }
                return@launch
            }
            memoId = schedule.memoId
            val stored = schedule.toFields(zone)
            _uiState.update {
                it.copy(fields = store.restore() ?: stored, original = stored, isLoading = false)
            }
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onAllDayChange(value: Boolean) = edit { it.copy(isAllDay = value) }
    fun onStartDateChange(value: LocalDate) = edit { it.copy(startDate = value) }
    fun onStartTimeChange(value: LocalTime) = edit { it.copy(startTime = value) }
    fun onClearStart() = edit { it.copy(startDate = null) }
    fun onEndDateChange(value: LocalDate) = edit { it.copy(endDate = value) }
    fun onEndTimeChange(value: LocalTime) = edit { it.copy(endTime = value) }

    private fun edit(transform: (ScheduleFields) -> ScheduleFields) {
        _uiState.update {
            val fields = transform(it.fields)
            it.copy(fields = fields, errors = if (hasAttemptedSave) errorsOf(fields) else emptySet())
        }
        store.persist(_uiState.value.fields)
    }

    private fun errorsOf(fields: ScheduleFields): Set<ScheduleValidationError> =
        when (val result = validate(fields.toInput(zone, memoId))) {
            is ScheduleValidation.Invalid -> result.errors
            is ScheduleValidation.Valid -> emptySet()
        }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        hasAttemptedSave = true
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                when (val result = saveSchedule(scheduleId, state.fields.toInput(zone, memoId))) {
                    is SaveScheduleResult.Saved -> {
                        store.clear()
                        effectChannel.send(ScheduleEditorEffect.Close)
                    }
                    is SaveScheduleResult.Invalid ->
                        _uiState.update { it.copy(isBusy = false, errors = result.errors) }
                    SaveScheduleResult.NotFound ->
                        _uiState.update { it.copy(isBusy = false, isUnavailable = true) }
                }
            } catch (_: SQLiteException) {
                _uiState.update { it.copy(isBusy = false) }
                effectChannel.send(ScheduleEditorEffect.SaveFailed)
            }
        }
    }

    fun delete() {
        val id = scheduleId ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                repository.deleteSchedule(id)
                effectChannel.send(ScheduleEditorEffect.Close)
            } catch (_: SQLiteException) {
                _uiState.update { it.copy(isBusy = false) }
                effectChannel.send(ScheduleEditorEffect.DeleteFailed)
            }
        }
    }
}
