package io.wit117.thoughtweave.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.wit117.thoughtweave.domain.model.Schedule
import java.time.Instant

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["memo_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("end_at"), Index("memo_id")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "start_at") val startAt: Long?,
    @ColumnInfo(name = "end_at") val endAt: Long,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean,
    @ColumnInfo(name = "memo_id") val memoId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    title = title,
    description = description,
    startAt = startAt?.let(Instant::ofEpochMilli),
    endAt = Instant.ofEpochMilli(endAt),
    isAllDay = isAllDay,
    memoId = memoId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)
