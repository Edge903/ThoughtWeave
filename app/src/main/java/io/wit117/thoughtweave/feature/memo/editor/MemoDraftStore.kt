package io.wit117.thoughtweave.feature.memo.editor

import androidx.lifecycle.SavedStateHandle

/** Keeps an unsaved draft across process death. */
class MemoDraftStore(private val savedStateHandle: SavedStateHandle) {

    fun restore(): MemoDraft? {
        val title = savedStateHandle.get<String>(KEY_TITLE) ?: return null
        val content = savedStateHandle.get<String>(KEY_CONTENT) ?: return null
        return MemoDraft(title, content)
    }

    /** Saved state crosses Binder, so very long drafts are left out instead of risking a crash. */
    fun persist(draft: MemoDraft) {
        if (draft.title.length + draft.content.length > MAX_PERSISTED_DRAFT_CHARS) {
            clear()
        } else {
            savedStateHandle[KEY_TITLE] = draft.title
            savedStateHandle[KEY_CONTENT] = draft.content
        }
    }

    fun clear() {
        savedStateHandle.remove<String>(KEY_TITLE)
        savedStateHandle.remove<String>(KEY_CONTENT)
    }

    private companion object {
        const val KEY_TITLE = "draft_title"
        const val KEY_CONTENT = "draft_content"
        const val MAX_PERSISTED_DRAFT_CHARS = 50_000
    }
}
