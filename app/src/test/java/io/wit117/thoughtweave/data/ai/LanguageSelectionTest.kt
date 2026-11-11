package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.FakeLanguageSettings
import io.wit117.thoughtweave.FakeUiLanguage
import io.wit117.thoughtweave.data.ai.language.AiLanguageCatalogImpl
import io.wit117.thoughtweave.data.ai.language.EnglishProfile
import io.wit117.thoughtweave.data.ai.language.KoreanProfile
import io.wit117.thoughtweave.data.ai.language.LanguageProfiles
import io.wit117.thoughtweave.data.ai.parse.readScheduleDates
import io.wit117.thoughtweave.data.ai.prompt.scheduleTitlePrompt
import io.wit117.thoughtweave.data.ai.prompt.summaryPrompt
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting
import io.wit117.thoughtweave.testLanguageResolver
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSelectionTest {
    private val catalog = AiLanguageCatalogImpl()
    private val en = AppLanguages.ENGLISH
    private val ko = AppLanguages.KOREAN

    private fun memo(content: String) = Memo(1, "", content, Instant.EPOCH, Instant.EPOCH, false, MemoOrigin.MANUAL)

    private fun resolve(
        text: String,
        input: InputLanguageSetting = InputLanguageSetting.Auto,
        output: OutputLanguageSetting = OutputLanguageSetting.SameAsInput,
        app: io.wit117.thoughtweave.domain.model.AppLanguage? = null,
    ) = testLanguageResolver(FakeLanguageSettings(LanguageSettings(input, output)), FakeUiLanguage(app))(text)

    @Test
    fun catalogOffersRegisteredLanguages() {
        assertEquals(listOf(en, ko), catalog.inputLanguages)
        assertEquals(listOf(en, ko), catalog.outputLanguages)
        // No translation pair has passed the real-model test, so the output setting is hidden.
        assertTrue(catalog.translationTargets.isEmpty())
    }

    @Test
    fun detectsKoreanEvenWithSomeEnglishWords() {
        assertEquals(ko, catalog.detect("Dec 3 디자인 리뷰 회의", null))
        assertEquals(en, catalog.detect("Design review on Dec 3", null))
        assertNull(catalog.detect("12/3 15:00", null))
    }

    @Test
    fun autoUsesTheMemoLanguageAndSameOutput() {
        assertEquals(AiLanguagePlan(ko, ko), resolve("내일 회의"))
        assertEquals(AiLanguagePlan(en, en), resolve("Meeting tomorrow", app = ko))
    }

    @Test
    fun memoWithoutLettersFallsBackToTheAppLanguage() {
        assertEquals(AiLanguagePlan(ko, ko), resolve("12/3", app = ko))
        assertEquals(AiLanguagePlan(en, en), resolve("12/3", app = en))
    }

    @Test
    fun aChosenInputLanguageOverridesDetection() {
        assertEquals(ko, resolve("Meeting tomorrow", input = InputLanguageSetting.Specific(ko)).input)
    }

    @Test
    fun unverifiedTranslationKeepsTheInputLanguageAndSaysSo() {
        // No pair has passed the real-model test yet (LanguageProfiles.verifiedTranslations).
        assertTrue(LanguageProfiles.verifiedTranslations.isEmpty())
        assertEquals(
            AiLanguagePlan(ko, ko, unavailableOutput = en),
            resolve("내일 회의", output = OutputLanguageSetting.Specific(en)),
        )
    }

    @Test
    fun requestingTheInputLanguageIsNotATranslation() {
        assertEquals(AiLanguagePlan(ko, ko), resolve("내일 회의", output = OutputLanguageSetting.Specific(ko)))
    }

    @Test
    fun translationPromptPairsInputMemoWithOutputReply() {
        val prompt = summaryPrompt(listOf(memo("내일 회의")), KoreanProfile, EnglishProfile)

        assertTrue(prompt.text.startsWith("메모 1:"))
        assertTrue("Write only in English" in prompt.systemInstruction!!)
        val example = prompt.examples.single()
        assertEquals(KoreanProfile.prompts!!.summaryExample.input, example.input)
        assertEquals(EnglishProfile.prompts!!.summaryExample.output, example.output)
    }

    @Test
    fun sameLanguagePromptHasNoTranslationNote() {
        val prompt = summaryPrompt(listOf(memo("Meeting")), EnglishProfile, EnglishProfile)
        assertFalse("Write only in English" in prompt.systemInstruction!!)
        assertFalse("반드시 한국어로만" in scheduleTitlePrompt("", "회의", KoreanProfile, KoreanProfile))
        assertTrue("반드시 한국어로만" in scheduleTitlePrompt("", "Meeting", EnglishProfile, KoreanProfile))
    }

    @Test
    fun writtenOutDatesOfEveryLanguageAreReadWhateverTheInput() {
        val today = LocalDate.of(2026, 11, 9)
        assertEquals(LocalDate.of(2026, 12, 3), readScheduleDates("Dec 3 회의", today, KoreanProfile)?.deadlineDate)
        assertEquals(LocalDate.of(2026, 12, 3), readScheduleDates("12월 3일 meeting", today, EnglishProfile)?.deadlineDate)
    }

    @Test
    fun everyExampleHasTheSameStructureForTranslationPairing() {
        val examples = LanguageProfiles.all.mapNotNull { it.prompts?.summaryExample }
        assertTrue(examples.all { it.input.lines().count { line -> line.endsWith(":") } == 2 })
    }
}
