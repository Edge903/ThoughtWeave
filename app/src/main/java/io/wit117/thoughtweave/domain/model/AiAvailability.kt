package io.wit117.thoughtweave.domain.model

/** Bump when the agreement text changes: acceptance is stored per version, so users are asked again. */
const val CURRENT_AGREEMENT_VERSION = 1

enum class AgreementStatus { NOT_ACCEPTED, ACCEPTED }

/**
 * The only AI state the UI sees. Delivery and consent are independent: the model can download
 * before the user has consented, but no AI action runs until [isReady].
 */
data class AiAvailability(
    val model: AiModelStatus,
    val consentGiven: Boolean,
) {
    val isModelUsable: Boolean get() = model == AiModelStatus.Ready
    val isReady: Boolean get() = isModelUsable && consentGiven
}
