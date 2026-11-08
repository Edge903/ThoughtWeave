package io.wit117.thoughtweave.data.repository

import io.wit117.thoughtweave.data.local.dao.MemoDao
import io.wit117.thoughtweave.data.local.dao.toLikeContainsPattern
import io.wit117.thoughtweave.data.local.entity.MemoEntity
import io.wit117.thoughtweave.data.local.entity.toDomain
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import io.wit117.thoughtweave.domain.repository.MemoRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoRepositoryImpl @Inject constructor(
    private val memoDao: MemoDao,
    private val clock: Clock,
) : MemoRepository {

    override fun observeMemos(query: String): Flow<List<Memo>> {
        val trimmed = query.trim()
        val memos = if (trimmed.isEmpty()) {
            memoDao.observeAll()
        } else {
            memoDao.observeMatching(toLikeContainsPattern(trimmed))
        }
        return memos.map { list -> list.map(MemoEntity::toDomain) }
    }

    override fun observeMemo(id: Long): Flow<Memo?> =
        memoDao.observeById(id).map { it?.toDomain() }

    override suspend fun getMemo(id: Long): Memo? = memoDao.getById(id)?.toDomain()

    override suspend fun createMemo(title: String, content: String): Long =
        memoDao.insert(newMemo(title, content, MemoOrigin.MANUAL))

    override suspend fun updateMemo(id: Long, title: String, content: String): Boolean =
        memoDao.update(id, title.trim(), content, clock.millis()) > 0

    override suspend fun setPinned(id: Long, pinned: Boolean) = memoDao.setPinned(id, pinned)

    override suspend fun deleteMemo(id: Long) = memoDao.deleteById(id)

    override suspend fun createSynthesizedMemo(
        title: String,
        content: String,
        sourceMemoIds: List<Long>,
    ): Long = memoDao.insertSynthesized(
        memo = newMemo(title, content, MemoOrigin.SYNTHESIZED),
        sourceMemoIds = sourceMemoIds,
    )

    private fun newMemo(title: String, content: String, origin: MemoOrigin): MemoEntity {
        val now = clock.millis()
        return MemoEntity(
            title = title.trim(),
            content = content,
            createdAt = now,
            updatedAt = now,
            isPinned = false,
            origin = origin,
        )
    }
}
