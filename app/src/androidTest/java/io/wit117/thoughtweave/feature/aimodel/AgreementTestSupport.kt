package io.wit117.thoughtweave.feature.aimodel

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.wit117.thoughtweave.data.repository.AGREEMENT_PREFS_NAME
import io.wit117.thoughtweave.data.repository.KEY_ACCEPTED_VERSION
import io.wit117.thoughtweave.domain.model.CURRENT_AGREEMENT_VERSION

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
