package io.wit117.thoughtweave.data.ai.parse

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Dates and times written in a memo. A null time means none was written. */
data class ScheduleDates(
    val deadlineDate: LocalDate,
    val deadlineTime: LocalTime? = null,
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
)

private data class Mention<T>(val range: IntRange, val value: T)

private const val MONTH_NAMES =
    "january|february|march|april|may|june|july|august|september|october|november|december|" +
        "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec"
private const val WEEKDAY_NAMES = "monday|tuesday|wednesday|thursday|friday|saturday|sunday"
private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)

private val FULL_DATE = Regex("(\\d{4})\\s*[년./-]\\s*(\\d{1,2})\\s*[월./-]\\s*(\\d{1,2})\\s*일?")
private val KOREAN_DATE = Regex("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
private val SLASH_DATE = Regex("(?<![\\d/.])(\\d{1,2})/(\\d{1,2})(?![\\d/])")
private val MONTH_DAY = Regex("\\b($MONTH_NAMES)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b", IGNORE_CASE)
private val DAY_MONTH = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+($MONTH_NAMES)\\b", IGNORE_CASE)
private val RELATIVE_DAY = Regex("오늘|내일|모레|글피|\\b(?:today|tonight|tomorrow)\\b", IGNORE_CASE)
private val KOREAN_WEEKDAY = Regex("(다음\\s*주|이번\\s*주)?\\s*([월화수목금토일])요일")
private val ENGLISH_WEEKDAY = Regex("\\b(next|this)?\\s*\\b($WEEKDAY_NAMES)\\b", IGNORE_CASE)

private val KOREAN_TIME = Regex("(오전|오후)?\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분|\\s*(반))?")
private val COLON_TIME = Regex("(오전|오후)?\\s*(?<![\\d:])(\\d{1,2}):(\\d{2})(?:\\s*([ap])\\.?m\\.?\\b)?", IGNORE_CASE)
private val AM_PM_TIME = Regex("\\b(\\d{1,2})\\s*([ap])\\.?m\\b\\.?", IGNORE_CASE)

private val RANGE_CUE = Regex("부터|~|–|\\bfrom\\b|\\bto\\b|\\buntil\\b|\\bthrough\\b|-", IGNORE_CASE)

private val KOREAN_WEEKDAYS = "월화수목금토일"
private val RELATIVE_OFFSETS = mapOf(
    "오늘" to 0L, "내일" to 1L, "모레" to 2L, "글피" to 3L, "today" to 0L, "tonight" to 0L, "tomorrow" to 1L,
)

private fun monthNumber(name: String): Int =
    MONTH_NAMES.split("|").take(12).indexOfFirst { it.startsWith(name.lowercase().take(3)) } + 1

private fun safeDate(year: Int, month: Int, day: Int): LocalDate? = try {
    LocalDate.of(year, month, day)
} catch (_: DateTimeException) {
    null
}

/** A date written without a year is its next occurrence. */
private fun upcoming(today: LocalDate, month: Int, day: Int): LocalDate? {
    val date = safeDate(today.year, month, day) ?: return null
    return if (date.isBefore(today)) safeDate(today.year + 1, month, day) else date
}

private fun weekdayDate(today: LocalDate, day: DayOfWeek, isNextWeek: Boolean): LocalDate =
    if (isNextWeek) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1)
            .with(TemporalAdjusters.nextOrSame(day))
    } else {
        today.with(TemporalAdjusters.nextOrSame(day))
    }

