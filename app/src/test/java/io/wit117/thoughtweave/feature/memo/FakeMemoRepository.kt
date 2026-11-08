package io.wit117.thoughtweave.feature.memo

import android.database.sqlite.SQLiteException
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import io.wit117.thoughtweave.domain.repository.MemoRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeMemoRepository : MemoRepository {
    val memos = MutableStateFlow<List<Memo>>(emptyList())
    var failWrites = false
    var failObserve = false
    private var nextId = 1L

    fun add(title: String, content: String = ""): Memo {
        val memo = Memo(
            id = nextId++,
            title = title.trim(),
            content = content,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            isPinned = false,
            origin = MemoOrigin.MANUAL,
        )
        memos.value = memos.value + memo
        return memo
    }

    private fun checkWrite() {
        if (failWrites) throw SQLiteException()
    }

    override fun observeMemos(query: String): Flow<List<Memo>> = memos.map { list ->
        if (failObserve) throw SQLiteException()
        list.filter { query in it.title || query in it.content }
    }

    override fun observeMemo(id: Long): Flow<Memo?> = memos.map { list -> list.find { it.id == id } }

    override suspend fun getMemo(id: Long): Memo? = memos.value.find { it.id == id }

    override suspend fun createMemo(title: String, content: String): Long {
        checkWrite()
        return add(title, content).id
    }

    override suspend fun updateMemo(id: Long, title: String, content: String): Boolean {
        checkWrite()
        if (memos.value.none { it.id == id }) return false
        memos.value = memos.value.map {
            if (it.id == id) it.copy(title = title.trim(), content = content) else it
        }
        return true
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        checkWrite()
        memos.value = memos.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
    }

    override suspend fun deleteMemo(id: Long) {
        checkWrite()
        memos.value = memos.value.filterNot { it.id == id }
    }

    override suspend fun createSynthesizedMemo(
        title: String,
        content: String,
        sourceMemoIds: List<Long>,
    ): Long = error("Not used by these tests")
}
