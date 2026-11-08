package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.Memo
import kotlinx.coroutines.flow.Flow

class MemoSynthesisException(message: String) : Exception(message)

interface MemoSynthesisRepository {
    val isAvailable: Boolean

    /**
     * Summarizes [memos], emitting the text in chunks as it is produced.
     * The flow fails with [MemoSynthesisException] if no summary can be produced.
     */
    fun summarize(memos: List<Memo>): Flow<String>
}
