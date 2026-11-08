package io.wit117.thoughtweave.feature.memo.editor

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ConfirmDialog
import io.wit117.thoughtweave.core.ui.component.ContextFab
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveFirstDialog by rememberSaveable { mutableStateOf(false) }

    val savedMessage = stringResource(R.string.memo_saved)
    val saveFailedMessage = stringResource(R.string.memo_save_failed)
    val deleteFailedMessage = stringResource(R.string.memo_delete_failed)

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
                    enabled = !state.isBusy && (!state.isDirty || state.canSave),
                    onClick = {
                        if (state.isDirty) showSaveFirstDialog = true else onOpenExtraction(memoId)
                    },
                )
            }
        },
        reserveFabSpace = memoId != null,
    )

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
