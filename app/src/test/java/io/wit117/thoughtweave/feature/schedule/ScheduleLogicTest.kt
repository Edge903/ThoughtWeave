package io.wit117.thoughtweave.feature.schedule

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import io.wit117.thoughtweave.domain.model.ExtractedSchedule
import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.ScheduleDraft
import io.wit117.thoughtweave.domain.model.toAllDayInstant
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionException
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionRepository
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import io.wit117.thoughtweave.domain.usecase.ExtractScheduleUseCase
import io.wit117.thoughtweave.feature.memo.FakeMemoRepository
import java.time.Clock
import io.wit117.thoughtweave.domain.usecase.SaveScheduleResult
import io.wit117.thoughtweave.domain.usecase.SaveScheduleUseCase
import io.wit117.thoughtweave.domain.usecase.ScheduleInput
import io.wit117.thoughtweave.domain.usecase.ScheduleValidation
import io.wit117.thoughtweave.domain.usecase.ScheduleValidationError
import io.wit117.thoughtweave.domain.usecase.ValidateScheduleDraftUseCase
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorEffect
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorInit
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorViewModel
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleFields
import io.wit117.thoughtweave.feature.schedule.editor.toFields
import io.wit117.thoughtweave.feature.schedule.list.groupSchedulesByDeadline
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeScheduleRepository : ScheduleRepository {
    val schedules = MutableStateFlow<List<Schedule>>(emptyList())
    var failWrites = false
    private var nextId = 1L

    private fun Schedule.applying(draft: ScheduleDraft) = copy(
        title = draft.title.trim(),
        description = draft.description,
        startAt = draft.startAt,
        endAt = draft.endAt,
        isAllDay = draft.isAllDay,
        memoId = draft.memoId,
    )

    override fun observeSchedules(): Flow<List<Schedule>> = schedules
    override suspend fun getSchedule(id: Long) = schedules.value.find { it.id == id }

    override suspend fun createSchedule(draft: ScheduleDraft): Long {
        if (failWrites) throw SQLiteException()
        val schedule = schedule(id = nextId++, end = draft.endAt).applying(draft)
        schedules.value = schedules.value + schedule
        return schedule.id
    }

    override suspend fun updateSchedule(id: Long, draft: ScheduleDraft): Boolean {
        if (failWrites) throw SQLiteException()
        if (schedules.value.none { it.id == id }) return false
        schedules.value = schedules.value.map { if (it.id == id) it.applying(draft) else it }
        return true
    }

    override suspend fun deleteSchedule(id: Long) {
        schedules.value = schedules.value.filterNot { it.id == id }
    }
}

