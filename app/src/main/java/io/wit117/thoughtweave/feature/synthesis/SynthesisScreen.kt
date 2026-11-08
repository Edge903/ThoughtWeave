package io.wit117.thoughtweave.feature.synthesis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.BannerAction
import io.wit117.thoughtweave.core.ui.component.ContextFab
import io.wit117.thoughtweave.core.ui.component.InfoBanner
import io.wit117.thoughtweave.core.ui.component.EmptyState
import io.wit117.thoughtweave.core.ui.component.ScrollToTopOnNewFirstItem
import io.wit117.thoughtweave.domain.model.AiAvailability
import io.wit117.thoughtweave.domain.model.AiModelStatus
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.feature.aimodel.AiModelBanner
import io.wit117.thoughtweave.feature.legal.AiConsentDialog

/** S03 – AI 메모 통합, including the POP01 preview sheet. */
@Composable
fun SynthesisScreen(
    onSynthesize: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SynthesisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SynthesisContent(
        uiState = uiState,
        onToggleSelection = viewModel::toggleSelection,
        onSynthesize = onSynthesize,
        onAcceptAgreement = viewModel::acceptAgreement,
        modelBanner = { AiModelBanner(it) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesisContent(
    uiState: SynthesisUiState,
    onToggleSelection: (Long) -> Unit,
    onSynthesize: (List<Long>) -> Unit,
    onAcceptAgreement: () -> Unit,
    modelBanner: @Composable (AiAvailability) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewMemoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showConsentDialog by rememberSaveable { mutableStateOf(false) }
    val loaded = uiState as? SynthesisUiState.Loaded

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val count = loaded?.selectedIds?.size ?: 0
                    Text(
                        if (count == 0) {
                            stringResource(R.string.title_ai_merge)
                        } else {
                            pluralStringResource(R.plurals.ai_merge_selected_count, count, count)
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            ContextFab(
                text = stringResource(R.string.fab_ai_synthesize),
                iconRes = R.drawable.ic_auto_awesome,
                onClick = { loaded?.let { onSynthesize(it.selectedIds) } },
                enabled = loaded?.canSynthesize == true,
            )
        },
    ) { innerPadding ->
        val bodyModifier = Modifier.padding(innerPadding).fillMaxSize()
        when (uiState) {
            SynthesisUiState.Loading -> Box(bodyModifier, Alignment.Center) {
                CircularProgressIndicator()
            }

            SynthesisUiState.Failed -> EmptyState(
                title = stringResource(R.string.memo_list_error_title),
                message = stringResource(R.string.memo_list_error_message),
                iconRes = R.drawable.ic_auto_awesome,
                modifier = bodyModifier,
            )

            is SynthesisUiState.Loaded -> Column(bodyModifier) {
                modelBanner(uiState.aiAvailability)
                if (!uiState.aiAvailability.consentGiven &&
                    uiState.aiAvailability.model != AiModelStatus.DeviceUnsupported
                ) {
                    InfoBanner(
                        message = stringResource(R.string.ai_consent_banner_message),
                        actions = listOf(
                            BannerAction(stringResource(R.string.action_review_agreement)) {
                                showConsentDialog = true
                            },
                        ),
                    )
                }
                if (uiState.memos.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.empty_ai_merge_title),
                        message = stringResource(R.string.empty_ai_merge_message),
                        iconRes = R.drawable.ic_auto_awesome,
                    )
                } else {
                    val gridState = rememberLazyGridState()
                    ScrollToTopOnNewFirstItem(
                        firstKey = uiState.memos.first().id.toString(),
                        isNearTop = { gridState.firstVisibleItemIndex <= 2 },
                        scrollToTop = { gridState.scrollToItem(0) },
                    )
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        // The bottom inset keeps the last row clear of the FAB.
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.memos, key = Memo::id) { memo ->
                            SelectableMemoCard(
                                memo = memo,
                                isSelected = memo.id in uiState.selectedIds,
                                onClick = { onToggleSelection(memo.id) },
                                onLongClick = { previewMemoId = memo.id },
                            )
                        }
                    }
                }
            }
        }
    }

    val previewMemo = loaded?.memos?.find { it.id == previewMemoId }
    if (previewMemo != null) {
        MemoPreviewSheet(memo = previewMemo, onDismiss = { previewMemoId = null })
    }

    if (showConsentDialog) {
        AiConsentDialog(
            onAccept = {
                showConsentDialog = false
                onAcceptAgreement()
            },
            onDismiss = { showConsentDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableMemoCard(
    memo: Memo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val longClickLabel = stringResource(R.string.action_preview)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CardDefaults.shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = longClickLabel,
                )
                .semantics { selected = isSelected },
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = memo.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 24.dp),
                )
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle_fill1),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
        }
    }
}

/** POP01 – 메모 미리보기. Read-only; the S03 selection is not affected. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoPreviewSheet(memo: Memo, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = memo.displayTitle(), style = MaterialTheme.typography.titleLarge)
            Text(text = memo.content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Memo.displayTitle(): String = title.ifEmpty { stringResource(R.string.memo_untitled) }
