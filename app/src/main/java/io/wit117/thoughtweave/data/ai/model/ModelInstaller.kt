package io.wit117.thoughtweave.data.ai.model

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Records which verified file is installed, so later launches avoid a full hash. */
class InstallMarkerStore(private val markerFile: File) {

    private fun markerFor(file: File) = "${file.absolutePath}\n${file.length()}\n${file.lastModified()}"

    fun matches(file: File): Boolean =
        markerFile.isFile && markerFile.readText() == markerFor(file)

    fun write(file: File) {
        markerFile.parentFile?.mkdirs()
        markerFile.writeText(markerFor(file))
    }

    fun clear() {
        markerFile.delete()
    }
}

/** Full SHA-256 on first install or import; size and marker on later launches. */
class ModelInstaller @Inject constructor(
    private val spec: ModelSpec,
    private val markerStore: InstallMarkerStore,
) {
    suspend fun verify(file: File): Boolean = withContext(Dispatchers.IO) {
        when {
            !file.isFile || file.length() != spec.sizeBytes -> false
            markerStore.matches(file) -> true
            sha256(file) == spec.sha256 -> {
                markerStore.write(file)
                true
            }
            else -> false
        }
    }

    fun clearMarker() = markerStore.clear()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val BUFFER_BYTES = 1 shl 20
    }
}
