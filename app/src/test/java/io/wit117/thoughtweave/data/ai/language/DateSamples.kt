package io.wit117.thoughtweave.data.ai.language

import io.wit117.thoughtweave.data.ai.parse.ScheduleDates
import java.time.LocalDate
import java.time.LocalTime

/** The date every sample is read against: Monday, 2026-11-09. */
val SAMPLE_TODAY: LocalDate = LocalDate.of(2026, 11, 9)

/**
 * Required for every language with date rules (checked by [LanguageContractTest]): how people write
 * dates in that language, with the expected reading. Cover at least a written-out date, a relative day,
 * a weekday, a time, and a range.
 */
val DATE_SAMPLES: Map<String, List<Pair<String, ScheduleDates>>> = mapOf(
    "en" to listOf(
        "Submit by December 3 at 5pm" to ScheduleDates(LocalDate.of(2026, 12, 3), LocalTime.of(17, 0)),
        "Call tomorrow" to ScheduleDates(LocalDate.of(2026, 11, 10)),
        "Meet next Friday" to ScheduleDates(LocalDate.of(2026, 11, 20)),
        "Review on 12/3 14:05" to ScheduleDates(LocalDate.of(2026, 12, 3), LocalTime.of(14, 5)),
        "Trip from Dec 1 to Dec 3" to ScheduleDates(LocalDate.of(2026, 12, 3), null, LocalDate.of(2026, 12, 1)),
    ),
    "ko" to listOf(
        "12월 3일 오후 3시 회의" to ScheduleDates(LocalDate.of(2026, 12, 3), LocalTime.of(15, 0)),
        "내일 치과" to ScheduleDates(LocalDate.of(2026, 11, 10)),
        "다음주 금요일 회식" to ScheduleDates(LocalDate.of(2026, 11, 20)),
        "12/3 14:05 리뷰" to ScheduleDates(LocalDate.of(2026, 12, 3), LocalTime.of(14, 5)),
        "11.25 보고서 마감" to ScheduleDates(LocalDate.of(2026, 11, 25)),
        "12월 1일부터 12월 3일까지 출장" to ScheduleDates(LocalDate.of(2026, 12, 3), null, LocalDate.of(2026, 12, 1)),
    ),
)
