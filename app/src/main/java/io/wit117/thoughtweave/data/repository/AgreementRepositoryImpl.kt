package io.wit117.thoughtweave.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.domain.model.AgreementStatus
import io.wit117.thoughtweave.domain.model.CURRENT_AGREEMENT_VERSION
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Excluded from backups (`backup_rules`, `data_extraction_rules`): consent is per install. */
const val AGREEMENT_PREFS_NAME = "agreement"
const val KEY_ACCEPTED_VERSION = "accepted_version"

class AgreementRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AgreementRepository {

    private val prefs = context.getSharedPreferences(AGREEMENT_PREFS_NAME, Context.MODE_PRIVATE)

    override fun currentStatus(): AgreementStatus =
        if (prefs.getInt(KEY_ACCEPTED_VERSION, 0) == CURRENT_AGREEMENT_VERSION) {
            AgreementStatus.ACCEPTED
        } else {
            AgreementStatus.NOT_ACCEPTED
        }

    override fun observeStatus(): Flow<AgreementStatus> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(currentStatus())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(currentStatus())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun accept() {
        prefs.edit { putInt(KEY_ACCEPTED_VERSION, CURRENT_AGREEMENT_VERSION) }
    }

    override fun withdraw() {
        prefs.edit { remove(KEY_ACCEPTED_VERSION) }
    }
}
