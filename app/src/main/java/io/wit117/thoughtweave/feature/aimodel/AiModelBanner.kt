package io.wit117.thoughtweave.feature.aimodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.BannerAction
import io.wit117.thoughtweave.core.ui.component.InfoBanner
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.model.AiModelStatus
import io.wit117.thoughtweave.domain.repository.AiModelRepository
import javax.inject.Inject

@HiltViewModel
class AiModelViewModel @Inject constructor(
    val repository: AiModelRepository,
) : ViewModel()

private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing can handle it; the banner's message still explains the state.
    }
}

/** S03 banner for model delivery. Consent has its own banner, because the two are independent. */
@Composable
fun AiModelBanner(
    availability: AiAvailability,
    viewModel: AiModelViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val repository = viewModel.repository
    val confirmation = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {}
    val retry = BannerAction(stringResource(R.string.action_retry), repository::retry)

    when (val model = availability.model) {
        AiModelStatus.Checking -> Unit
        AiModelStatus.Ready -> if (!availability.inferenceAvailable) {
            InfoBanner(stringResource(R.string.ai_unavailable_message))
        }
        AiModelStatus.NotDownloaded -> InfoBanner(
            message = stringResource(R.string.ai_model_not_downloaded),
            extraAction = { ModelImportAction() },
        )
        AiModelStatus.Queued -> InfoBanner(stringResource(R.string.ai_model_queued))
        is AiModelStatus.Downloading -> InfoBanner(
            message = stringResource(R.string.ai_model_downloading, model.percent),
            progress = model.percent / 100f,
        )
        is AiModelStatus.WaitingForWifi -> InfoBanner(
            message = stringResource(R.string.ai_model_waiting_for_wifi),
            actions = listOf(
                BannerAction(
                    text = stringResource(
                        R.string.ai_model_allow_mobile_data,
                        Formatter.formatShortFileSize(context, model.totalBytes),
                    ),
                    onClick = { repository.showConfirmation(confirmation) },
                ),
            ),
        )
        AiModelStatus.NeedsPlayConfirmation -> InfoBanner(
            message = stringResource(R.string.ai_model_needs_confirmation),
            actions = listOf(
                BannerAction(stringResource(R.string.action_confirm)) {
                    repository.showConfirmation(confirmation)
                },
            ),
        )
        AiModelStatus.Installing -> InfoBanner(stringResource(R.string.ai_model_installing))
        AiModelStatus.Verifying -> InfoBanner(stringResource(R.string.ai_model_verifying))
        AiModelStatus.Corrupted -> InfoBanner(
            message = stringResource(R.string.ai_model_corrupted),
            actions = listOf(BannerAction(stringResource(R.string.action_redownload), repository::redownload)),
            extraAction = { ModelImportAction() },
        )
        AiModelStatus.Failed -> InfoBanner(
            message = stringResource(R.string.ai_model_failed),
            actions = listOf(retry),
            extraAction = { ModelImportAction() },
        )
        is AiModelStatus.InsufficientStorage -> InfoBanner(
            message = stringResource(
                R.string.ai_model_insufficient_storage,
                Formatter.formatShortFileSize(context, model.requiredBytes),
                Formatter.formatShortFileSize(context, model.freeBytes),
            ),
            actions = listOf(
                BannerAction(stringResource(R.string.action_storage_settings)) {
                    context.openSafely(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                },
                retry,
            ),
        )
        AiModelStatus.NotInstalledFromPlay -> InfoBanner(
            message = stringResource(R.string.ai_model_not_from_play),
            actions = listOf(
                BannerAction(stringResource(R.string.action_open_play)) {
                    context.openSafely(
                        Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri()),
                    )
                },
            ),
        )
        AiModelStatus.PlayUnavailable -> InfoBanner(stringResource(R.string.ai_model_play_unavailable))
        AiModelStatus.DeviceUnsupported -> InfoBanner(stringResource(R.string.ai_model_device_unsupported))
    }
}
