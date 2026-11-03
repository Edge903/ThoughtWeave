package io.wit117.thoughtweave.feature.memo.list

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

/** S01 – 메모 목록. Phase 0: layout and FAB only; the memo list arrives in Phase 2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    onCreateMemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_memo_list)) }) },
        floatingActionButton = {
            ContextFab(
                text = stringResource(R.string.fab_new_memo),
                iconRes = R.drawable.ic_edit_square,
                onClick = onCreateMemo,
            )
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.empty_memos_title),
            message = stringResource(R.string.empty_memos_message),
            iconRes = R.drawable.ic_description,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
