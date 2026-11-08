package io.wit117.thoughtweave.domain.model

/** Where the on-device model is in its delivery and verification. */
sealed interface AiModelStatus {
    data object Checking : AiModelStatus
    data object NotDownloaded : AiModelStatus
    data object Queued : AiModelStatus
    data class Downloading(val percent: Int) : AiModelStatus
    data class WaitingForWifi(val totalBytes: Long) : AiModelStatus
    data object NeedsPlayConfirmation : AiModelStatus
    data object Installing : AiModelStatus
    data object Verifying : AiModelStatus
    data object Ready : AiModelStatus
    data object Corrupted : AiModelStatus
    data object Failed : AiModelStatus
    data class InsufficientStorage(val requiredBytes: Long, val freeBytes: Long) : AiModelStatus
    data object NotInstalledFromPlay : AiModelStatus
    data object PlayUnavailable : AiModelStatus
    data object DeviceUnsupported : AiModelStatus
}
