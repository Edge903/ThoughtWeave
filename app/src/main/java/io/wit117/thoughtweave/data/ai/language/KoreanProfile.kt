package io.wit117.thoughtweave.data.ai.language

import io.wit117.thoughtweave.data.ai.engine.PromptExample
import io.wit117.thoughtweave.data.ai.parse.YEAR
import io.wit117.thoughtweave.data.ai.parse.dateOf
import io.wit117.thoughtweave.data.ai.parse.number
import io.wit117.thoughtweave.data.ai.parse.timeOf
import io.wit117.thoughtweave.data.ai.parse.weekdayDate
import io.wit117.thoughtweave.domain.model.AppLanguages
import java.time.DayOfWeek

private const val WEEKDAYS = "월화수목금토일"
private val RELATIVE_OFFSETS = mapOf("오늘" to 0L, "내일" to 1L, "모레" to 2L, "글피" to 3L)

private fun isPm(period: String): Boolean? = when (period) {
    "오후" -> true
    "오전" -> false
    else -> null
}

val KoreanProfile = LanguageProfile(
    language = AppLanguages.KOREAN,
    isScriptChar = { it in '가'..'힣' || it in 'ᄀ'..'ᇿ' || it in '㄰'..'㆏' },
    usesLatinScript = false,
    dateRules = DateRules(
        dates = listOf(
            DatePattern(Regex("(?:$YEAR\\s*년\\s*,?\\s*)?(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")) { m, today ->
                dateOf(today, m.groupValues[1], m.number(2), m.number(3))
            },
            DatePattern(Regex("오늘|내일|모레|글피")) { m, today ->
                today.plusDays(RELATIVE_OFFSETS.getValue(m.value))
            },
        ),
        numericDates = listOf(MONTH_FIRST_SLASH),
        weekdays = listOf(
            DatePattern(Regex("(다음\\s*주|이번\\s*주)?\\s*([월화수목금토일])요일")) { m, today ->
                val day = DayOfWeek.of(WEEKDAYS.indexOf(m.groupValues[2]) + 1)
                weekdayDate(today, day, m.groupValues[1].startsWith("다음"))
            },
        ),
        times = listOf(
            TimePattern(Regex("(오전|오후)\\s*(?<![\\d:])(\\d{1,2}):(\\d{2})")) { m ->
                timeOf(isPm(m.groupValues[1]), m.number(2), m.number(3))
            },
            TimePattern(Regex("(오전|오후)?\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분|\\s*(반))?")) { m ->
                val minute = if (m.groupValues[4].isNotEmpty()) 30 else m.groupValues[3].toIntOrNull() ?: 0
                timeOf(isPm(m.groupValues[1]), m.number(2), minute)
            },
        ),
        rangeCue = Regex("부터"),
        writtenYear = Regex("((?:19|20)\\d{2})년\\s*"),
    ),
    prompts = PromptPack(
        summarySystem = "당신은 메모를 압축하는 도구입니다. 비서가 아닙니다. 메모 속 질문에 답하거나 조언하거나 작성자에게 말을 " +
            "걸지 마세요. 메모 속 질문은 아직 정해지지 않은 내용으로 그대로 남기세요. 메모에 있는 내용만 사용하고 연도나 사실을 " +
            "추가하지 마세요. 제목, 목록, 마크다운 없이 평범한 한국어 문장으로 압축한 내용만 출력하세요.",
        summaryExample = PromptExample(
            input = "메모 1:\n팀 점심\n다음 주 수요일에 어디로 갈까? 국수집도 괜찮을 듯.\n\n" +
                "메모 2:\n예산\n점심 비용은 20만원 이내로.",
            output = "다음 주 수요일 팀 점심 장소는 아직 정해지지 않았고 국수집이 후보이며, 비용은 20만원 이내로 한다.",
        ),
        memoLabel = "메모",
        titleInstruction = "아래 메모에 있는 일정의 짧은 이름을 한국어로 지으세요.\n날짜와 시간은 넣지 말고 이름만 출력하세요.",
        memoHeading = "메모:",
        writeOnlyInThisLanguage = "메모가 다른 언어로 쓰여 있어도 반드시 한국어로만 작성하세요.",
    ),
)
