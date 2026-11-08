package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Debug-only stand-in until LiteRT-LM arrives in Phase 5c. It never ships in release builds. */
class FakeMemoSynthesisRepository @Inject constructor() : MemoSynthesisRepository {
    override val isAvailable: Boolean = true

    override fun summarize(memos: List<Memo>): Flow<String> = flow {
        emit("[DEBUG FAKE: not AI-generated]\n")
        memos.forEach { memo ->
            delay(CHUNK_DELAY_MILLIS)
            emit("- ${memo.title.ifEmpty { "(untitled)" }}: ${memo.content.lineSequence().first()}\n")
        }
    }

    private companion object {
        const val CHUNK_DELAY_MILLIS = 100L
    }
}
