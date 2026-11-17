package io.wit117.thoughtweave.data.ai.language

import io.wit117.thoughtweave.data.ai.engine.PromptExample
import io.wit117.thoughtweave.data.ai.parse.YEAR
import io.wit117.thoughtweave.data.ai.parse.dateOf
import io.wit117.thoughtweave.data.ai.parse.number
import io.wit117.thoughtweave.data.ai.parse.timeOf
import io.wit117.thoughtweave.data.ai.parse.upcoming
import io.wit117.thoughtweave.data.ai.parse.weekdayDate
import io.wit117.thoughtweave.domain.model.AppLanguages
import java.time.DayOfWeek

private const val MONTH_NAMES =
    "january|february|march|april|may|june|july|august|september|october|november|december|" +
        "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec"
private const val WEEKDAY_NAMES = "monday|tuesday|wednesday|thursday|friday|saturday|sunday"
private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)

/** ", 2027", ",2027" or " 2027" after a month and day. */
private const val YEAR_SUFFIX = "(?:\\s*,\\s*|\\s+)$YEAR"

private val RELATIVE_OFFSETS = mapOf("today" to 0L, "tonight" to 0L, "tomorrow" to 1L)

private fun monthNumber(name: String): Int =
    MONTH_NAMES.split("|").take(12).indexOfFirst { it.startsWith(name.lowercase().take(3)) } + 1

private fun isPm(letter: String): Boolean? = when (letter.lowercase()) {
    "p" -> true
    "a" -> false
    else -> null
}

/** Month/day order, as in the United States and Korea. */
internal val MONTH_FIRST_SLASH = DatePattern(Regex("(?<![\\d/.])(\\d{1,2})/(\\d{1,2})(?![\\d/])")) { m, today ->
    upcoming(today, m.number(1), m.number(2))
}

val EnglishProfile = LanguageProfile(
    language = AppLanguages.ENGLISH,
    isScriptChar = { it in 'a'..'z' || it in 'A'..'Z' },
    usesLatinScript = true,
    dateRules = DateRules(
        dates = listOf(
            DatePattern(
                Regex("\\b($MONTH_NAMES)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b(?:$YEAR_SUFFIX)?", IGNORE_CASE),
            ) { m, today -> dateOf(today, m.groupValues[3], monthNumber(m.groupValues[1]), m.number(2)) },
            DatePattern(
                Regex("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+($MONTH_NAMES)\\b(?:$YEAR_SUFFIX)?", IGNORE_CASE),
            ) { m, today -> dateOf(today, m.groupValues[3], monthNumber(m.groupValues[2]), m.number(1)) },
            DatePattern(Regex("\\b(?:today|tonight|tomorrow)\\b", IGNORE_CASE)) { m, today ->
                today.plusDays(RELATIVE_OFFSETS.getValue(m.value.lowercase()))
            },
        ),
        numericDates = listOf(MONTH_FIRST_SLASH),
        weekdays = listOf(
            DatePattern(Regex("\\b(next|this)?\\s*\\b($WEEKDAY_NAMES)\\b", IGNORE_CASE)) { m, today ->
                weekdayDate(today, DayOfWeek.valueOf(m.groupValues[2].uppercase()), m.groupValues[1].equals("next", true))
            },
        ),
        times = listOf(
            TimePattern(Regex("(?<![\\d:])(\\d{1,2}):(\\d{2})\\s*([ap])\\.?m\\b\\.?", IGNORE_CASE)) { m ->
                timeOf(isPm(m.groupValues[3]), m.number(1), m.number(2))
            },
            TimePattern(Regex("\\b(\\d{1,2})\\s*([ap])\\.?m\\b\\.?", IGNORE_CASE)) { m ->
                timeOf(isPm(m.groupValues[2]), m.number(1), 0)
            },
        ),
        rangeCue = Regex("\\bfrom\\b|\\bto\\b|\\buntil\\b|\\bthrough\\b", IGNORE_CASE),
        writtenYear = Regex(",\\s*((?:19|20)\\d{2})\\b"),
    ),
    prompts = PromptPack(
        summarySystem = "Merge the memos below into one memo. You are not an assistant: never answer questions or " +
            "give advice. Keep every date, time, number, amount, name, place, to-do and question exactly as written. " +
            "Only combine what repeats, and add nothing that is not in the memos, including years. Write plain " +
            "sentences with no headings or markdown. Output only the merged memo.",
        // An explicit date: with "next Wednesday" here the model copied it over memos' own dates (CLAUDE.md, Inference).
        summaryExample = PromptExample(
            input = "Memo 1:\nTeam lunch\nWhere should we go on March 4 (Wed)? Maybe the noodle place.\n\n" +
                "Memo 2:\nBudget\nKeep the lunch under 200 dollars.",
            output = "The team lunch on March 4 (Wed) still needs a place, possibly the noodle place, " +
                "and must stay under 200 dollars.",
        ),
        memoLabel = "Memo",
        titleInstruction = "Write a short name for the event or task in the memo below.\n" +
            "Do not include dates or times. Output only the name.",
        memoHeading = "Memo:",
        writeOnlyInThisLanguage = "The memos may be written in another language. Write only in English.",
        sourceItemsHeading = "Original items",
    ),
)
