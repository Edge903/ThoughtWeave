package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.AgreementStatus
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Combines model availability and consent; no AI action may run unless this is READY. */
class AiAvailabilityUseCase @Inject constructor(
    private val agreementRepository: AgreementRepository,
    private val synthesisRepository: MemoSynthesisRepository,
) {
    fun current(): AiAvailability = availability(agreementRepository.currentStatus())

    operator fun invoke(): Flow<AiAvailability> =
        agreementRepository.observeStatus().map(::availability)

    private fun availability(status: AgreementStatus): AiAvailability = when {
        !synthesisRepository.isAvailable -> AiAvailability.NOT_AVAILABLE
        status == AgreementStatus.NOT_ACCEPTED -> AiAvailability.CONSENT_REQUIRED
        else -> AiAvailability.READY
    }
}
