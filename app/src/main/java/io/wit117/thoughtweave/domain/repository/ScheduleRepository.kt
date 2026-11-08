package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.ScheduleDraft
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    /** Earliest deadline first. */
    fun observeSchedules(): Flow<List<Schedule>>

    suspend fun getSchedule(id: Long): Schedule?

    /** @return the new schedule's id. */
    suspend fun createSchedule(draft: ScheduleDraft): Long

    /** @return false if the schedule no longer exists. */
    suspend fun updateSchedule(id: Long, draft: ScheduleDraft): Boolean

    suspend fun deleteSchedule(id: Long)
}
