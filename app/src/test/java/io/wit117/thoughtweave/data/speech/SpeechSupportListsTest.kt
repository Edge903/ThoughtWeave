package io.wit117.thoughtweave.data.speech

import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechSupportListsTest {
    private val lists = SpeechSupportLists(
        installed = listOf("en-US"),
        pending = listOf("ja_JP"),
        supported = listOf("en-US", "ko-KR", "ja-JP", "en-GB"),
    )

    @Test
    fun installedWinsOverSupported() = assertEquals(SpeechLanguageState.INSTALLED, lists.stateOf("en-US"))

    @Test
    fun supportedButNotInstalledCanBeDownloaded() =
        assertEquals(SpeechLanguageState.DOWNLOADABLE, lists.stateOf("ko-KR"))

    @Test
    fun pendingIsDownloadingAndUnderscoresMatch() =
        assertEquals(SpeechLanguageState.DOWNLOADING, lists.stateOf("ja-JP"))

    @Test
    fun caseIsIgnored() = assertEquals(SpeechLanguageState.INSTALLED, lists.stateOf("EN-us"))

    @Test
    fun anotherRegionDoesNotCount() {
        val onlyBritish = SpeechSupportLists(listOf("en-GB"), emptyList(), listOf("en-GB"))
        assertEquals(SpeechLanguageState.UNSUPPORTED, onlyBritish.stateOf("en-US"))
    }

    @Test
    fun unlistedIsUnsupported() = assertEquals(SpeechLanguageState.UNSUPPORTED, lists.stateOf("fr-FR"))
}
