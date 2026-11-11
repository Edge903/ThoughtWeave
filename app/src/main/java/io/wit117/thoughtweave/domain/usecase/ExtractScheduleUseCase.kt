package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.ExtractedSchedule
import io.wit117.thoughtweave.domain.repository.MemoRepository
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionException
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Reads the saved memo and extracts from it, relative to today in the device's time zone. */
class ExtractScheduleUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val extractionRepository: ScheduleExtractionRepository,
    private val resolveLanguage: ResolveAiLanguageUseCase,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend operator fun invoke(memoId: Long): ExtractedSchedule {
        val memo = memoRepository.getMemo(memoId)
            ?: throw ScheduleExtractionException("Memo $memoId no longer exists")
        return extractionRepository.extract(
            title = memo.title,
            content = memo.content,
            today = LocalDate.now(clock.withZone(zone)),
            languages = resolveLanguage(memo.title + "\n" + memo.content),
        )
    }
}
