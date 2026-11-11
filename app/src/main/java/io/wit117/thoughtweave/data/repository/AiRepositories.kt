package io.wit117.thoughtweave.data.repository

import io.wit117.thoughtweave.data.ai.engine.InferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferenceException
import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.language.EnglishProfile
import io.wit117.thoughtweave.data.ai.language.LanguageProfile
import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.data.ai.parse.cleanTitle
import io.wit117.thoughtweave.data.ai.parse.factList
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import io.wit117.thoughtweave.data.ai.parse.withoutInventedYears
import io.wit117.thoughtweave.data.ai.prompt.TITLE_TEMPERATURE
import io.wit117.thoughtweave.data.ai.prompt.scheduleTitlePrompt
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguage
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold

class MemoSynthesisRepositoryImpl @Inject constructor(
    private val engine: InferenceEngine,
) : MemoSynthesisRepository {
    /**
     * The AI's merged text, then every item from the memos in their original wording (user decision,
     * 2026-11-11): the model drops or rewords some facts, so the list keeps all of them.
     */
    override fun summarize(memos: List<Memo>, languages: AiLanguagePlan): Flow<String> {
        val input = LanguageProfiles.forLanguage(languages.input)
        val output = LanguageProfiles.forLanguage(languages.output)
        val aiText = engine.generate(summaryPrompt(memos, input, output)).withoutInventedYears(
            memos.joinToString("\n") { it.title + "\n" + it.content },
        )
        val heading = itemsHeading(output)
        val items = factList(memos)
        return flow {
            var text = ""
            aiText.collect { chunk ->
                text += chunk
                emit(chunk)
            }
            if (items.isNotEmpty()) {
                val separator = if (text.endsWith("\n")) "\n" else "\n\n"
                emit(separator + heading + "\n" + items.joinToString("\n") { "- $it" })
            }
        }.catch { e ->
            throw if (e is InferenceException) {
                MemoSynthesisException("The summary failed", isInputTooLong = e.isInputTooLong)
            } else {
                e
            }
        }
    }

    /** Everything before the last heading line; the whole text if the user removed the heading. */
    override fun aiPart(summary: String, output: AppLanguage): String {
        val heading = itemsHeading(LanguageProfiles.forLanguage(output))
        val lines = summary.lines()
        val at = lines.indexOfLast { it.trim() == heading }
        return if (at < 0) summary else lines.take(at).joinToString("\n").trimEnd()
    }

    private fun itemsHeading(output: LanguageProfile): String =
        (output.prompts ?: EnglishProfile.prompts)?.sourceItemsHeading.orEmpty()
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
