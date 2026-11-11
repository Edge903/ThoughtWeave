package io.wit117.thoughtweave.data.ai.parse

import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.usecase.TextDateReader
import java.time.LocalDate
import javax.inject.Inject

class TextDateReaderImpl @Inject constructor() : TextDateReader {
    override fun datesIn(text: String, language: AppLanguage, today: LocalDate): Set<LocalDate> =
        readAllDates(text, today, LanguageProfiles.forLanguage(language))
}
