package io.wit117.thoughtweave.domain.model

/** Bump when the agreement text changes: acceptance is stored per version, so users are asked again. */
const val CURRENT_AGREEMENT_VERSION = 1

enum class AgreementStatus { NOT_ACCEPTED, ACCEPTED }

/** The only AI state the UI sees. Phase 5b adds the model delivery states. */
enum class AiAvailability { NOT_AVAILABLE, CONSENT_REQUIRED, READY }
