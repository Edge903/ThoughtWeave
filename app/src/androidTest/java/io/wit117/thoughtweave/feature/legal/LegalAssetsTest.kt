package io.wit117.thoughtweave.feature.legal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegalAssetsTest {
    @Test
    fun everyDocumentAssetExistsAndHasText() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val agreements = listOf("legal/agreement_en.txt", "legal/agreement_ko.txt")
        (LegalDocument.entries.flatMap { it.assets } + agreements).distinct().forEach { path ->
            val text = assets.open(path).bufferedReader().use { it.readText() }
            assertTrue(path, parseLegalParagraphs(text).isNotEmpty())
        }
    }
}
