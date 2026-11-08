package io.wit117.thoughtweave.data.ai.delivery

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wit117.thoughtweave.data.ai.model.ModelSpec
import io.wit117.thoughtweave.data.ai.model.storageShortfall
import io.wit117.thoughtweave.data.di.ApplicationScope
import io.wit117.thoughtweave.domain.model.AiModelStatus
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun importedModelFile(context: Context, spec: ModelSpec): File =
    File(File(context.noBackupFilesDir, "model"), spec.fileName)

/** Debug-only delivery: the model comes from the system file picker. */
@Singleton
class ImportedModelDelivery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spec: ModelSpec,
    @ApplicationScope private val scope: CoroutineScope,
) : ModelDelivery {

    private val target = importedModelFile(context, spec)
    private val _state = MutableStateFlow<ModelDeliveryState>(
        ModelDeliveryState.InProgress(AiModelStatus.Checking),
    )
    override val state: StateFlow<ModelDeliveryState> = _state.asStateFlow()

    override fun refresh() {
        if (_state.value == ModelDeliveryState.InProgress(AiModelStatus.Installing)) return
        _state.value = if (target.isFile) {
            ModelDeliveryState.Delivered(target)
        } else {
            ModelDeliveryState.InProgress(AiModelStatus.NotDownloaded)
        }
    }

    override fun retry() = refresh()

    override fun discard() {
        target.delete()
        refresh()
    }

    override fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) = false

    fun import(uri: Uri) {
        val parent = checkNotNull(target.parentFile).apply { mkdirs() }
        val shortfall = spec.storageShortfall(context)
        if (shortfall != null) {
            _state.value = ModelDeliveryState.InProgress(shortfall)
            return
        }
        _state.value = ModelDeliveryState.InProgress(AiModelStatus.Installing)
        scope.launch(Dispatchers.IO) {
            val temporary = File(parent, "${spec.fileName}.importing")
            _state.value = try {
                val input = checkNotNull(context.contentResolver.openInputStream(uri))
                input.use { source -> temporary.outputStream().use { source.copyTo(it) } }
                target.delete()
                if (!temporary.renameTo(target)) throw IOException("Could not move the imported model")
                ModelDeliveryState.Delivered(target)
            } catch (_: IOException) {
                temporary.delete()
                ModelDeliveryState.InProgress(AiModelStatus.Failed)
            } catch (_: SecurityException) {
                temporary.delete()
                ModelDeliveryState.InProgress(AiModelStatus.Failed)
            }
        }
    }
}
