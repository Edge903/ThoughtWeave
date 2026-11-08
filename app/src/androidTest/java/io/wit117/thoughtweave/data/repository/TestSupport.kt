package io.wit117.thoughtweave.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.wit117.thoughtweave.data.local.database.ThoughtWeaveDatabase
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal fun inMemoryDatabase(): ThoughtWeaveDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ThoughtWeaveDatabase::class.java,
    ).build()

internal class MutableClock(var now: Instant = Instant.parse("2026-11-08T00:00:00Z")) : Clock() {
    fun advance() {
        now += Duration.ofMinutes(1)
    }

    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
