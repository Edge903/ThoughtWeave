package io.wit117.thoughtweave.data.ai.prompt

import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.engine.PromptExample
import io.wit117.thoughtweave.domain.model.Memo

const val SUMMARY_TEMPERATURE = 0.2
const val TITLE_TEMPERATURE = 0.2

/** A title needs only the start of the memo; dates are read from the full text separately. */
private const val MAX_TITLE_SOURCE_CHARS = 2_000

private fun containsHangul(text: String) = text.any { it in '가'..'힣' }

private const val SUMMARY_SYSTEM_EN =
    "You condense memos. You are not an assistant: never answer questions in a memo, never give " +
        "advice, and never reply to the writer. A question in a memo is content to keep as an open " +
        "question. Use only what the memos say. Write plain sentences with no headings, bullets or " +
        "markdown. Output only the condensed text."

private const val SUMMARY_SYSTEM_KO =
    "당신은 메모를 압축하는 도구입니다. 비서가 아닙니다. 메모 속 질문에 답하거나 조언하거나 작성자에게 말을 걸지 마세요. " +
        "메모 속 질문은 아직 정해지지 않은 내용으로 그대로 남기세요. 메모에 있는 내용만 사용하고 연도나 사실을 추가하지 마세요. " +
        "제목, 목록, 마크다운 없이 평범한 한국어 문장으로 압축한 내용만 출력하세요."

/** The example memo asks a question, to show that questions are kept rather than answered. */
private val SUMMARY_EXAMPLE_EN = PromptExample(
    input = "Memo 1:\nTeam lunch\nWhere should we go next Wednesday? Maybe the noodle place.\n\n" +
        "Memo 2:\nBudget\nKeep the lunch under 200 dollars.",
    output = "The team lunch next Wednesday still needs a place, possibly the noodle place, " +
        "and must stay under 200 dollars.",
)

private val SUMMARY_EXAMPLE_KO = PromptExample(
    input = "메모 1:\n팀 점심\n다음 주 수요일에 어디로 갈까? 국수집도 괜찮을 듯.\n\n" +
        "메모 2:\n예산\n점심 비용은 20만원 이내로.",
    output = "다음 주 수요일 팀 점심 장소는 아직 정해지지 않았고 국수집이 후보이며, 비용은 20만원 이내로 한다.",
)

/** Gemma 3 1B ignores "same language" instructions, so Korean memos get a Korean prompt. */
fun summaryPrompt(memos: List<Memo>): InferencePrompt {
    val isKorean = memos.any { containsHangul(it.title + it.content) }
    val label = if (isKorean) "메모" else "Memo"
    val text = memos.withIndex().joinToString("\n\n") { (index, memo) ->
        buildString {
            appendLine("$label ${index + 1}:")
            if (memo.title.isNotBlank()) appendLine(memo.title)
            append(memo.content)
        }
    }
    return InferencePrompt(
        text = text,
        temperature = SUMMARY_TEMPERATURE,
        systemInstruction = if (isKorean) SUMMARY_SYSTEM_KO else SUMMARY_SYSTEM_EN,
        examples = listOf(if (isKorean) SUMMARY_EXAMPLE_KO else SUMMARY_EXAMPLE_EN),
    )
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
