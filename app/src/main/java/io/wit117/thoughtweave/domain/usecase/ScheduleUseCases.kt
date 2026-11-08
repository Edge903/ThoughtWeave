package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.ScheduleDraft
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import java.time.Instant
import javax.inject.Inject

/** Schedule values as entered, before validation. */
data class ScheduleInput(
    val title: String,
    val description: String = "",
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    val isAllDay: Boolean = false,
    val memoId: Long? = null,
)

enum class ScheduleValidationError { TITLE_REQUIRED, DEADLINE_REQUIRED, START_AFTER_DEADLINE }

sealed interface ScheduleValidation {
    data class Valid(val draft: ScheduleDraft) : ScheduleValidation
    data class Invalid(val errors: Set<ScheduleValidationError>) : ScheduleValidation
}

class ValidateScheduleDraftUseCase @Inject constructor() {
    operator fun invoke(input: ScheduleInput): ScheduleValidation {
        val errors = buildSet {
            if (input.title.isBlank()) add(ScheduleValidationError.TITLE_REQUIRED)
            if (input.endAt == null) add(ScheduleValidationError.DEADLINE_REQUIRED)
            if (input.startAt != null && input.endAt != null && input.startAt > input.endAt) {
                add(ScheduleValidationError.START_AFTER_DEADLINE)
            }
        }
        val endAt = input.endAt
        if (errors.isNotEmpty() || endAt == null) return ScheduleValidation.Invalid(errors)
        return ScheduleValidation.Valid(
            ScheduleDraft(
                title = input.title,
                description = input.description,
                startAt = input.startAt,
                endAt = endAt,
                isAllDay = input.isAllDay,
                memoId = input.memoId,
            ),
        )
    }
}

sealed interface SaveScheduleResult {
    data class Saved(val scheduleId: Long) : SaveScheduleResult
    data class Invalid(val errors: Set<ScheduleValidationError>) : SaveScheduleResult
    data object NotFound : SaveScheduleResult
}

/** The single write path for schedules, so nothing unvalidated is stored. */
class SaveScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
    private val validate: ValidateScheduleDraftUseCase,
) {
    /** Creates a schedule when [scheduleId] is null, otherwise updates it. */
    suspend operator fun invoke(scheduleId: Long?, input: ScheduleInput): SaveScheduleResult =
        when (val validation = validate(input)) {
            is ScheduleValidation.Invalid -> SaveScheduleResult.Invalid(validation.errors)
            is ScheduleValidation.Valid -> when {
                scheduleId == null -> SaveScheduleResult.Saved(repository.createSchedule(validation.draft))
                repository.updateSchedule(scheduleId, validation.draft) -> SaveScheduleResult.Saved(scheduleId)
                else -> SaveScheduleResult.NotFound
            }
        }
}
