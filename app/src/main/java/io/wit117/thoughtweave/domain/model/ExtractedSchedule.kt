package io.wit117.thoughtweave.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** A schedule read from a memo, for the user to review. A null deadline time means all day. */
data class ExtractedSchedule(
    val title: String,
    val deadlineDate: LocalDate,
    val deadlineTime: LocalTime? = null,
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
)
