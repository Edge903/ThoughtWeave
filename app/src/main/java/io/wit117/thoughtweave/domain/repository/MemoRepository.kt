package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    /** Pinned first, then most recently updated. A blank [query] returns every memo. */
    fun observeMemos(query: String = ""): Flow<List<Memo>>

    fun observeMemo(id: Long): Flow<Memo?>

    suspend fun getMemo(id: Long): Memo?

    /** @return the new memo's id. */
    suspend fun createMemo(title: String, content: String): Long

    /** @return false if the memo no longer exists. */
    suspend fun updateMemo(id: Long, title: String, content: String): Boolean

    /** Does not change `updatedAt`, so pinning never reorders memos within their group. */
    suspend fun setPinned(id: Long, pinned: Boolean)

    suspend fun deleteMemo(id: Long)

    /**
     * Atomically writes a synthesized memo and its source references. Source memos are not modified.
     *
     * @return the new memo's id.
     * @throws IllegalArgumentException if a source memo no longer exists; nothing is written.
     */
    suspend fun createSynthesizedMemo(
        title: String,
        content: String,
        sourceMemoIds: List<Long>,
    ): Long
}
