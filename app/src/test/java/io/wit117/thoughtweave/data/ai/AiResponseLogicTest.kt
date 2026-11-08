package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.data.ai.engine.InferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferenceException
import io.wit117.thoughtweave.data.ai.parse.ScheduleDates
import io.wit117.thoughtweave.data.ai.parse.cleanTitle
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import io.wit117.thoughtweave.data.ai.prompt.scheduleTitlePrompt
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
import io.wit117.thoughtweave.data.repository.MemoSynthesisRepositoryImpl
import io.wit117.thoughtweave.data.repository.ScheduleExtractionRepositoryImpl
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import io.wit117.thoughtweave.domain.repository.MemoSynthesisException
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private class ScriptedEngine(private val reply: () -> Flow<String>) : InferenceEngine {
    var calls = 0
    override fun generate(prompt: String, temperature: Double): Flow<String> {
        calls++
        return reply()
    }
}

class AiResponseLogicTest {
    /** A Monday. */
    private val today = LocalDate.of(2026, 11, 9)
    private val dec3 = LocalDate.of(2026, 12, 3)

    private fun read(text: String) = readScheduleDates(text, today)
    private fun memo(content: String, title: String = "") =
        Memo(1, title, content, Instant.EPOCH, Instant.EPOCH, false, MemoOrigin.MANUAL)

    @Test
    fun readsKoreanDateAndTime() {
        assertEquals(ScheduleDates(dec3, LocalTime.of(15, 0)), read("12월 3일 오후 3시 회의"))
        assertEquals(ScheduleDates(dec3, LocalTime.of(9, 30)), read("12월 3일 오전 9시 30분"))
        assertEquals(ScheduleDates(dec3, LocalTime.of(15, 30)), read("12월 3일 오후 3시 반"))
    }

    @Test
    fun readsEnglishDateForms() {
        assertEquals(ScheduleDates(dec3, LocalTime.of(17, 0)), read("Submit by December 3 at 5pm."))
        assertEquals(ScheduleDates(dec3), read("Dec 3rd review"))
        assertEquals(ScheduleDates(dec3), read("3 December review"))
        assertEquals(ScheduleDates(dec3, LocalTime.of(14, 5)), read("12/3 14:05"))
    }

    @Test
    fun aFullDateIsNotAlsoReadAsShortDate() {
        assertEquals(ScheduleDates(LocalDate.of(2027, 1, 2)), read("2027-01-02 출장"))
        assertEquals(ScheduleDates(LocalDate.of(2027, 1, 2)), read("2027년 1월 2일 출장"))
    }

    @Test
    fun readsRelativeDays() {
        assertEquals(today, read("오늘 장보기")?.deadlineDate)
        val tomorrow = ScheduleDates(today.plusDays(1), LocalTime.of(15, 0))
        assertEquals(tomorrow, read("내일 오후 3시에 치과 예약"))
        assertEquals(today.plusDays(2), read("모레 출발")?.deadlineDate)
        assertEquals(today.plusDays(1), read("Call tomorrow")?.deadlineDate)
    }

    @Test
    fun readsWeekdays() {
        val friday = LocalDate.of(2026, 11, 13)
        assertEquals(friday, read("금요일 회식")?.deadlineDate)
        assertEquals(friday.plusWeeks(1), read("다음주 금요일 회식")?.deadlineDate)
        assertEquals(friday, read("Meet on Friday")?.deadlineDate)
        assertEquals(friday.plusWeeks(1), read("Meet next Friday")?.deadlineDate)
        assertEquals(today, read("월요일 보고")?.deadlineDate)
    }

    @Test
    fun aPassedDateMeansNextYear() {
        assertEquals(LocalDate.of(2027, 3, 1), read("3월 1일 개강")?.deadlineDate)
    }

    @Test
    fun theThreeInADateIsNotATime() {
        assertNull(read("12월 3일 회의")?.deadlineTime)
    }

    @Test
    fun readsRanges() {
        val dec1 = LocalDate.of(2026, 12, 1)
        assertEquals(ScheduleDates(dec3, null, dec1), read("12월 1일부터 12월 3일까지 출장"))
        assertEquals(
            ScheduleDates(dec3, LocalTime.of(17, 0), dec3, LocalTime.of(15, 0)),
            read("12월 3일 오후 3시부터 오후 5시까지"),
        )
    }

    @Test
    fun twoUnrelatedDatesUseTheFirst() {
        assertEquals(ScheduleDates(dec3), read("12월 3일 제출, 12월 8일 발표"))
    }

    @Test
    fun rejectsMemosWithoutARealDate() {
        assertNull(read("장보기 목록과 아이디어"))
        assertNull(read("우유 3개, 계란 12개"))
        assertNull(read("2월 30일 회의"))
    }

    @Test
    fun cleansTitles() {
        assertEquals("치과 예약", cleanTitle("**치과 예약**\n추가 설명"))
        assertEquals("Review", cleanTitle("\n\"Review\""))
        assertNull(cleanTitle("  \n "))
    }

    @Test
    fun koreanMemosGetAKoreanPrompt() {
        assertTrue("한국어로" in summaryPrompt(listOf(memo("회의 내용"))))
        assertFalse("한국어로" in summaryPrompt(listOf(memo("Meeting notes"))))
        assertTrue("한국어로" in scheduleTitlePrompt("치과", "예약"))
    }

    @Test
    fun titlePromptUsesOnlyTheStartOfLongMemos() {
        val prompt = scheduleTitlePrompt("", "x".repeat(50_000))
        assertTrue(prompt.length < 2_500)
    }

    @Test
    fun memoWithoutDateNeverReachesTheModel() {
        val engine = ScriptedEngine { flowOf("name") }

        assertThrows(ScheduleExtractionException::class.java) {
            runBlocking { ScheduleExtractionRepositoryImpl(engine).extract("장보기", "우유와 빵", today) }
        }
        assertEquals(0, engine.calls)
    }

    @Test
    fun extractionUsesModelTitleAndWrittenDates() = runTest {
        val engine = ScriptedEngine { flowOf("치과 ", "예약") }

        val result = ScheduleExtractionRepositoryImpl(engine).extract("", "내일 오후 3시 치과", today)

        assertEquals("치과 예약", result.title)
        assertEquals(today.plusDays(1), result.deadlineDate)
        assertEquals(LocalTime.of(15, 0), result.deadlineTime)
    }

    @Test
    fun engineFailuresBecomeFeatureFailures() {
        val failing = ScriptedEngine { flow { throw InferenceException("failed") } }
        assertThrows(ScheduleExtractionException::class.java) {
            runBlocking { ScheduleExtractionRepositoryImpl(failing).extract("", "12/3", today) }
        }
        assertThrows(MemoSynthesisException::class.java) {
            runBlocking { MemoSynthesisRepositoryImpl(failing).summarize(emptyList()).toList() }
        }
    }

    @Test
    fun tooLongInputIsReported() {
        val tooLong = ScriptedEngine { flow { throw InferenceException("x", null, isInputTooLong = true) } }

        val error = assertThrows(MemoSynthesisException::class.java) {
            runBlocking { MemoSynthesisRepositoryImpl(tooLong).summarize(emptyList()).toList() }
        }

        assertTrue(error.isInputTooLong)
    }
}
