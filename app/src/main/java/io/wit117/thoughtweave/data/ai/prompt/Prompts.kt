package io.wit117.thoughtweave.data.ai.prompt

import io.wit117.thoughtweave.domain.model.Memo

const val SUMMARY_TEMPERATURE = 0.3
const val TITLE_TEMPERATURE = 0.2

/** A title needs only the start of the memo; dates are read from the full text separately. */
private const val MAX_TITLE_SOURCE_CHARS = 2_000

private fun containsHangul(text: String) = text.any { it in '가'..'힣' }

/** Gemma 3 1B ignores "same language" instructions, so Korean memos get a Korean prompt. */
fun summaryPrompt(memos: List<Memo>): String = buildString {
    val isKorean = memos.any { containsHangul(it.title + it.content) }
    if (isKorean) {
        appendLine("아래 메모들을 하나의 간결한 메모로 요약하세요.")
        appendLine("중요한 사실, 이름, 숫자, 날짜는 모두 유지하고 메모에 없는 내용은 추가하지 마세요.")
        appendLine("반드시 한국어로 작성하고, 제목이나 머리말 없이 요약문만 출력하세요.")
    } else {
        appendLine("Summarize the memos below into one concise memo.")
        appendLine("Keep every important fact, name, number and date.")
        appendLine("Do not add anything that is not in the memos.")
        appendLine("Output only the summary, with no heading.")
    }
    memos.forEachIndexed { index, memo ->
        appendLine()
        appendLine(if (isKorean) "메모 ${index + 1}:" else "Memo ${index + 1}:")
        if (memo.title.isNotBlank()) appendLine(memo.title)
        appendLine(memo.content)
    }
}

fun scheduleTitlePrompt(title: String, content: String): String = buildString {
    val source = "$title\n$content".trim().take(MAX_TITLE_SOURCE_CHARS)
    if (containsHangul(source)) {
        appendLine("아래 메모에 있는 일정의 짧은 이름을 한국어로 지으세요.")
        appendLine("날짜와 시간은 넣지 말고 이름만 출력하세요.")
        appendLine()
        appendLine("메모:")
    } else {
        appendLine("Write a short name for the event or task in the memo below.")
        appendLine("Do not include dates or times. Output only the name.")
        appendLine()
        appendLine("Memo:")
    }
    appendLine(source)
}
