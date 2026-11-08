package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.ExtractedSchedule
import java.time.LocalDate

class ScheduleExtractionException(message: String) : Exception(message)

interface ScheduleExtractionRepository {
    /** @throws ScheduleExtractionException if no reliable schedule can be read from the memo. */
    suspend fun extract(title: String, content: String, today: LocalDate): ExtractedSchedule
}
