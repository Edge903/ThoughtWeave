package io.wit117.thoughtweave.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.wit117.thoughtweave.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY end_at, id")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    @Query(
        """
        UPDATE schedules SET title = :title, description = :description, start_at = :startAt,
            end_at = :endAt, is_all_day = :isAllDay, memo_id = :memoId, updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun update(
        id: Long,
        title: String,
        description: String,
        startAt: Long?,
        endAt: Long,
        isAllDay: Boolean,
        memoId: Long?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