private fun schedule(id: Long, end: Instant, allDay: Boolean = false, start: Instant? = null) = Schedule(
    id = id,
    title = "s$id",
    description = "",
    startAt = start,
    endAt = end,
    isAllDay = allDay,
    memoId = null,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleLogicTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeScheduleRepository()
    private val validate = ValidateScheduleDraftUseCase()
    private val handle = SavedStateHandle()
    private val seoul = ZoneId.of("Asia/Seoul")
    private val deadline = Instant.parse("2026-12-01T09:00:00Z")
    private val date = LocalDate.of(2026, 12, 1)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val memos = FakeMemoRepository()
    private var extraction: () -> ExtractedSchedule = { error("Not used") }
    private val extractionRepository = object : ScheduleExtractionRepository {
        override suspend fun extract(title: String, content: String, today: LocalDate) = extraction()
    }
    private val extract = ExtractScheduleUseCase(memos, extractionRepository, Clock.systemUTC(), seoul)

    private fun viewModel(init: ScheduleEditorInit = ScheduleEditorInit.Manual) = ScheduleEditorViewModel(
        init, repository, SaveScheduleUseCase(repository, validate), validate, extract, seoul, handle,
    )

    @Test
    fun extractionPrefillsLinkedScheduleAndWaitsForSave() = runTest(dispatcher) {
        val memo = memos.add("memo", "12/1 15:00")
        extraction = { ExtractedSchedule("회의", date, LocalTime.of(15, 0)) }

        val viewModel = viewModel(ScheduleEditorInit.AiExtracted(memo.id))

        val state = viewModel.uiState.value
        assertFalse(state.isExtracting)
        assertEquals("회의", state.fields.title)
        assertEquals(date, state.fields.endDate)
        assertFalse(state.fields.isAllDay)
        assertTrue(state.canSave)
        assertTrue(repository.schedules.value.isEmpty())

        viewModel.save()
        assertEquals(memo.id, repository.schedules.value.single().memoId)
    }

    @Test
    fun extractionWithoutTimeIsAllDay() {
        val memo = memos.add("memo", "12/1")
        extraction = { ExtractedSchedule("a", date) }

        assertTrue(viewModel(ScheduleEditorInit.AiExtracted(memo.id)).uiState.value.fields.isAllDay)
    }

    @Test
    fun extractionFailureIsReported() = runTest(dispatcher) {
        val memo = memos.add("memo", "none")
        extraction = { throw ScheduleExtractionException("no date") }

        val viewModel = viewModel(ScheduleEditorInit.AiExtracted(memo.id))

        assertEquals(ScheduleEditorEffect.ExtractionFailed, viewModel.effects.first())
    }

    @Test
    fun missingMemoFailsExtraction() = runTest(dispatcher) {
        val viewModel = viewModel(ScheduleEditorInit.AiExtracted(99))

        assertEquals(ScheduleEditorEffect.ExtractionFailed, viewModel.effects.first())
    }

    @Test
    fun restoredFieldsSkipExtraction() {
        val memo = memos.add("memo", "12/1")
        extraction = { ExtractedSchedule("first", date) }
        viewModel(ScheduleEditorInit.AiExtracted(memo.id)).onTitleChange("edited")
        extraction = { error("Must not run again") }

        assertEquals("edited", viewModel(ScheduleEditorInit.AiExtracted(memo.id)).uiState.value.fields.title)
    }

    @Test
    fun titleAndDeadlineAreRequired() {
        assertEquals(
            ScheduleValidation.Invalid(
                setOf(ScheduleValidationError.TITLE_REQUIRED, ScheduleValidationError.DEADLINE_REQUIRED),
            ),
            validate(ScheduleInput(title = "  ")),
        )
    }

    @Test
    fun startMayEqualButNotExceedDeadline() {
        assertTrue(validate(ScheduleInput("a", startAt = deadline, endAt = deadline)) is ScheduleValidation.Valid)
        assertEquals(
            ScheduleValidation.Invalid(setOf(ScheduleValidationError.START_AFTER_DEADLINE)),
            validate(ScheduleInput("a", startAt = deadline.plusSeconds(1), endAt = deadline)),
        )
    }

    @Test
    fun saveDoesNotWriteInvalidInputAndReportsMissingSchedule() = runTest(dispatcher) {
        val save = SaveScheduleUseCase(repository, validate)

        assertTrue(save(null, ScheduleInput("")) is SaveScheduleResult.Invalid)
        assertTrue(repository.schedules.value.isEmpty())
        assertEquals(SaveScheduleResult.NotFound, save(99, ScheduleInput("a", endAt = deadline)))
    }

    @Test
    fun groupsByLocalDeadlineDateInDateOrder() {
        val pacific = ZoneId.of("America/Los_Angeles")
        val day = LocalDate.of(2026, 12, 1)
        // 20:00 local on Dec 1 is Dec 2 04:00 UTC, later than the Dec 2 all-day value in storage.
        val timedDec1 = schedule(1, Instant.parse("2026-12-02T04:00:00Z"))
        val allDayDec2 = schedule(2, day.plusDays(1).toAllDayInstant(), allDay = true)

        val groups = groupSchedulesByDeadline(listOf(allDayDec2, timedDec1), pacific)

        assertEquals(listOf(day, day.plusDays(1)), groups.map { it.date })
        assertEquals(listOf(listOf(1L), listOf(2L)), groups.map { g -> g.schedules.map { it.id } })
    }

    @Test
    fun allDayComesFirstWithinADate() {
        val timedEarly = schedule(1, Instant.parse("2026-11-30T16:00:00Z")) // Dec 1, 01:00 in Seoul
        val allDay = schedule(2, date.toAllDayInstant(), allDay = true)

        val group = groupSchedulesByDeadline(listOf(timedEarly, allDay), seoul).single()

        assertEquals(date, group.date)
        assertEquals(listOf(2L, 1L), group.schedules.map { it.id })
    }

    @Test
    fun fieldsRoundTripForTimedAndAllDay() {
        val timed = ScheduleFields(
            title = "t",
            startDate = date,
            startTime = LocalTime.of(10, 30),
            endDate = date.plusDays(1),
            endTime = LocalTime.of(23, 15),
        )
        val input = timed.toInput(seoul, null)
        val stored = schedule(1, input.endAt!!, start = input.startAt).copy(title = "t")
        assertEquals(timed, stored.toFields(seoul))

        val allDay = ScheduleFields(title = "t", isAllDay = true, endDate = date).toInput(seoul, null)
        assertEquals(date.toAllDayInstant(), allDay.endAt)
        assertEquals(
            date,
            schedule(2, allDay.endAt!!, allDay = true).toFields(seoul).endDate,
        )
    }

    @Test
    fun errorsAppearOnlyAfterSaveThenFollowEdits() {
        val viewModel = viewModel()
        viewModel.onDescriptionChange("d")
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.save()
        assertEquals(
            setOf(ScheduleValidationError.TITLE_REQUIRED, ScheduleValidationError.DEADLINE_REQUIRED),
            viewModel.uiState.value.errors,
        )
        assertFalse(viewModel.uiState.value.isBusy)

        viewModel.onTitleChange("a")
        assertEquals(setOf(ScheduleValidationError.DEADLINE_REQUIRED), viewModel.uiState.value.errors)
    }

    @Test
    fun validManualScheduleIsSavedAndCloses() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onTitleChange(" a ")
        viewModel.onEndDateChange(date)

        viewModel.save()

        assertEquals(ScheduleEditorEffect.Close, viewModel.effects.first())
        assertEquals("a", repository.schedules.value.single().title)
    }

    @Test
    fun existingScheduleLoadsCleanAndKeepsMemoLink() = runTest(dispatcher) {
        val id = repository.createSchedule(ScheduleDraft("a", endAt = deadline, memoId = 5))
        val viewModel = viewModel(ScheduleEditorInit.Existing(id))
        assertFalse(viewModel.uiState.value.isDirty)
        assertTrue(viewModel.uiState.value.isExisting)

        viewModel.onTitleChange("b")
        viewModel.save()

        assertEquals(ScheduleEditorEffect.Close, viewModel.effects.first())
        val saved = repository.schedules.value.single()
        assertEquals("b", saved.title)
        assertEquals(5L, saved.memoId)
    }

    @Test
    fun clearingStartRemovesIt() = runTest(dispatcher) {
        val id = repository.createSchedule(
            ScheduleDraft("a", startAt = deadline.minusSeconds(60), endAt = deadline),
        )
        val viewModel = viewModel(ScheduleEditorInit.Existing(id))

        viewModel.onClearStart()
        viewModel.save()

        assertEquals(ScheduleEditorEffect.Close, viewModel.effects.first())
        assertEquals(null, repository.schedules.value.single().startAt)
    }

    @Test
    fun saveFailureKeepsEditing() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onTitleChange("a")
        viewModel.onEndDateChange(date)
        repository.failWrites = true

        viewModel.save()

        assertEquals(ScheduleEditorEffect.SaveFailed, viewModel.effects.first())
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun deleteAndMissingSchedule() = runTest(dispatcher) {
        val id = repository.createSchedule(ScheduleDraft("a", endAt = deadline))
        val viewModel = viewModel(ScheduleEditorInit.Existing(id))

        viewModel.delete()

        assertEquals(ScheduleEditorEffect.Close, viewModel.effects.first())
        assertTrue(repository.schedules.value.isEmpty())
        assertTrue(viewModel(ScheduleEditorInit.Existing(99)).uiState.value.isUnavailable)
    }

    @Test
    fun unsavedFieldsAreRestored() {
        viewModel().apply {
            onTitleChange("draft")
            onEndDateChange(date)
            onEndTimeChange(LocalTime.of(7, 5))
        }

        val restored = viewModel().uiState.value.fields

        assertEquals("draft", restored.title)
        assertEquals(date, restored.endDate)
        assertEquals(LocalTime.of(7, 5), restored.endTime)
    }
}
