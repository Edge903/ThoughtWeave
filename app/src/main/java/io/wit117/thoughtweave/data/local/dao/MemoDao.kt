package io.wit117.thoughtweave.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.wit117.thoughtweave.data.local.entity.MemoEntity
import io.wit117.thoughtweave.data.local.entity.MemoSynthesisSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY is_pinned DESC, updated_at DESC, id DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    /** [pattern] must come from [toLikeContainsPattern]. */
    @Query(
        """
        SELECT * FROM memos
        WHERE title LIKE :pattern ESCAPE '\' OR content LIKE :pattern ESCAPE '\'
        ORDER BY is_pinned DESC, updated_at DESC, id DESC
        """,
    )
    fun observeMatching(pattern: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id")
    fun observeById(id: Long): Flow<MemoEntity?>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: Long): MemoEntity?

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Query("UPDATE memos SET title = :title, content = :content, updated_at = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, title: String, content: String, updatedAt: Long): Int

    @Query("UPDATE memos SET is_pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertSources(sources: List<MemoSynthesisSourceEntity>)

    @Query("SELECT * FROM memo_synthesis_sources WHERE synthesized_memo_id = :memoId ORDER BY position")
    suspend fun getSources(memoId: Long): List<MemoSynthesisSourceEntity>

    /** Title snapshots are read in the same transaction, so they match the memos as synthesized. */
    @Transaction
    suspend fun insertSynthesized(memo: MemoEntity, sourceMemoIds: List<Long>): Long {
        val sourceMemos = sourceMemoIds.map { sourceId ->
            requireNotNull(getById(sourceId)) { "Source memo $sourceId does not exist" }
        }
        val memoId = insert(memo)
        insertSources(
            sourceMemos.mapIndexed { index, source ->
                MemoSynthesisSourceEntity(
                    synthesizedMemoId = memoId,
                    sourceMemoId = source.id,
                    sourceTitleSnapshot = source.title,
                    position = index,
                )
            },
        )
        return memoId
    }
}

/** Builds a "contains" pattern for `LIKE ... ESCAPE '\'`, matching [query] literally. */
fun toLikeContainsPattern(query: String): String {
    val escaped = query
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}
