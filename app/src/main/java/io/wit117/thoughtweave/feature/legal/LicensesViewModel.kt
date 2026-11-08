package io.wit117.thoughtweave.feature.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.domain.model.AgreementStatus
import io.wit117.thoughtweave.domain.repository.AgreementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** S05: consent withdrawal. */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    private val agreementRepository: AgreementRepository,
) : ViewModel() {

    val agreementStatus: StateFlow<AgreementStatus> = agreementRepository.observeStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = agreementRepository.currentStatus(),
        )

    fun withdrawConsent() = agreementRepository.withdraw()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
