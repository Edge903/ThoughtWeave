package io.wit117.thoughtweave.feature.aimodel

import android.net.Uri
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.MainActivity
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.data.ai.delivery.ImportedModelDelivery
import io.wit117.thoughtweave.data.ai.delivery.ModelDeliveryState
import io.wit117.thoughtweave.data.ai.delivery.importedModelFile
import io.wit117.thoughtweave.data.ai.model.GEMMA_MODEL_SPEC
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** S03 without a model, and the debug import path, using a small stand-in for the real file. */
@RunWith(AndroidJUnit4::class)
class ModelDeliveryFlowTest {
    @get:Rule(order = 0)
    val modelRule = ModelStateRule(installed = false)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = setAgreementAccepted(true)

    private fun str(id: Int) = rule.activity.getString(id)

    @Test
    fun missingModelShowsBannerAndKeepsActionDisabled() {
        rule.onNode(hasText(str(R.string.nav_ai_merge)) and hasClickAction()).performClick()

        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasText(str(R.string.ai_model_not_downloaded)))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasText(str(R.string.debug_import_model)) and hasClickAction()).assertExists()
        rule.onNode(hasText(str(R.string.fab_ai_synthesize)) and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun importCopiesThePickedFile() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "picked.bin").apply { writeText("model bytes") }
        val delivery = ImportedModelDelivery(
            context,
            GEMMA_MODEL_SPEC,
            CoroutineScope(Dispatchers.Default),
        )

        delivery.import(Uri.fromFile(source))
        // Stops at any outcome, so a refused import fails with its status instead of hanging.
        val delivered = withTimeout(TIMEOUT) {
            delivery.state.first {
                it is ModelDeliveryState.Delivered ||
                    (it as ModelDeliveryState.InProgress).status != AiModelStatus.Checking &&
                    it.status != AiModelStatus.Installing
            }
        }

        val target = importedModelFile(context, GEMMA_MODEL_SPEC)
        assertEquals(ModelDeliveryState.Delivered(target), delivered)
        assertEquals("model bytes", target.readText())
        target.delete()
        Unit
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
