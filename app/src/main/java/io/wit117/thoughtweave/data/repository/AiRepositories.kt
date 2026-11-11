package io.wit117.thoughtweave.data.repository

import io.wit117.thoughtweave.data.ai.engine.InferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferenceException
import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.data.ai.parse.cleanTitle
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import io.wit117.thoughtweave.data.ai.parse.withoutInventedYears
import io.wit117.thoughtweave.data.ai.prompt.TITLE_TEMPERATURE
import io.wit117.thoughtweave.data.ai.prompt.scheduleTitlePrompt
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.ExtractedSchedule
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.MemoSynthesisException
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionException
import io.wit117.thoughtweave.domain.repository.ScheduleExtractionRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.fold

class MemoSynthesisRepositoryImpl @Inject constructor(
    private val engine: InferenceEngine,
) : MemoSynthesisRepository {
    override fun summarize(memos: List<Memo>, languages: AiLanguagePlan): Flow<String> {
        val prompt = summaryPrompt(
            memos,
            LanguageProfiles.forLanguage(languages.input),
            LanguageProfiles.forLanguage(languages.output),
        )
        return engine.generate(prompt).withoutInventedYears(
            memos.joinToString("\n") { it.title + "\n" + it.content },
        ).catch { e ->
            throw if (e is InferenceException) {
                MemoSynthesisException("The summary failed", isInputTooLong = e.isInputTooLong)
            } else {
                e
            }
        }
    }
}

/** Dates and times are read from the memo by code; the model only names the schedule. */
class ScheduleExtractionRepositoryImpl @Inject constructor(
    private val engine: InferenceEngine,
) : ScheduleExtractionRepository {
    override suspend fun extract(
        title: String,
        content: String,
        today: LocalDate,
        languages: AiLanguagePlan,
    ): ExtractedSchedule {
        val input = LanguageProfiles.forLanguage(languages.input)
        val output = LanguageProfiles.forLanguage(languages.output)
        val dates = readScheduleDates("$title\n$content", today, input)
            ?: throw ScheduleExtractionException("The memo gives no date")
        val reply = try {
            engine.generate(InferencePrompt(scheduleTitlePrompt(title, content, input, output), TITLE_TEMPERATURE))
                .fold("") { text, chunk -> text + chunk }
        } catch (_: InferenceException) {
            throw ScheduleExtractionException("The schedule could not be named")
        }
        return ExtractedSchedule(
            title = cleanTitle(reply) ?: throw ScheduleExtractionException("The reply had no name"),
            deadlineDate = dates.deadlineDate,
            deadlineTime = dates.deadlineTime,
            startDate = dates.startDate,
            startTime = dates.startTime,
        )
    }
}
