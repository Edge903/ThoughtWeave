package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.data.ai.language.EnglishProfile
import io.wit117.thoughtweave.data.ai.language.KoreanProfile
import io.wit117.thoughtweave.data.ai.parse.ScheduleDates
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Month.day dates such as "11.25" and "12.2", reported 2026-11-11. */
class DotDateFormatsTest {
    /** A Monday. */
    private val today = LocalDate.of(2026, 11, 9)
    private val nov25 = LocalDate.of(2026, 11, 25)
    private val dec2 = LocalDate.of(2026, 12, 2)

    private fun read(text: String) = readScheduleDates(text, today, KoreanProfile)

    @Test
    fun readsTheReportedForms() {
        assertEquals(ScheduleDates(nov25), read("11.25 보고서 마감"))
        assertEquals(ScheduleDates(dec2), read("12.2 발표"))
    }

    @Test
    fun readsWithWeekdayTimeAndTrailingDot() {
        assertEquals(ScheduleDates(nov25, LocalTime.of(14, 0)), read("11.25(화) 14:00 회의"))
        assertEquals(ScheduleDates(nov25), read("마감은 11.25."))
        assertEquals(ScheduleDates(dec2, LocalTime.of(15, 0)), read("12.2 오후 3시"))
    }

    @Test
    fun readsRangesAndLists() {
        assertEquals(ScheduleDates(dec2, null, nov25), read("11.25~12.2 출장"))
        assertEquals(ScheduleDates(nov25), read("11.25, 12.2 발표"))
    }

    @Test
    fun wordsThatStartLikeUnitsAreNotUnits() {
        assertEquals(ScheduleDates(dec2), read("12.2 회의"))
        assertEquals(ScheduleDates(dec2), read("12.2 점심 약속"))
        assertEquals(ScheduleDates(dec2), read("12.2 도착"))
        assertEquals(ScheduleDates(dec2), read("12.2 분기 보고"))
        assertEquals(ScheduleDates(dec2), read("12.2 초안 제출"))
    }

    @Test
    fun aPassedDateMeansNextYear() {
        assertEquals(LocalDate.of(2027, 3, 1), read("3.1 개강")?.deadlineDate)
    }

    @Test
    fun decimalsWithUnitsAreNotDates() {
        assertNull(read("회의 3.5시간 소요"))
        assertNull(read("2.5kg 구매"))
        assertNull(read("예산 3.5만원"))
        assertNull(read("1.5배 증가"))
        assertNull(read("성장률 2.5%"))
        assertNull(read("평점 4.5점"))
        assertNull(read("2.5 kg 구매"))
        assertNull(read("2.5 시간 소요"))
    }

    @Test
    fun versionsAndLongerNumbersAreNotDates() {
        assertNull(read("v1.2 배포"))
        assertNull(read("버전 1.2.3 배포"))
        assertNull(read("13.45 측정"))
    }

    @Test
    fun fullDatesStillWin() {
        assertEquals(ScheduleDates(LocalDate.of(2027, 1, 2)), read("2027.1.2 출장"))
    }

    @Test
    fun englishInputDoesNotUseKoreanDotOrder() {
        // Month.day is a Korean convention; English memos use month/day.
        assertNull(readScheduleDates("Due 12.2", today, EnglishProfile))
    }
}
