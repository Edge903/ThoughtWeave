package io.wit117.thoughtweave.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/** S01 – 메모 목록 (top-level, start destination). */
@Serializable
data object MemoListRoute

/** S03 – AI 메모 통합 (top-level). */
@Serializable
data object SynthesisRoute

/** S04 – 일정 확인 (top-level). */
@Serializable
data object ScheduleListRoute

/** S02-A (memoId == null, new memo) and S02-B (existing memo). */
@Serializable
data class MemoEditorRoute(val memoId: Long? = null)

/** S02-C – AI 합성 결과 편집. */
@Serializable
data class SynthesisResultRoute(val sourceIds: List<Long>)

/** S05 – 라이선스 및 고지. */
@Serializable
data object LicensesRoute

/** How the shared schedule editor is initialized. Kept so its serializer survives minification. */
@Keep
@Serializable
enum class ScheduleEditorMode {
    /** POP03 Mode A: empty editor, opened from the S04 FAB. */
    MANUAL,

    /** POP03 Mode B: prefilled from AI extraction of [ScheduleEditorRoute.memoId], opened from S02-B. */
    AI_EXTRACTED,

    /** POP02: edit an existing schedule [ScheduleEditorRoute.scheduleId], opened from an S04 item. */
    EXISTING,
}

/** POP02 / POP03 – shared schedule editor. */
@Serializable
data class ScheduleEditorRoute(
    val mode: ScheduleEditorMode,
    val memoId: Long? = null,
    val scheduleId: Long? = null,
)
