package io.wit117.thoughtweave.feature.memo.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.domain.model.SpeechError
import io.wit117.thoughtweave.feature.aimodel.AiGateViewModel
import io.wit117.thoughtweave.feature.dictation.DictationEffect
import io.wit117.thoughtweave.feature.dictation.DictationViewModel
import io.wit117.thoughtweave.feature.legal.AiConsentDialog
import io.wit117.thoughtweave.core.ui.component.ConfirmDialog
import io.wit117.thoughtweave.core.ui.component.ContextFab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** S02-A 메모 작성 / S02-B 메모 조회·수정. */
@Composable
fun MemoEditorScreen(
    viewModel: MemoEditorViewModel,
    memoId: Long?,
    onBack: () -> Unit,
    onOpenExtraction: (Long) -> Unit,
    onUnavailable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    aiGate: AiGateViewModel = hiltViewModel(),
    dictation: DictationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aiAvailability by aiGate.availability.collectAsStateWithLifecycle()
    val dictationState by dictation.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveFirstDialog by rememberSaveable { mutableStateOf(false) }
    var showConsentDialog by rememberSaveable { mutableStateOf(false) }
    var showMicRationale by rememberSaveable { mutableStateOf(false) }
    val insertions = remember { Channel<String>(Channel.BUFFERED) }
    val context = LocalContext.current
    val activity = LocalActivity.current

    fun proceedToExtraction(id: Long) {
        if (state.isDirty) showSaveFirstDialog = true else onOpenExtraction(id)
    }

    val savedMessage = stringResource(R.string.memo_saved)
    val saveFailedMessage = stringResource(R.string.memo_save_failed)
    val deleteFailedMessage = stringResource(R.string.memo_delete_failed)
    val micBlockedMessage = stringResource(R.string.dictation_permission_blocked)
    val micDeniedMessage = stringResource(R.string.dictation_permission_denied)
    val appSettingsLabel = stringResource(R.string.action_app_settings)
    val noSpeechMessage = stringResource(R.string.dictation_no_speech)
    val busyMessage = stringResource(R.string.dictation_busy)
    val failedMessage = stringResource(R.string.dictation_failed)
    val languageMissingFormat = stringResource(R.string.dictation_language_unavailable)

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> dictation.start()
            // No rationale after a denial means the system no longer asks: only Settings can grant it.
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false ->
                scope.launch {
                    val result = snackbarHostState.showSnackbar(micBlockedMessage, actionLabel = appSettingsLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }
                }
            else -> scope.launch { snackbarHostState.showSnackbar(micDeniedMessage) }
        }
    }

    fun onMicClick() {
        when {
            dictationState.isListening -> dictation.stop()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> dictation.start()
            else -> showMicRationale = true
        }
    }

    // Stop listening when the app goes to the background; what was heard is still inserted.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { dictation.stop() }

    LaunchedEffect(dictation) {
        dictation.effects.collect { effect ->
            when (effect) {
                is DictationEffect.Insert -> insertions.send(effect.text)
                is DictationEffect.Failed -> launch {
                    snackbarHostState.showSnackbar(
                        when (effect.error) {
                            SpeechError.NO_SPEECH -> noSpeechMessage
                            SpeechError.LANGUAGE_UNAVAILABLE -> languageMissingFormat.format(effect.language.nativeName)
                            SpeechError.PERMISSION_DENIED -> micDeniedMessage
                            SpeechError.BUSY -> busyMessage
                            SpeechError.OTHER -> failedMessage
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MemoEditorEffect.Close -> onBack()
                is MemoEditorEffect.OpenExtraction -> onOpenExtraction(effect.memoId)
                MemoEditorEffect.Saved -> launch { snackbarHostState.showSnackbar(savedMessage) }
                MemoEditorEffect.SaveFailed -> launch { snackbarHostState.showSnackbar(saveFailedMessage) }
                MemoEditorEffect.DeleteFailed -> launch { snackbarHostState.showSnackbar(deleteFailedMessage) }
            }
        }
    }

    if (state.isUnavailable) {
        onUnavailable()
        return
    }

    MemoEditorContent(
        screenTitle = stringResource(if (state.isExisting) R.string.title_memo else R.string.title_new_memo),
        draft = state.draft,
        isLoading = state.isLoading,
        isDirty = state.isDirty,
        canSave = state.canSave,
        snackbarHostState = snackbarHostState,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (dictationState.isSupported && !state.isLoading) {
                IconButton(onClick = ::onMicClick, enabled = !state.isBusy) {
                    Icon(
                        painter = painterResource(
                            if (dictationState.isListening) R.drawable.ic_stop else R.drawable.ic_mic,
                        ),
                        contentDescription = stringResource(
                            if (dictationState.isListening) R.string.action_stop_dictation else R.string.action_dictate,
                        ),
                    )
                }
            }
            if (state.isExisting && !state.isLoading) {
                IconButton(onClick = { showDeleteDialog = true }, enabled = !state.isBusy) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
        },
        floatingActionButton = {
            if (memoId != null && !state.isLoading) {
                ContextFab(
                    text = stringResource(R.string.fab_extract_schedule),
                    iconRes = R.drawable.ic_calendar_add_on,
                    enabled = aiAvailability.isModelUsable &&
                        !state.isBusy && (!state.isDirty || state.canSave),
                    onClick = {
                        if (!aiAvailability.consentGiven) {
                            showConsentDialog = true
                        } else {
                            proceedToExtraction(memoId)
                        }
                    },
                )
            }
        },
        reserveFabSpace = memoId != null,
        notices = if (dictationState.isListening) {
            listOf(
                dictationState.partialText.ifBlank { stringResource(R.string.dictation_listening) },
            )
        } else {
            emptyList()
        },
        insertions = remember(insertions) { insertions.receiveAsFlow() },
    )

    if (showMicRationale) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_mic_permission_title),
            message = stringResource(R.string.dialog_mic_permission_message),
            confirmText = stringResource(R.string.action_continue),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showMicRationale = false
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showMicRationale = false },
        )
    }

    if (showConsentDialog && memoId != null) {
        AiConsentDialog(
            onAccept = {
                showConsentDialog = false
                aiGate.acceptAgreement()
                proceedToExtraction(memoId)
            },
            onDismiss = { showConsentDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_memo_title),
            message = stringResource(R.string.dialog_delete_memo_message),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showSaveFirstDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_save_first_title),
            message = stringResource(R.string.dialog_save_first_message),
            confirmText = stringResource(R.string.action_save_and_continue),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showSaveFirstDialog = false
                viewModel.saveAndOpenExtraction()
            },
            onDismiss = { showSaveFirstDialog = false },
        )
    }
}
