package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.repository.MemoSynthesisException
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Release builds have no inference until Phase 5c, so S03 reports AI as unavailable. */
class UnavailableMemoSynthesisRepository @Inject constructor() : MemoSynthesisRepository {
    override val isAvailable: Boolean = false

    override fun summarize(memos: List<Memo>): Flow<String> = flow {
        throw MemoSynthesisException("On-device AI is not available in this build")
    }
}
