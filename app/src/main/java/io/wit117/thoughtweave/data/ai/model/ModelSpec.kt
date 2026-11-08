package io.wit117.thoughtweave.data.ai.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.IOException

data class ModelSpec(val fileName: String, val sizeBytes: Long, val sha256: String)

val GEMMA_MODEL_SPEC = ModelSpec(
    fileName = "gemma3-1b-it-int4.litertlm",
    sizeBytes = 584_417_280L,
    sha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be",
)

/** Space that can be used for the model, counting cached data the system can clear. */
@SuppressLint("UsableSpace") // Only the fallback; API 26+ uses getAllocatableBytes.
fun availableStorageBytes(context: Context): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            return context.getSystemService(StorageManager::class.java)
                .getAllocatableBytes(StorageManager.UUID_DEFAULT)
        } catch (_: IOException) {
            // Fall through to the plain free-space figure.
        }
    }
    return context.filesDir.usableSpace
}

/** @return the status to report if the device cannot hold the model, otherwise null. */
fun ModelSpec.storageShortfall(context: Context): AiModelStatus.InsufficientStorage? {
    val free = availableStorageBytes(context)
    return if (free < sizeBytes) AiModelStatus.InsufficientStorage(sizeBytes, free) else null
}
