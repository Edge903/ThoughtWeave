package io.wit117.thoughtweave.feature.schedule.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.toAllDayDate
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ScheduleGroup(val date: LocalDate, val schedules: List<Schedule>)

sealed interface ScheduleListUiState {
    data object Loading : ScheduleListUiState
    data class Loaded(val groups: List<ScheduleGroup>) : ScheduleListUiState
    data object Failed : ScheduleListUiState
}

/**
 * Groups by deadline date, earliest first; within a date, all-day items come first. The stored
 * order cannot be used directly because all-day values are UTC midnights, not local times.
 */
fun groupSchedulesByDeadline(schedules: List<Schedule>, zone: ZoneId): List<ScheduleGroup> =
    schedules
        .groupBy { schedule ->
            if (schedule.isAllDay) {
                schedule.endAt.toAllDayDate()
            } else {
                schedule.endAt.atZone(zone).toLocalDate()
            }
        }
        .toSortedMap()
        .map { (date, items) ->
            ScheduleGroup(date, items.sortedWith(compareBy({ !it.isAllDay }, { it.endAt }, { it.id })))
        }

/** S04 – 일정 확인. */
@HiltViewModel
class ScheduleListViewModel @Inject constructor(
    repository: ScheduleRepository,
    private val zone: ZoneId,
) : ViewModel() {

    val uiState: StateFlow<ScheduleListUiState> = repository.observeSchedules()
        .map<List<Schedule>, ScheduleListUiState> {
            ScheduleListUiState.Loaded(groupSchedulesByDeadline(it, zone))
        }
        .catch { emit(ScheduleListUiState.Failed) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ScheduleListUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
