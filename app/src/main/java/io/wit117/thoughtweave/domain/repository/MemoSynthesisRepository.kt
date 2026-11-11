package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.Memo
import kotlinx.coroutines.flow.Flow

class MemoSynthesisException(
    message: String,
    val isInputTooLong: Boolean = false,
) : Exception(message)

interface MemoSynthesisRepository {
    /**
     * Summarizes [memos], emitting the text in chunks as it is produced.
     * The flow fails with [MemoSynthesisException] if no summary can be produced.
     */
    fun summarize(memos: List<Memo>, languages: AiLanguagePlan): Flow<String>

    /**
     * The AI-written part of [summary], without the list of original items that follows it. Checks of what
     * the AI wrote use this, because the list repeats every source fact.
     */
    fun aiPart(summary: String, output: AppLanguage): String = summary
}
