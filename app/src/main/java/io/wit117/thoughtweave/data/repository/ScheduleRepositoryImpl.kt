package io.wit117.thoughtweave.data.repository

import io.wit117.thoughtweave.data.local.dao.ScheduleDao
import io.wit117.thoughtweave.data.local.entity.ScheduleEntity
import io.wit117.thoughtweave.data.local.entity.toDomain
import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.ScheduleDraft
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val clock: Clock,
) : ScheduleRepository {

    override fun observeSchedules(): Flow<List<Schedule>> =
        scheduleDao.observeAll().map { list -> list.map(ScheduleEntity::toDomain) }

    override suspend fun getSchedule(id: Long): Schedule? = scheduleDao.getById(id)?.toDomain()

    override suspend fun createSchedule(draft: ScheduleDraft): Long {
        val now = clock.millis()
        return scheduleDao.insert(
            ScheduleEntity(
                title = draft.title.trim(),
                description = draft.description,
                startAt = draft.startAt?.toEpochMilli(),
                endAt = draft.endAt.toEpochMilli(),
                isAllDay = draft.isAllDay,
                memoId = draft.memoId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun updateSchedule(id: Long, draft: ScheduleDraft): Boolean =
        scheduleDao.update(
            id = id,
            title = draft.title.trim(),
            description = draft.description,
            startAt = draft.startAt?.toEpochMilli(),
            endAt = draft.endAt.toEpochMilli(),
            isAllDay = draft.isAllDay,
            memoId = draft.memoId,
            updatedAt = clock.millis(),
        ) > 0

    override suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)
}
