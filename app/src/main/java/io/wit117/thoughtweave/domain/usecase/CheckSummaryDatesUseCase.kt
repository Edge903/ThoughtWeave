package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.Memo
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Reads the dates written in a text, as the schedule extraction does. */
interface TextDateReader {
    fun datesIn(text: String, language: AppLanguage, today: LocalDate): Set<LocalDate>
}

/** Dates in the sources that the summary lacks, and dates in the summary that no source has. */
data class SummaryDateCheck(val missing: List<LocalDate>, val added: List<LocalDate>) {
    val hasProblems: Boolean get() = missing.isNotEmpty() || added.isNotEmpty()
}

/**
 * The model sometimes rewrites dates ("12.2" becomes "12일 후", "11.25" becomes "다음 주 수요일"), so a summary's
 * dates are compared with the sources'. Dates are compared as calendar days, so "12.2" and "12월 2일" match.
 */
class CheckSummaryDatesUseCase @Inject constructor(
    private val reader: TextDateReader,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    operator fun invoke(sources: List<Memo>, summary: String, language: AppLanguage): SummaryDateCheck {
        val today = LocalDate.now(clock.withZone(zone))
        val sourceDates = reader.datesIn(sources.joinToString("\n") { it.title + "\n" + it.content }, language, today)
        val summaryDates = reader.datesIn(summary, language, today)
        return SummaryDateCheck(
            missing = (sourceDates - summaryDates).sorted(),
            added = (summaryDates - sourceDates).sorted(),
        )
    }
}
