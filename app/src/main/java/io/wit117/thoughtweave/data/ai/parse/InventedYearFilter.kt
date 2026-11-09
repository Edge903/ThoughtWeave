package io.wit117.thoughtweave.data.ai.parse

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** "2024년 " in Korean text, or ", 2024" after a date in English text. */
private val YEAR = Regex("((?:19|20)\\d{2})년\\s*|,\\s*((?:19|20)\\d{2})\\b")
private val YEAR_DIGITS = Regex("(?:19|20)\\d{2}")

/** Enough text to hold back so a year split across chunks is still seen whole. */
private const val HOLD_BACK_CHARS = 8

private fun removeYears(text: String, allowed: Set<String>): String = YEAR.replace(text) { match ->
    val year = match.groupValues[1].ifEmpty { match.groupValues[2] }
    if (year in allowed) match.value else ""
}

/**
 * Gemma 3 1B adds a year to dates written without one, and prompting does not stop it. Removes any
 * year that the source memos do not contain, while the summary is still streaming.
 */
fun Flow<String>.withoutInventedYears(source: String): Flow<String> {
    val allowed = YEAR_DIGITS.findAll(source).map { it.value }.toSet()
    return flow {
        var pending = ""
        collect { chunk ->
            pending = removeYears(pending + chunk, allowed)
            if (pending.length > HOLD_BACK_CHARS) {
                emit(pending.dropLast(HOLD_BACK_CHARS))
                pending = pending.takeLast(HOLD_BACK_CHARS)
            }
        }
        if (pending.isNotEmpty()) emit(removeYears(pending, allowed))
    }
}
