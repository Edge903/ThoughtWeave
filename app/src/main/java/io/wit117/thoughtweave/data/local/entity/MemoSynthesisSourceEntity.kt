package io.wit117.thoughtweave.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which memos a synthesized memo came from. Stored but never shown in the UI. */
@Entity(
    tableName = "memo_synthesis_sources",
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["synthesized_memo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_memo_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("synthesized_memo_id"), Index("source_memo_id")],
)
data class MemoSynthesisSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "synthesized_memo_id") val synthesizedMemoId: Long,
    @ColumnInfo(name = "source_memo_id") val sourceMemoId: Long?,
    @ColumnInfo(name = "source_title_snapshot") val sourceTitleSnapshot: String,
    val position: Int,
)
