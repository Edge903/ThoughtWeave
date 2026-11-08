package io.wit117.thoughtweave.domain.model

import java.time.Instant

/** Stored by name in `memos.origin`: renaming a constant requires a migration. */
enum class MemoOrigin { MANUAL, SYNTHESIZED }

data class Memo(
    val id: Long,
    /** Empty when the user gave no title; the UI displays it as "무제" / "Untitled". */
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPinned: Boolean,
    val origin: MemoOrigin,
)
