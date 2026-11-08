package io.wit117.thoughtweave.feature.schedule.editor

import androidx.lifecycle.SavedStateHandle
import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.toAllDayDate
import io.wit117.thoughtweave.domain.model.toAllDayInstant
import io.wit117.thoughtweave.domain.usecase.ScheduleInput
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val DEFAULT_START_TIME: LocalTime = LocalTime.of(9, 0)
private val DEFAULT_END_TIME: LocalTime = LocalTime.of(18, 0)

/** The schedule editor's fields. Times are kept while all-day is on, so toggling loses nothing. */
data class ScheduleFields(
    val title: String = "",
    val description: String = "",
    val isAllDay: Boolean = false,
    val startDate: LocalDate? = null,
    val startTime: LocalTime = DEFAULT_START_TIME,
    val endDate: LocalDate? = null,
    val endTime: LocalTime = DEFAULT_END_TIME,
) {
    fun toInput(zone: ZoneId, memoId: Long?) = ScheduleInput(
        title = title,
        description = description,
        startAt = startDate?.let { toInstant(it, startTime, zone) },
        endAt = endDate?.let { toInstant(it, endTime, zone) },
        isAllDay = isAllDay,
        memoId = memoId,
    )

    private fun toInstant(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        if (isAllDay) date.toAllDayInstant() else date.atTime(time).atZone(zone).toInstant()
}

fun Schedule.toFields(zone: ZoneId): ScheduleFields {
    fun date(instant: Instant) =
        if (isAllDay) instant.toAllDayDate() else instant.atZone(zone).toLocalDate()

    fun time(instant: Instant, default: LocalTime) =
        if (isAllDay) default else instant.atZone(zone).toLocalTime()

    return ScheduleFields(
        title = title,
        description = description,
        isAllDay = isAllDay,
        startDate = startAt?.let(::date),
        startTime = startAt?.let { time(it, DEFAULT_START_TIME) } ?: DEFAULT_START_TIME,
        endDate = date(endAt),
        endTime = time(endAt, DEFAULT_END_TIME),
    )
}

/** Keeps unsaved fields across process death. */
class ScheduleFieldsStore(private val handle: SavedStateHandle) {

    fun restore(): ScheduleFields? {
        val title = handle.get<String>(KEY_TITLE) ?: return null
        return ScheduleFields(
            title = title,
            description = handle.get<String>(KEY_DESCRIPTION).orEmpty(),
            isAllDay = handle.get<Boolean>(KEY_ALL_DAY) == true,
            startDate = handle.get<String>(KEY_START_DATE)?.let(LocalDate::parse),
            startTime = LocalTime.parse(handle.get<String>(KEY_START_TIME)),
            endDate = handle.get<String>(KEY_END_DATE)?.let(LocalDate::parse),
            endTime = LocalTime.parse(handle.get<String>(KEY_END_TIME)),
        )
    }

    /** Saved state crosses Binder, so very long text is left out instead of risking a crash. */
    fun persist(fields: ScheduleFields) {
        if (fields.title.length + fields.description.length > MAX_PERSISTED_CHARS) {
            clear()
            return
        }
        handle[KEY_TITLE] = fields.title
        handle[KEY_DESCRIPTION] = fields.description
        handle[KEY_ALL_DAY] = fields.isAllDay
        handle[KEY_START_DATE] = fields.startDate?.toString()
        handle[KEY_START_TIME] = fields.startTime.toString()
        handle[KEY_END_DATE] = fields.endDate?.toString()
        handle[KEY_END_TIME] = fields.endTime.toString()
    }

    fun clear() {
        ALL_KEYS.forEach { handle.remove<Any>(it) }
    }

    private companion object {
        const val KEY_TITLE = "schedule_title"
        const val KEY_DESCRIPTION = "schedule_description"
        const val KEY_ALL_DAY = "schedule_all_day"
        const val KEY_START_DATE = "schedule_start_date"
        const val KEY_START_TIME = "schedule_start_time"
        const val KEY_END_DATE = "schedule_end_date"
        const val KEY_END_TIME = "schedule_end_time"
        val ALL_KEYS = listOf(
            KEY_TITLE, KEY_DESCRIPTION, KEY_ALL_DAY, KEY_START_DATE, KEY_START_TIME, KEY_END_DATE, KEY_END_TIME,
        )
        const val MAX_PERSISTED_CHARS = 50_000
    }
}
