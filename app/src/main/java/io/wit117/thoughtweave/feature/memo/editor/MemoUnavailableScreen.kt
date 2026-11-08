package io.wit117.thoughtweave.feature.memo.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.UnavailableScreen

/** Shown when S02-B's memo was deleted or could not be read. */
@Composable
fun MemoUnavailableScreen(onBack: () -> Unit) {
    UnavailableScreen(
        screenTitle = stringResource(R.string.title_memo),
        title = stringResource(R.string.memo_unavailable_title),
        message = stringResource(R.string.memo_unavailable_message),
        onBack = onBack,
    )
}
