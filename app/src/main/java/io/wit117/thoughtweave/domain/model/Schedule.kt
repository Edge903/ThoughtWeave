package io.wit117.thoughtweave.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class Schedule(
    val id: Long,
    val title: String,
    val description: String,
    val startAt: Instant?,
    /** The deadline. */
    val endAt: Instant,
    val isAllDay: Boolean,
    val memoId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** The user-editable fields of a schedule. */
data class ScheduleDraft(
    val title: String,
    val description: String = "",
    val startAt: Instant? = null,
    val endAt: Instant,
    val isAllDay: Boolean = false,
    val memoId: Long? = null,
)

/** All-day values are UTC midnight of the date, so they do not shift across time zones. */
fun LocalDate.toAllDayInstant(): Instant = atStartOfDay(ZoneOffset.UTC).toInstant()

/** Inverse of [toAllDayInstant]. Only meaningful for all-day values. */
fun Instant.toAllDayDate(): LocalDate = atOffset(ZoneOffset.UTC).toLocalDate()
