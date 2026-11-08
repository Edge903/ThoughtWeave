package io.wit117.thoughtweave.domain.repository

import io.wit117.thoughtweave.domain.model.AgreementStatus
import kotlinx.coroutines.flow.Flow

interface AgreementRepository {
    fun currentStatus(): AgreementStatus

    fun observeStatus(): Flow<AgreementStatus>

    /** Records acceptance of the current agreement version. */
    fun accept()

    fun withdraw()
}
