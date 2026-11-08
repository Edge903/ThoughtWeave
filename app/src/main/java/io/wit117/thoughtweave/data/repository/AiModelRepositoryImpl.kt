package io.wit117.thoughtweave.data.repository

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import io.wit117.thoughtweave.data.ai.delivery.ModelDelivery
import io.wit117.thoughtweave.data.ai.delivery.ModelDeliveryState
import io.wit117.thoughtweave.data.ai.model.DeviceCompatibilityChecker
import io.wit117.thoughtweave.data.ai.model.ModelInstaller
import io.wit117.thoughtweave.data.di.ApplicationScope
import io.wit117.thoughtweave.domain.model.AiModelStatus
import io.wit117.thoughtweave.domain.repository.AiModelRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@Singleton
class AiModelRepositoryImpl internal constructor(
    private val delivery: ModelDelivery,
    private val installer: ModelInstaller,
    scope: CoroutineScope,
    isDeviceSupported: Boolean,
) : AiModelRepository {

    @Inject
    constructor(
        delivery: ModelDelivery,
        installer: ModelInstaller,
        compatibility: DeviceCompatibilityChecker,
        @ApplicationScope scope: CoroutineScope,
    ) : this(delivery, installer, scope, compatibility.isSupported())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val status: StateFlow<AiModelStatus> = if (isDeviceSupported) {
        delivery.state
            .transformLatest { state ->
                when (state) {
                    is ModelDeliveryState.InProgress -> emit(state.status)
                    is ModelDeliveryState.Delivered -> {
                        emit(AiModelStatus.Verifying)
                        emit(verify(state))
                    }
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, AiModelStatus.Checking)
    } else {
        MutableStateFlow(AiModelStatus.DeviceUnsupported)
    }

    private suspend fun verify(state: ModelDeliveryState.Delivered): AiModelStatus = try {
        if (installer.verify(state.file)) AiModelStatus.Ready else AiModelStatus.Corrupted
    } catch (_: IOException) {
        AiModelStatus.Failed
    }

    override fun refresh() = delivery.refresh()

    override fun retry() = delivery.retry()

    override fun redownload() {
        installer.clearMarker()
        delivery.discard()
    }

    override fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
        delivery.showConfirmation(launcher)
}
