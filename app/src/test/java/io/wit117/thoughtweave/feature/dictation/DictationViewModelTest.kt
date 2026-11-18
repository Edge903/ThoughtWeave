package io.wit117.thoughtweave.feature.dictation

import io.wit117.thoughtweave.FakeLanguageSettings
import io.wit117.thoughtweave.FakeUiLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.SpeechError
import io.wit117.thoughtweave.domain.model.SpeechEvent
import io.wit117.thoughtweave.testLanguageResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        speech: FakeSpeechInput,
        settings: LanguageSettings = LanguageSettings(),
        ui: FakeUiLanguage = FakeUiLanguage(AppLanguages.ENGLISH),
    ) = DictationViewModel(speech, testLanguageResolver(FakeLanguageSettings(settings), ui))

    @Test
    fun unsupportedDeviceNeverStartsListening() {
        val speech = FakeSpeechInput(supported = false)
        val viewModel = viewModel(speech)
        assertFalse(viewModel.uiState.value.isSupported)

        viewModel.start()
        assertFalse(viewModel.uiState.value.isListening)
        assertTrue(speech.tags.isEmpty())
    }

    @Test
    fun partialTextIsShownThenTheFinalTextIsInserted() = runTest(dispatcher) {
        val speech = FakeSpeechInput()
        val viewModel = viewModel(speech)
        viewModel.start()
        val session = speech.last!!
        assertTrue(viewModel.uiState.value.isListening)

        session.channel.send(SpeechEvent.Partial("buy"))
        assertEquals("buy", viewModel.uiState.value.partialText)

        session.channel.send(SpeechEvent.Recognized("buy milk"))
        session.channel.close()
        assertEquals(DictationEffect.Insert("buy milk"), viewModel.effects.first())
        assertFalse(viewModel.uiState.value.isListening)
        assertEquals("", viewModel.uiState.value.partialText)
    }

    @Test
    fun failureReportsTheErrorAndLanguage() = runTest(dispatcher) {
        val speech = FakeSpeechInput()
        val viewModel = viewModel(speech, ui = FakeUiLanguage(AppLanguages.KOREAN))
        viewModel.start()
        speech.last!!.channel.apply {
            send(SpeechEvent.Failed(SpeechError.LANGUAGE_UNAVAILABLE))
            close()
        }
        assertEquals(
            DictationEffect.Failed(SpeechError.LANGUAGE_UNAVAILABLE, AppLanguages.KOREAN, canDownload = true),
            viewModel.effects.first(),
        )
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun everyPieceOfALongSessionIsInsertedAndThePartialTextResets() = runTest(dispatcher) {
        val speech = FakeSpeechInput()
        val viewModel = viewModel(speech)
        viewModel.start()
        speech.last!!.channel.apply {
            send(SpeechEvent.Partial("first"))
            send(SpeechEvent.Recognized("first part"))
        }
        assertEquals("", viewModel.uiState.value.partialText)
        assertTrue(viewModel.uiState.value.isListening)
        speech.last!!.channel.apply {
            send(SpeechEvent.Recognized("second part"))
            close()
        }
        assertEquals(
            listOf(DictationEffect.Insert("first part"), DictationEffect.Insert("second part")),
            viewModel.effects.take(2).toList(),
        )
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun missingLanguageSaysWhetherItCanBeDownloaded() = runTest(dispatcher) {
        val speech = FakeSpeechInput(manageable = false)
        val viewModel = viewModel(speech)
        viewModel.start()
        speech.last!!.channel.apply {
            send(SpeechEvent.Failed(SpeechError.LANGUAGE_UNAVAILABLE))
            close()
        }
        assertFalse((viewModel.effects.first() as DictationEffect.Failed).canDownload)
    }

    @Test
    fun stopAsksTheSessionForItsResult() {
        val speech = FakeSpeechInput()
        val viewModel = viewModel(speech)
        viewModel.start()
        viewModel.stop()
        assertTrue(speech.last!!.stopped)
    }

    @Test
    fun secondStartWhileListeningIsIgnored() {
        val speech = FakeSpeechInput()
        val viewModel = viewModel(speech)
        viewModel.start()
        viewModel.start()
        assertEquals(1, speech.tags.size)
    }

    @Test
    fun listensInTheAppLanguageByDefault() {
        val speech = FakeSpeechInput()
        viewModel(speech, ui = FakeUiLanguage(AppLanguages.KOREAN)).start()
        assertEquals(listOf("ko-KR"), speech.tags)
    }

    @Test
    fun aSetInputLanguageWinsOverTheAppLanguage() {
        val speech = FakeSpeechInput()
        viewModel(
            speech,
            settings = LanguageSettings(input = InputLanguageSetting.Specific(AppLanguages.ENGLISH)),
            ui = FakeUiLanguage(AppLanguages.KOREAN),
        ).start()
        assertEquals(listOf("en-US"), speech.tags)
    }
}
