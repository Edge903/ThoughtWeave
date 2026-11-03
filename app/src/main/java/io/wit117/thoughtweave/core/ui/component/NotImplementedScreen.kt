package io.wit117.thoughtweave.core.ui.component

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

/**
 * Phase 0 scaffolding for destinations that exist in the navigation graph but whose content
 * belongs to a later phase. It states this explicitly instead of pretending to work.
 * Every usage is replaced when its phase is implemented; none may remain at release.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotImplementedScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            title = stringResource(R.string.not_implemented_title),
            message = stringResource(R.string.not_implemented_message),
            iconRes = R.drawable.ic_description,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
