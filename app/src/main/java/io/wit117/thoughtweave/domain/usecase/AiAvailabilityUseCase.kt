package io.wit117.thoughtweave.domain.usecase

import io.wit117.thoughtweave.domain.model.AgreementStatus
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.model.AiModelStatus
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import io.wit117.thoughtweave.domain.repository.AiModelRepository
import io.wit117.thoughtweave.domain.repository.MemoSynthesisRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Combines model state and consent; no AI action may run unless the result is ready. */
class AiAvailabilityUseCase @Inject constructor(
    private val agreementRepository: AgreementRepository,
    private val synthesisRepository: MemoSynthesisRepository,
    private val aiModelRepository: AiModelRepository,
) {
    fun current(): AiAvailability =
        availability(aiModelRepository.status.value, agreementRepository.currentStatus())

    operator fun invoke(): Flow<AiAvailability> =
        combine(aiModelRepository.status, agreementRepository.observeStatus(), ::availability)

    private fun availability(model: AiModelStatus, agreement: AgreementStatus) = AiAvailability(
        model = model,
        consentGiven = agreement == AgreementStatus.ACCEPTED,
        inferenceAvailable = synthesisRepository.isAvailable,
    )
}
