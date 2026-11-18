package io.wit117.thoughtweave.feature.memo.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ConfirmDialog
import io.wit117.thoughtweave.core.ui.component.InfoBanner
import io.wit117.thoughtweave.feature.dictation.withDictation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** The user-editable fields of a memo. */
data class MemoDraft(val title: String = "", val content: String = "") {
    val hasContent: Boolean get() = title.isNotBlank() || content.isNotBlank()
}

/**
 * Editor shared by S02-A, S02-B and S02-C, including the unsaved-changes guard. The draft is hoisted;
 * only the content field's cursor is kept here, so [insertions] (dictated text) land at the cursor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditorContent(
    screenTitle: String,
    draft: MemoDraft,
    isLoading: Boolean,
    isDirty: Boolean,
    canSave: Boolean,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    reserveFabSpace: Boolean = false,
    isReadOnly: Boolean = false,
    showProgress: Boolean = false,
    notices: List<String> = emptyList(),
    insertions: Flow<String> = emptyFlow(),
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    var contentField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(draft.content, TextRange(draft.content.length)))
    }
    // contentField is the field's truth. Content changed outside the field (loaded, restored or streamed)
    // is copied into it with the cursor at the end; edits made here already match, so this is a no-op then.
    LaunchedEffect(draft.content) {
        if (contentField.text != draft.content) {
            contentField = TextFieldValue(draft.content, TextRange(draft.content.length))
        }
    }
    // Until that effect runs, show the outside content rather than a stale field for one frame.
    val shownContent = if (contentField.text == draft.content) {
        contentField
    } else {
        TextFieldValue(draft.content, TextRange(draft.content.length))
    }
    val latestOnContentChange by rememberUpdatedState(onContentChange)
    LaunchedEffect(insertions) {
        insertions.collect { text ->
            // Read the state itself, not the composed value: pieces can arrive before the next recomposition.
            val updated = contentField.withDictation(text)
            contentField = updated
            latestOnContentChange(updated.text)
        }
    }

    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    actions()
                    TextButton(onClick = onSave, enabled = canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .fillMaxSize(),
            ) {
                if (showProgress) LinearProgressIndicator(Modifier.fillMaxWidth())
                notices.forEach { InfoBanner(it) }
                TextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isReadOnly,
                    textStyle = MaterialTheme.typography.titleLarge,
                    placeholder = { Text(stringResource(R.string.memo_title_hint)) },
                    singleLine = true,
                    colors = borderlessFieldColors(),
                )
                HorizontalDivider()
                TextField(
                    value = shownContent,
                    onValueChange = {
                        contentField = it
                        if (it.text != draft.content) onContentChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = if (reserveFabSpace) 88.dp else 0.dp),
                    readOnly = isReadOnly,
                    placeholder = { Text(stringResource(R.string.memo_content_hint)) },
                    colors = borderlessFieldColors(),
                )
            }
        }
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_discard_title),
            message = stringResource(R.string.dialog_discard_message),
            confirmText = stringResource(R.string.action_discard),
            dismissText = stringResource(R.string.action_keep_editing),
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun borderlessFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
