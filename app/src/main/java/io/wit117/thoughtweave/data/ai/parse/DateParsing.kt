package io.wit117.thoughtweave.data.ai.parse

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Building blocks shared by every language's date rules. */

internal data class Mention<T>(val range: IntRange, val value: T)

/** A 19xx or 20xx year that is not part of a longer number or an amount ("2,027명", "2027원"). */
internal const val YEAR = "((?:19|20)\\d{2})(?!\\d)(?!,\\d{3})(?!\\s*(?:원|개|명|%))"

internal fun safeDate(year: Int, month: Int, day: Int): LocalDate? = try {
    LocalDate.of(year, month, day)
} catch (_: DateTimeException) {
    null
}

/** A date written without a year is its next occurrence. */
internal fun upcoming(today: LocalDate, month: Int, day: Int): LocalDate? {
    val date = safeDate(today.year, month, day) ?: return null
    return if (date.isBefore(today)) safeDate(today.year + 1, month, day) else date
}

/** A written year is used as it is; only a missing year is inferred. */
internal fun dateOf(today: LocalDate, year: String, month: Int, day: Int): LocalDate? =
    if (year.isEmpty()) upcoming(today, month, day) else safeDate(year.toInt(), month, day)

internal fun weekdayDate(today: LocalDate, day: DayOfWeek, isNextWeek: Boolean): LocalDate =
    if (isNextWeek) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1)
            .with(TemporalAdjusters.nextOrSame(day))
    } else {
        today.with(TemporalAdjusters.nextOrSame(day))
    }

/** [isAfternoon]: true for PM, false for AM, null when no period was written. */
internal fun timeOf(isAfternoon: Boolean?, hour: Int, minute: Int): LocalTime? {
    val resolved = when {
        isAfternoon == true && hour < 12 -> hour + 12
        isAfternoon == false && hour == 12 -> 0
        else -> hour
    }
    return if (resolved in 0..23 && minute in 0..59) LocalTime.of(resolved, minute) else null
}

internal fun MatchResult.number(group: Int) = groupValues[group].toInt()

internal fun <T> withoutOverlaps(ordered: List<Mention<T>>): List<Mention<T>> {
    val kept = mutableListOf<Mention<T>>()
    for (mention in ordered) {
        if (kept.none { it.range.first <= mention.range.last && mention.range.first <= it.range.last }) {
            kept += mention
        }
    }
    return kept.sortedBy { it.range.first }
}
