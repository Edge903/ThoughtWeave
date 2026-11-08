package io.wit117.thoughtweave.data.ai.delivery

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.File
import kotlinx.coroutines.flow.StateFlow

sealed interface ModelDeliveryState {
    data class InProgress(val status: AiModelStatus) : ModelDeliveryState

    /** The file exists but has not been verified. */
    data class Delivered(val file: File) : ModelDeliveryState
}

/** How the model reaches the device. Replacing the delivery strategy means a new implementation. */
interface ModelDelivery {
    val state: StateFlow<ModelDeliveryState>

    fun refresh()

    fun retry()

    /** Removes the delivered model and obtains it again where the strategy can. */
    fun discard()

    fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