private fun dateMentions(text: String, today: LocalDate): List<Mention<LocalDate>> {
    fun find(regex: Regex, read: (MatchResult) -> LocalDate?) = regex.findAll(text).mapNotNull { match ->
        read(match)?.let { Mention(match.range, it) }
    }.toList()

    fun MatchResult.number(group: Int) = groupValues[group].toInt()

    // Most specific first, so "2026/12/3" is not also read as 12/3.
    val ordered = find(FULL_DATE) { safeDate(it.number(1), it.number(2), it.number(3)) } +
        find(KOREAN_DATE) { upcoming(today, it.number(1), it.number(2)) } +
        find(SLASH_DATE) { upcoming(today, it.number(1), it.number(2)) } +
        find(MONTH_DAY) { upcoming(today, monthNumber(it.groupValues[1]), it.number(2)) } +
        find(DAY_MONTH) { upcoming(today, monthNumber(it.groupValues[2]), it.number(1)) } +
        find(RELATIVE_DAY) { today.plusDays(RELATIVE_OFFSETS.getValue(it.value.lowercase())) } +
        find(KOREAN_WEEKDAY) {
            val day = DayOfWeek.of(KOREAN_WEEKDAYS.indexOf(it.groupValues[2]) + 1)
            weekdayDate(today, day, it.groupValues[1].startsWith("다음"))
        } +
        find(ENGLISH_WEEKDAY) {
            weekdayDate(today, DayOfWeek.valueOf(it.groupValues[2].uppercase()), it.groupValues[1].equals("next", true))
        }
    return withoutOverlaps(ordered)
}

private fun timeOf(period: String, hour: Int, minute: Int): LocalTime? {
    val isAfternoon = period == "오후" || period.equals("p", true)
    val isMorning = period == "오전" || period.equals("a", true)
    val resolved = when {
        isAfternoon && hour < 12 -> hour + 12
        isMorning && hour == 12 -> 0
        else -> hour
    }
    return if (resolved in 0..23 && minute in 0..59) LocalTime.of(resolved, minute) else null
}

private fun timeMentions(text: String): List<Mention<LocalTime>> {
    fun find(regex: Regex, read: (MatchResult) -> LocalTime?) = regex.findAll(text).mapNotNull { match ->
        read(match)?.let { Mention(match.range, it) }
    }.toList()

    val ordered = find(COLON_TIME) {
        val period = it.groupValues[1].ifEmpty { it.groupValues[4] }
        timeOf(period, it.groupValues[2].toInt(), it.groupValues[3].toInt())
    } + find(KOREAN_TIME) {
        val minute = if (it.groupValues[4].isNotEmpty()) 30 else it.groupValues[3].toIntOrNull() ?: 0
        timeOf(it.groupValues[1], it.groupValues[2].toInt(), minute)
    } + find(AM_PM_TIME) { timeOf(it.groupValues[2], it.groupValues[1].toInt(), 0) }
    return withoutOverlaps(ordered)
}

private fun <T> withoutOverlaps(ordered: List<Mention<T>>): List<Mention<T>> {
    val kept = mutableListOf<Mention<T>>()
    for (mention in ordered) {
        if (kept.none { it.range.first <= mention.range.last && mention.range.first <= it.range.last }) {
            kept += mention
        }
    }
    return kept.sortedBy { it.range.first }
}

private fun <T> isRange(text: String, first: Mention<T>, second: Mention<T>): Boolean =
    first.range.last < second.range.first &&
        RANGE_CUE.containsMatchIn(text.substring(first.range.last + 1, second.range.first))

/**
 * Reads the schedule's dates from a memo. Timestamps come only from what is written, never from the
 * model, and a memo with no readable date yields null.
 */
fun readScheduleDates(text: String, today: LocalDate): ScheduleDates? {
    val dates = dateMentions(text, today)
    val times = timeMentions(text)
    val first = dates.firstOrNull() ?: return null

    val dateRange = dates.size >= 2 && isRange(text, dates[0], dates[1]) && dates[1].value >= dates[0].value
    val timeRange = times.size >= 2 && isRange(text, times[0], times[1])
    val hasStartTime = timeRange && (dateRange || times[1].value >= times[0].value)

    return ScheduleDates(
        deadlineDate = if (dateRange) dates[1].value else first.value,
        deadlineTime = (if (hasStartTime) times[1] else times.firstOrNull())?.value,
        startDate = if (dateRange || hasStartTime) first.value else null,
        startTime = if (hasStartTime) times[0].value else null,
    )
}

/** Cleans the model's title reply; null if nothing usable remains. */
fun cleanTitle(reply: String): String? = reply.lineSequence()
    .map { it.trim().trim('*', '#', '"', '\'', '`', ' ') }
    .firstOrNull { it.isNotEmpty() }
    ?.take(MAX_TITLE_LENGTH)

private const val MAX_TITLE_LENGTH = 60
