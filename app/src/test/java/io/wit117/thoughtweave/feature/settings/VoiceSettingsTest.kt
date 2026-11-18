package io.wit117.thoughtweave.feature.settings

import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.feature.dictation.FakeSpeechInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class VoiceRowLabelTest {
    private fun row(state: SpeechLanguageState?, download: SpeechDownloadEvent? = null) =
        VoiceLanguageRow(AppLanguages.KOREAN, state, download)

    @Test
    fun statesWithoutADownload() {
        assertEquals(VoiceRowLabel.Checking, row(null).label())
        assertEquals(VoiceRowLabel.Installed, row(SpeechLanguageState.INSTALLED).label())
        assertEquals(VoiceRowLabel.Downloading(null), row(SpeechLanguageState.DOWNLOADING).label())
        assertEquals(VoiceRowLabel.NotInstalled, row(SpeechLanguageState.DOWNLOADABLE).label())
        assertEquals(VoiceRowLabel.Unsupported, row(SpeechLanguageState.UNSUPPORTED).label())
    }

    @Test
    fun aDownloadUnderWayIsShownUntilTheLanguageIsInstalled() {
        val downloadable = SpeechLanguageState.DOWNLOADABLE
        assertEquals(VoiceRowLabel.Downloading(40), row(downloadable, SpeechDownloadEvent.Progress(40)).label())
        assertEquals(VoiceRowLabel.Requested, row(downloadable, SpeechDownloadEvent.Requested).label())
        assertEquals(VoiceRowLabel.Scheduled, row(downloadable, SpeechDownloadEvent.Scheduled).label())
        assertEquals(VoiceRowLabel.Failed, row(downloadable, SpeechDownloadEvent.Failed).label())
        assertEquals(VoiceRowLabel.Installed, row(SpeechLanguageState.INSTALLED, SpeechDownloadEvent.Requested).label())
    }

    @Test
    fun downloadIsOfferedOnlyWhenDownloadableAndIdleOrFailed() {
        assertTrue(row(SpeechLanguageState.DOWNLOADABLE).canDownload)
        assertTrue(row(SpeechLanguageState.DOWNLOADABLE, SpeechDownloadEvent.Failed).canDownload)
        assertFalse(row(SpeechLanguageState.DOWNLOADABLE, SpeechDownloadEvent.Requested).canDownload)
        assertFalse(row(SpeechLanguageState.DOWNLOADING).canDownload)
        assertFalse(row(SpeechLanguageState.UNSUPPORTED).canDownload)
        assertFalse(row(null).canDownload)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun states(ko: SpeechLanguageState, en: SpeechLanguageState = SpeechLanguageState.INSTALLED) =
        mapOf(AppLanguages.KOREAN.speechTag to ko, AppLanguages.ENGLISH.speechTag to en)

    @Test
    fun unsupportedAndAndroid12ShowNoLanguageRows() {
        val unsupported = VoiceSettingsViewModel(FakeSpeechInput(supported = false))
        assertEquals(VoiceSupport.UNSUPPORTED, unsupported.uiState.value.support)
        val basic = VoiceSettingsViewModel(FakeSpeechInput(manageable = false))
        assertEquals(VoiceSupport.BASIC, basic.uiState.value.support)
        basic.refresh()
        assertTrue(basic.uiState.value.rows.isEmpty())
    }

    @Test
    fun refreshReadsEveryLanguage() {
        val speech = FakeSpeechInput(states = states(SpeechLanguageState.DOWNLOADABLE))
        val viewModel = VoiceSettingsViewModel(speech)
        viewModel.refresh()
        val rows = viewModel.uiState.value.rows.associate { it.language to it.state }
        assertEquals(SpeechLanguageState.DOWNLOADABLE, rows[AppLanguages.KOREAN])
        assertEquals(SpeechLanguageState.INSTALLED, rows[AppLanguages.ENGLISH])
        assertFalse(viewModel.uiState.value.checkFailed)
    }

    @Test
    fun aFailedCheckIsReported() {
        val viewModel = VoiceSettingsViewModel(FakeSpeechInput(states = null))
        viewModel.refresh()
        assertTrue(viewModel.uiState.value.checkFailed)
    }

    @Test
    fun downloadShowsProgressThenRereadsTheStateWhenDone() = runTest(dispatcher) {
        val speech = FakeSpeechInput(states = states(SpeechLanguageState.DOWNLOADABLE))
        val viewModel = VoiceSettingsViewModel(speech)
        viewModel.refresh()
        viewModel.download(AppLanguages.KOREAN)
        val channel = speech.downloads.getValue("ko-KR")
        fun korean() = viewModel.uiState.value.rows.first { it.language == AppLanguages.KOREAN }

        channel.send(SpeechDownloadEvent.Progress(30))
        assertEquals(VoiceRowLabel.Downloading(30), korean().label())

        speech.states = states(SpeechLanguageState.INSTALLED)
        val checksBefore = speech.stateChecks
        channel.send(SpeechDownloadEvent.Done)
        channel.close()
        assertEquals(checksBefore + 1, speech.stateChecks)
        assertEquals(VoiceRowLabel.Installed, korean().label())
    }

    @Test
    fun aSecondTapWhileDownloadingDoesNotStartAnother() {
        val speech = FakeSpeechInput(states = states(SpeechLanguageState.DOWNLOADABLE))
        val viewModel = VoiceSettingsViewModel(speech)
        viewModel.download(AppLanguages.KOREAN)
        val first = speech.downloads.getValue("ko-KR")
        viewModel.download(AppLanguages.KOREAN)
        assertEquals(first, speech.downloads.getValue("ko-KR"))
    }
}
