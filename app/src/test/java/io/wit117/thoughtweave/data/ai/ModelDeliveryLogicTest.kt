package io.wit117.thoughtweave.data.ai

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.aipacks.model.AiPackErrorCode
import com.google.android.play.core.aipacks.model.AiPackStatus
import io.wit117.thoughtweave.data.ai.delivery.ModelDelivery
import io.wit117.thoughtweave.data.ai.delivery.ModelDeliveryState
import io.wit117.thoughtweave.data.ai.delivery.mapPackStatus
import io.wit117.thoughtweave.data.ai.model.InstallMarkerStore
import io.wit117.thoughtweave.data.ai.model.ModelInstaller
import io.wit117.thoughtweave.data.ai.model.ModelSpec
import io.wit117.thoughtweave.data.repository.AiModelRepositoryImpl
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class FakeDelivery : ModelDelivery {
    override val state = MutableStateFlow<ModelDeliveryState>(
        ModelDeliveryState.InProgress(AiModelStatus.NotDownloaded),
    )
    var discarded = false
    override fun refresh() = Unit
    override fun retry() = Unit
    override fun discard() {
        discarded = true
    }

    override fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) = false
}

class ModelDeliveryLogicTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val content = "model".toByteArray()

    private val spec = ModelSpec(
        fileName = "m.litertlm",
        sizeBytes = content.size.toLong(),
        sha256 = realSha(content),
    )

    private fun realSha(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun status(code: Int, error: Int = AiPackErrorCode.NO_ERROR) =
        mapPackStatus(code, error, percent = 30, totalBytes = 900, requiredBytes = 500, freeBytes = 100)

    @Test
    fun mapsProgressStates() {
        assertEquals(AiModelStatus.Queued, status(AiPackStatus.PENDING))
        assertEquals(AiModelStatus.Downloading(30), status(AiPackStatus.DOWNLOADING))
        assertEquals(AiModelStatus.Installing, status(AiPackStatus.TRANSFERRING))
        assertEquals(AiModelStatus.WaitingForWifi(900), status(AiPackStatus.WAITING_FOR_WIFI))
        assertEquals(AiModelStatus.NeedsPlayConfirmation, status(AiPackStatus.REQUIRES_USER_CONFIRMATION))
        assertEquals(AiModelStatus.NotDownloaded, status(AiPackStatus.NOT_INSTALLED))
    }

    @Test
    fun mapsFailures() {
        fun failed(error: Int) = status(AiPackStatus.FAILED, error)
        assertEquals(AiModelStatus.Failed, failed(AiPackErrorCode.NETWORK_ERROR))
        assertEquals(AiModelStatus.InsufficientStorage(500, 100), failed(AiPackErrorCode.INSUFFICIENT_STORAGE))
        assertEquals(AiModelStatus.NotInstalledFromPlay, failed(AiPackErrorCode.APP_NOT_OWNED))
        assertEquals(AiModelStatus.NotInstalledFromPlay, failed(AiPackErrorCode.UNRECOGNIZED_INSTALLATION))
        assertEquals(AiModelStatus.PlayUnavailable, failed(AiPackErrorCode.API_NOT_AVAILABLE))
    }

    private fun installer(sha: String = realSha(content)) = ModelInstaller(
        spec.copy(sha256 = sha),
        InstallMarkerStore(File(folder.root, "marker")),
    )

    private fun modelFile(bytes: ByteArray = content) =
        File(folder.root, "m.litertlm").apply { writeBytes(bytes) }

    @Test
    fun firstVerifyHashesAndWritesMarker() = runTest {
        val file = modelFile()

        assertTrue(installer().verify(file))
        assertTrue(File(folder.root, "marker").isFile)
    }

    @Test
    fun wrongHashFailsAndWritesNoMarker() = runTest {
        assertFalse(installer(sha = "0".repeat(64)).verify(modelFile()))
        assertFalse(File(folder.root, "marker").exists())
    }

    @Test
    fun wrongSizeFails() = runTest {
        assertFalse(installer().verify(modelFile("longer than expected".toByteArray())))
    }

    @Test
    fun laterLaunchTrustsMarkerWithoutHashing() = runTest {
        val file = modelFile()
        assertTrue(installer().verify(file))

        // A wrong expected hash proves the file was not hashed again.
        assertTrue(installer(sha = "0".repeat(64)).verify(file))
    }

    @Test
    fun changedFileInvalidatesMarker() = runTest {
        val file = modelFile()
        assertTrue(installer().verify(file))
        file.setLastModified(file.lastModified() + 5_000)

        assertFalse(installer(sha = "0".repeat(64)).verify(file))
    }

    @Test
    fun repositoryReportsReadyAndCorrupted() = runTest {
        val delivery = FakeDelivery()
        val good = AiModelRepositoryImpl(delivery, installer(), backgroundScope, isDeviceSupported = true)
        delivery.state.value = ModelDeliveryState.Delivered(modelFile())
        good.status.first { it == AiModelStatus.Ready }

        val corruptDelivery = FakeDelivery()
        val bad = AiModelRepositoryImpl(
            corruptDelivery,
            ModelInstaller(spec.copy(sha256 = "0".repeat(64)), InstallMarkerStore(File(folder.root, "other"))),
            backgroundScope,
            isDeviceSupported = true,
        )
        corruptDelivery.state.value = ModelDeliveryState.Delivered(modelFile())
        bad.status.first { it == AiModelStatus.Corrupted }

        bad.redownload()
        assertTrue(corruptDelivery.discarded)
    }

    @Test
    fun unsupportedDeviceIgnoresDelivery() = runTest {
        val repository = AiModelRepositoryImpl(FakeDelivery(), installer(), backgroundScope, isDeviceSupported = false)

        assertEquals(AiModelStatus.DeviceUnsupported, repository.status.value)
    }
}
