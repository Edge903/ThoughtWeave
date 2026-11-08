package io.wit117.thoughtweave.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import java.time.Instant

@Entity(
    tableName = "memos",
    indices = [Index("is_pinned", "updated_at")],
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    val origin: MemoOrigin,
)

fun MemoEntity.toDomain() = Memo(
    id = id,
    title = title,
    content = content,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    isPinned = isPinned,
    origin = origin,
)
