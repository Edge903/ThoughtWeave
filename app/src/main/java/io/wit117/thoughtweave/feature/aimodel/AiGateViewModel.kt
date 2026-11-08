package io.wit117.thoughtweave.feature.aimodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import io.wit117.thoughtweave.domain.usecase.AiAvailabilityUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Availability and consent for AI actions outside S03 (S02-B schedule extraction). */
@HiltViewModel
class AiGateViewModel @Inject constructor(
    aiAvailability: AiAvailabilityUseCase,
    private val agreementRepository: AgreementRepository,
) : ViewModel() {

    val availability: StateFlow<AiAvailability> = aiAvailability()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = aiAvailability.current(),
        )

    fun acceptAgreement() = agreementRepository.accept()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
