package io.wit117.thoughtweave.domain.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AllDayConversionTest {
    @Test
    fun allDayValueIsUtcMidnight() {
        assertEquals(
            Instant.parse("2026-11-08T00:00:00Z"),
            LocalDate.of(2026, 11, 8).toAllDayInstant(),
        )
    }

    @Test
    fun dateRoundTripsRegardlessOfDefaultZone() {
        val date = LocalDate.of(2026, 12, 31)
        assertEquals(date, date.toAllDayInstant().toAllDayDate())
    }
}
