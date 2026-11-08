package io.wit117.thoughtweave.feature.memo.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.EmptyState

/** Shown when S02-B's memo was deleted or could not be read. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoUnavailableScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_memo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.memo_unavailable_title),
            message = stringResource(R.string.memo_unavailable_message),
            iconRes = R.drawable.ic_description,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
