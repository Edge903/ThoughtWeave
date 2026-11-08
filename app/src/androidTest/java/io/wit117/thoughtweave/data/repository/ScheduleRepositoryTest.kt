package io.wit117.thoughtweave.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.wit117.thoughtweave.data.local.database.ThoughtWeaveDatabase
import io.wit117.thoughtweave.domain.model.ScheduleDraft
import io.wit117.thoughtweave.domain.model.toAllDayDate
import io.wit117.thoughtweave.domain.model.toAllDayInstant
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleRepositoryTest {
    private lateinit var database: ThoughtWeaveDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: ScheduleRepositoryImpl
    private lateinit var memos: MemoRepositoryImpl

    private val deadline = Instant.parse("2026-12-01T09:00:00Z")

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        clock = MutableClock()
        repository = ScheduleRepositoryImpl(database.scheduleDao(), clock)
        memos = MemoRepositoryImpl(database.memoDao(), clock)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun storesOptionalStartAsNull() = runBlocking {
        val id = repository.createSchedule(ScheduleDraft(title = " 마감 ", endAt = deadline))

        val schedule = repository.getSchedule(id)!!
        assertEquals("마감", schedule.title)
        assertNull(schedule.startAt)
        assertEquals(deadline, schedule.endAt)
        assertFalse(schedule.isAllDay)
    }

    @Test
    fun ordersByEarliestDeadline() = runBlocking {
        repository.createSchedule(ScheduleDraft(title = "late", endAt = deadline.plusSeconds(60)))
        repository.createSchedule(ScheduleDraft(title = "early", endAt = deadline))

        assertEquals(
            listOf("early", "late"),
            repository.observeSchedules().first().map { it.title },
        )
    }

    @Test
    fun allDayDateSurvivesStorage() = runBlocking {
        val date = LocalDate.of(2026, 12, 25)
        val id = repository.createSchedule(
            ScheduleDraft(title = "휴일", endAt = date.toAllDayInstant(), isAllDay = true),
        )

        val schedule = repository.getSchedule(id)!!
        assertTrue(schedule.isAllDay)
        assertEquals(date, schedule.endAt.toAllDayDate())
    }

    @Test
    fun updateReplacesFieldsAndKeepsCreatedAt() = runBlocking {
        val id = repository.createSchedule(ScheduleDraft(title = "a", endAt = deadline))
        val createdAt = repository.getSchedule(id)!!.createdAt
        clock.advance()
        val start = deadline.minusSeconds(3_600)

        val updated = repository.updateSchedule(
            id,
            ScheduleDraft(title = "b", description = "d", startAt = start, endAt = deadline),
        )

        assertTrue(updated)
        val schedule = repository.getSchedule(id)!!
        assertEquals("b", schedule.title)
        assertEquals("d", schedule.description)
        assertEquals(start, schedule.startAt)
        assertEquals(createdAt, schedule.createdAt)
        assertEquals(clock.now, schedule.updatedAt)
    }

    @Test
    fun updateReportsMissingSchedule() = runBlocking {
        assertFalse(repository.updateSchedule(9_999, ScheduleDraft(title = "x", endAt = deadline)))
    }

    @Test
    fun delete() = runBlocking {
        val id = repository.createSchedule(ScheduleDraft(title = "a", endAt = deadline))

        repository.deleteSchedule(id)

        assertNull(repository.getSchedule(id))
    }

    @Test
    fun deletingLinkedMemoKeepsScheduleWithoutLink() = runBlocking {
        val memoId = memos.createMemo("memo", "body")
        val id = repository.createSchedule(
            ScheduleDraft(title = "a", endAt = deadline, memoId = memoId),
        )
        assertEquals(memoId, repository.getSchedule(id)!!.memoId)

        memos.deleteMemo(memoId)

        assertNull(repository.getSchedule(id)!!.memoId)
    }
}
