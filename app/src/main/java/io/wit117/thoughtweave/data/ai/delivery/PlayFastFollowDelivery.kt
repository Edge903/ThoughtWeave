package io.wit117.thoughtweave.data.ai.delivery

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.play.core.aipacks.AiPackManager
import com.google.android.play.core.aipacks.AiPackManagerFactory
import com.google.android.play.core.aipacks.AiPackState
import com.google.android.play.core.aipacks.AiPackStateUpdateListener
import com.google.android.play.core.aipacks.model.AiPackErrorCode
import com.google.android.play.core.aipacks.model.AiPackStatus
import com.google.android.play.core.aipacks.model.AiPackStorageMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.data.ai.model.ModelSpec
import io.wit117.thoughtweave.data.ai.model.availableStorageBytes
import io.wit117.thoughtweave.data.ai.model.storageShortfall
import io.wit117.thoughtweave.data.di.ApplicationScope
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val PACK_NAME = "gemma_pack"

/** The status for a pack that is not delivered yet; COMPLETED is resolved by the caller. */
fun mapPackStatus(
    status: Int,
    errorCode: Int,
    percent: Int,
    totalBytes: Long,
    requiredBytes: Long,
    freeBytes: Long,
): AiModelStatus = when (status) {
    AiPackStatus.PENDING -> AiModelStatus.Queued
    AiPackStatus.DOWNLOADING -> AiModelStatus.Downloading(percent)
    AiPackStatus.TRANSFERRING -> AiModelStatus.Installing
    AiPackStatus.WAITING_FOR_WIFI -> AiModelStatus.WaitingForWifi(totalBytes)
    AiPackStatus.REQUIRES_USER_CONFIRMATION -> AiModelStatus.NeedsPlayConfirmation
    AiPackStatus.FAILED -> mapPackError(errorCode, requiredBytes, freeBytes)
    else -> AiModelStatus.NotDownloaded
}

fun mapPackError(errorCode: Int, requiredBytes: Long, freeBytes: Long): AiModelStatus = when (errorCode) {
    AiPackErrorCode.INSUFFICIENT_STORAGE -> AiModelStatus.InsufficientStorage(requiredBytes, freeBytes)
    AiPackErrorCode.APP_NOT_OWNED,
    AiPackErrorCode.UNRECOGNIZED_INSTALLATION,
    AiPackErrorCode.APP_UNAVAILABLE,
    -> AiModelStatus.NotInstalledFromPlay
    AiPackErrorCode.API_NOT_AVAILABLE -> AiModelStatus.PlayUnavailable
    else -> AiModelStatus.Failed
}

/** Only this class talks to Play. */
@Singleton
class PlayFastFollowDelivery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spec: ModelSpec,
    @ApplicationScope private val scope: CoroutineScope,
) : ModelDelivery {

    private val manager: AiPackManager by lazy { AiPackManagerFactory.getInstance(context) }
    private val _state = MutableStateFlow<ModelDeliveryState>(
        ModelDeliveryState.InProgress(AiModelStatus.Checking),
    )
    override val state: StateFlow<ModelDeliveryState> = _state.asStateFlow()

    private var isListening = false
    private val listener = AiPackStateUpdateListener { packState ->
        if (packState.name() == PACK_NAME) apply(packState)
    }

    override fun refresh() {
        if (!isListening) {
            manager.registerListener(listener)
            isListening = true
        }
        launchCatching {
            val delivered = locate()
            if (delivered != null) {
                _state.value = delivered
            } else {
                val packState = manager.getPackStates(listOf(PACK_NAME)).await().packStates()[PACK_NAME]
                if (packState == null) {
                    _state.value = ModelDeliveryState.InProgress(AiModelStatus.Failed)
                } else if (needsFetch(packState.status())) {
                    fetch()
                } else {
                    apply(packState)
                }
            }
        }
    }

    override fun retry() = launchCatching { fetch() }

    override fun discard() = launchCatching {
        manager.removePack(PACK_NAME).await()
        fetch()
    }

    override fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
        manager.showConfirmationDialog(launcher)

    private fun needsFetch(status: Int) =
        status == AiPackStatus.NOT_INSTALLED ||
            status == AiPackStatus.UNKNOWN ||
            status == AiPackStatus.CANCELED

    private suspend fun fetch() {
        val shortfall = spec.storageShortfall(context)
        if (shortfall != null) {
            _state.value = ModelDeliveryState.InProgress(shortfall)
            return
        }
        manager.fetch(listOf(PACK_NAME)).await().packStates()[PACK_NAME]?.let(::apply)
    }

    private fun apply(packState: AiPackState) {
        _state.value = if (packState.status() == AiPackStatus.COMPLETED) {
            locate() ?: ModelDeliveryState.InProgress(AiModelStatus.Failed)
        } else {
            ModelDeliveryState.InProgress(
                mapPackStatus(
                    status = packState.status(),
                    errorCode = packState.errorCode(),
                    percent = packState.transferProgressPercentage(),
                    totalBytes = packState.totalBytesToDownload(),
                    requiredBytes = spec.sizeBytes,
                    freeBytes = availableStorageBytes(context),
                ),
            )
        }
    }

    /** LiteRT-LM needs a plain file path, so only extracted files at offset 0 can be used. */
    private fun locate(): ModelDeliveryState? {
        val location = manager.getPackLocation(PACK_NAME) ?: return null
        if (location.packStorageMethod() != AiPackStorageMethod.STORAGE_FILES) return unsupported()
        val asset = manager.getAiAssetLocation(PACK_NAME, spec.fileName) ?: return null
        if (asset.offset() != 0L) return unsupported()
        return ModelDeliveryState.Delivered(File(asset.path()))
    }

    private fun unsupported() = ModelDeliveryState.InProgress(AiModelStatus.DeviceUnsupported)

    private fun launchCatching(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val code = (e as? ApiException)?.statusCode ?: AiPackErrorCode.INTERNAL_ERROR
                _state.value = ModelDeliveryState.InProgress(
                    mapPackError(code, spec.sizeBytes, availableStorageBytes(context)),
                )
            }
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
