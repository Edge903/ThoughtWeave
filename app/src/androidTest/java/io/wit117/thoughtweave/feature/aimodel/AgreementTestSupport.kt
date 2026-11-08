package io.wit117.thoughtweave.feature.aimodel

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.data.ai.delivery.importedModelFile
import io.wit117.thoughtweave.data.ai.model.GEMMA_MODEL_SPEC
import io.wit117.thoughtweave.data.ai.model.InstallMarkerStore
import io.wit117.thoughtweave.data.di.modelMarkerFile
import io.wit117.thoughtweave.data.repository.AGREEMENT_PREFS_NAME
import io.wit117.thoughtweave.data.repository.KEY_ACCEPTED_VERSION
import io.wit117.thoughtweave.domain.model.CURRENT_AGREEMENT_VERSION
import java.io.RandomAccessFile
import org.junit.rules.ExternalResource

/** Installs a sparse file of the model's size plus a marker, or removes both. Runs before launch. */
class ModelStateRule(private val installed: Boolean = true) : ExternalResource() {
    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = importedModelFile(context, GEMMA_MODEL_SPEC)
        val marker = InstallMarkerStore(modelMarkerFile(context))
        if (installed) {
            model.parentFile?.mkdirs()
            RandomAccessFile(model, "rw").use { it.setLength(GEMMA_MODEL_SPEC.sizeBytes) }
            marker.write(model)
        } else {
            model.delete()
            marker.clear()
        }
    }
}

/** Consent is stored per install, so each test states the consent it needs. */
internal fun setAgreementAccepted(accepted: Boolean) {
    val prefs = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences(AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    if (accepted) {
        editor.putInt(KEY_ACCEPTED_VERSION, CURRENT_AGREEMENT_VERSION)
    } else {
        editor.remove(KEY_ACCEPTED_VERSION)
    }
    editor.commit()
}
