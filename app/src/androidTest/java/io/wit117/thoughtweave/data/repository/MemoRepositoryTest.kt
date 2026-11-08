package io.wit117.thoughtweave.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.data.local.database.ThoughtWeaveDatabase
import io.wit117.thoughtweave.domain.model.MemoOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoRepositoryTest {
    private lateinit var database: ThoughtWeaveDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: MemoRepositoryImpl

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        clock = MutableClock()
        repository = MemoRepositoryImpl(database.memoDao(), clock)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun create(title: String, content: String = "body"): Long {
        clock.advance()
        return repository.createMemo(title, content)
    }

    private suspend fun titles(query: String = "") =
        repository.observeMemos(query).first().map { it.title }

    @Test
    fun createStoresManualMemoWithTrimmedTitle() = runBlocking {
        val id = create("  제목  ", "내용")

        val memo = repository.getMemo(id)!!
        assertEquals("제목", memo.title)
        assertEquals("내용", memo.content)
        assertEquals(clock.now, memo.createdAt)
        assertEquals(clock.now, memo.updatedAt)
        assertFalse(memo.isPinned)
        assertEquals(MemoOrigin.MANUAL, memo.origin)
    }

    @Test
    fun blankTitleIsStoredEmpty() = runBlocking {
        assertEquals("", repository.getMemo(create("   "))!!.title)
    }

    @Test
    fun ordersPinnedFirstThenMostRecentlyUpdated() = runBlocking {
        val a = create("a")
        val b = create("b")
        create("c")
        repository.setPinned(a, true)
        assertEquals(listOf("a", "c", "b"), titles())

        clock.advance()
        repository.updateMemo(b, "b", "edited")
        assertEquals(listOf("a", "b", "c"), titles())
    }

    @Test
    fun pinningDoesNotChangeUpdatedAt() = runBlocking {
        val id = create("a")
        val before = repository.getMemo(id)!!.updatedAt
        clock.advance()

        repository.setPinned(id, true)

        val memo = repository.getMemo(id)!!
        assertTrue(memo.isPinned)
        assertEquals(before, memo.updatedAt)
    }

    @Test
    fun updateKeepsCreatedAtAndReportsMissingMemo() = runBlocking {
        val id = create("a")
        val createdAt = repository.getMemo(id)!!.createdAt
        clock.advance()

        assertTrue(repository.updateMemo(id, "new", "text"))
        val memo = repository.getMemo(id)!!
        assertEquals("new", memo.title)
        assertEquals(createdAt, memo.createdAt)
        assertEquals(clock.now, memo.updatedAt)

        assertFalse(repository.updateMemo(9_999, "x", "y"))
    }

    @Test
    fun searchMatchesTitleOrContent() = runBlocking {
        create("주간 회의", "안건")
        create("장보기", "회의 후 구매")
        create("독서", "소설")

        assertEquals(listOf("장보기", "주간 회의"), titles("회의"))
        assertEquals(3, titles("  ").size)
    }

    @Test
    fun searchTreatsLikeWildcardsLiterally() = runBlocking {
        create("100% done")
        create("100 done")
        create("snake_case")
        create("snakeXcase")

        assertEquals(listOf("100% done"), titles("%"))
        assertEquals(listOf("snake_case"), titles("_"))
    }

    @Test
    fun observeMemoEmitsNullAfterDelete() = runBlocking {
        val id = create("a")
        assertEquals("a", repository.observeMemo(id).first()?.title)

        repository.deleteMemo(id)

        assertNull(repository.observeMemo(id).first())
    }

    @Test
    fun synthesizedMemoStoresOrderedSourcesAndLeavesSourcesUntouched() = runBlocking {
        val first = create("first")
        val second = create("second")
        val firstBefore = repository.getMemo(first)
        clock.advance()

        val id = repository.createSynthesizedMemo("요약", "본문", listOf(second, first))

        assertEquals(MemoOrigin.SYNTHESIZED, repository.getMemo(id)!!.origin)
        assertEquals(firstBefore, repository.getMemo(first))
        val sources = database.memoDao().getSources(id)
        assertEquals(listOf(second, first), sources.map { it.sourceMemoId })
        assertEquals(listOf("second", "first"), sources.map { it.sourceTitleSnapshot })
        assertEquals(listOf(0, 1), sources.map { it.position })
    }

    @Test
    fun synthesisWithMissingSourceWritesNothing() = runBlocking {
        val source = create("source")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createSynthesizedMemo("요약", "본문", listOf(source, 9_999)) }
        }

        assertEquals(listOf("source"), titles())
    }

    @Test
    fun deletingSourceKeepsReferenceSnapshotWithNullLink() = runBlocking {
        val source = create("source")
        val id = repository.createSynthesizedMemo("요약", "본문", listOf(source))

        repository.deleteMemo(source)

        val reference = database.memoDao().getSources(id).single()
        assertNull(reference.sourceMemoId)
        assertEquals("source", reference.sourceTitleSnapshot)
    }

    @Test
    fun deletingSynthesizedMemoRemovesItsReferences() = runBlocking {
        val source = create("source")
        val id = repository.createSynthesizedMemo("요약", "본문", listOf(source))

        repository.deleteMemo(id)

        assertTrue(database.memoDao().getSources(id).isEmpty())
        assertEquals(listOf("source"), titles())
    }
}
