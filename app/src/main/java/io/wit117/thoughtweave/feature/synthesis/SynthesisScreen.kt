package io.wit117.thoughtweave.feature.synthesis

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ContextFab
import io.wit117.thoughtweave.core.ui.component.EmptyState

/** Synthesis needs at least this many selected memos (S03 FAB rule). */
const val MIN_MEMOS_FOR_SYNTHESIS = 2

/**
 * S03 – AI 메모 통합. Phase 0: layout and the FAB enablement rule. The grid, selection state and
 * AI availability gate arrive in Phases 3 and 5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesisScreen(
    selectedCount: Int,
    onSynthesize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_ai_merge)) }) },
        floatingActionButton = {
            ContextFab(
                text = stringResource(R.string.fab_ai_synthesize),
                iconRes = R.drawable.ic_auto_awesome,
                onClick = onSynthesize,
                enabled = selectedCount >= MIN_MEMOS_FOR_SYNTHESIS,
            )
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.empty_ai_merge_title),
            message = stringResource(R.string.empty_ai_merge_message),
            iconRes = R.drawable.ic_auto_awesome,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
