package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.data.ai.parse.ScheduleDates
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Dates written with a comma separator (docs/TODO.md, "Schedule extraction misses comma-separated dates"). */
class CommaDateFormatsTest {
    /** A Monday. December 3, 2026 is a Thursday. */
    private val today = LocalDate.of(2026, 11, 9)
    private val dec3 = LocalDate.of(2026, 12, 3)
    private val dec3NextYear = LocalDate.of(2027, 12, 3)

    private fun read(text: String) = readScheduleDates(text, today)

    @Test
    fun monthDayCommaYear() {
        assertEquals(ScheduleDates(dec3), read("Submit by December 3, 2026"))
    }

    @Test
    fun monthDayCommaYearUsesTheWrittenYear() {
        assertEquals(ScheduleDates(dec3NextYear), read("Submit by December 3, 2027"))
    }

    @Test
    fun abbreviatedMonthWithPeriodCommaYear() {
        assertEquals(ScheduleDates(dec3NextYear), read("Due Dec. 3, 2027"))
    }

    @Test
    fun commaYearWithoutSpace() {
        assertEquals(ScheduleDates(dec3NextYear), read("Due December 3,2027"))
    }

    @Test
    fun dayMonthCommaYear() {
        assertEquals(ScheduleDates(dec3NextYear), read("Due 3 December, 2027"))
    }

    @Test
    fun commaYearThenTime() {
        assertEquals(ScheduleDates(dec3NextYear, LocalTime.of(17, 0)), read("Dec 3, 2027 at 5pm"))
    }

    @Test
    fun commaBeforeTime() {
        assertEquals(ScheduleDates(dec3, LocalTime.of(17, 0)), read("Dec 3, 5pm"))
    }

    @Test
    fun weekdayCommaMonthDayIsOneDate() {
        assertEquals(ScheduleDates(dec3), read("Thursday, December 3"))
    }

    @Test
    fun weekdayCommaMonthDayCommaYear() {
        assertEquals(ScheduleDates(dec3NextYear), read("Friday, December 3, 2027"))
    }

    @Test
    fun numericYearMonthDayWithCommas() {
        assertEquals(ScheduleDates(dec3NextYear), read("마감 2027, 12, 3"))
        assertEquals(ScheduleDates(dec3NextYear), read("마감 2027,12,3"))
    }

    @Test
    fun koreanDateCommaTime() {
        assertEquals(ScheduleDates(dec3, LocalTime.of(15, 0)), read("12월 3일, 오후 3시 회의"))
    }

    @Test
    fun koreanYearCommaMonthDay() {
        assertEquals(ScheduleDates(dec3NextYear), read("2027년, 12월 3일 출장"))
    }

    @Test
    fun koreanWeekdayCommaDate() {
        assertEquals(ScheduleDates(dec3), read("목요일, 12월 3일 회의"))
    }

    @Test
    fun commaListOfDatesUsesTheFirst() {
        assertEquals(ScheduleDates(dec3), read("12월 3일, 12월 8일 발표"))
    }

    @Test
    fun explicitDateWinsWhenTheWeekdayDisagrees() {
        // December 4, 2026 is a Friday.
        assertEquals(ScheduleDates(LocalDate.of(2026, 12, 4)), read("목요일, 12월 4일 회의"))
        assertEquals(ScheduleDates(LocalDate.of(2026, 12, 4)), read("Thursday, December 4"))
    }

    @Test
    fun weekdayAfterTheDateIsAlsoMerged() {
        assertEquals(ScheduleDates(dec3), read("12월 3일 (목요일) 회의"))
    }

    @Test
    fun separateWeekdayAndDateKeepTheirOrder() {
        // Not attached: the weekday comes first in the text, so it stays the deadline.
        assertEquals(LocalDate.of(2026, 11, 12), read("목요일 회의 준비, 발표는 12월 3일")?.deadlineDate)
    }

    @Test
    fun amountsWithCommasAreNotDates() {
        assertEquals(null, read("예산 3,000원, 인원 12명"))
        assertEquals(null, read("참석 2,027명"))
    }

    @Test
    fun amountsAfterADateAreNotReadAsAYear() {
        assertEquals(ScheduleDates(dec3), read("Dec 3, 1,200 won"))
        assertEquals(ScheduleDates(dec3), read("Dec 3, 2026,500 won raised"))
        assertEquals(ScheduleDates(dec3), read("12월 3일 2027원"))
    }

    @Test
    fun yearWithoutCommaIsAlsoRead() {
        assertEquals(ScheduleDates(dec3NextYear), read("Due December 3 2027"))
    }
}
