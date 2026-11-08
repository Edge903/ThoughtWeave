package io.wit117.thoughtweave.feature.legal

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.EmptyState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface LegalTextState {
    data object Loading : LegalTextState
    data class Loaded(val paragraphs: List<LegalParagraph>) : LegalTextState
    data object Failed : LegalTextState
}

/** S05 – 라이선스 및 고지. The user agreement and consent withdrawal are added in Phase 5a. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<LegalDocument?>(null) }
    val document = selected

    BackHandler(enabled = document != null) { selected = null }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(document?.titleRes ?: R.string.title_licenses))
                },
                navigationIcon = {
                    IconButton(onClick = { if (document != null) selected = null else onBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val bodyModifier = Modifier.padding(innerPadding).fillMaxSize()
        if (document == null) {
            LazyColumn(bodyModifier) {
                items(LegalDocument.entries) { entry ->
                    ListItem(
                        headlineContent = { Text(stringResource(entry.titleRes)) },
                        modifier = Modifier.clickable { selected = entry },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            LegalDocumentBody(document, bodyModifier)
        }
    }
}

@Composable
private fun LegalDocumentBody(document: LegalDocument, modifier: Modifier) {
    val context = LocalContext.current
    val state by produceState<LegalTextState>(LegalTextState.Loading, document) {
        value = withContext(Dispatchers.IO) {
            try {
                LegalTextState.Loaded(loadParagraphs(context, document))
            } catch (_: IOException) {
                LegalTextState.Failed
            }
        }
    }
    when (val current = state) {
        LegalTextState.Loading -> Box(modifier, Alignment.Center) { CircularProgressIndicator() }
        LegalTextState.Failed -> EmptyState(
            title = stringResource(R.string.license_load_failed_title),
            message = stringResource(R.string.license_load_failed_message),
            iconRes = R.drawable.ic_description,
            modifier = modifier,
        )
        is LegalTextState.Loaded -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(current.paragraphs) { paragraph ->
                Text(
                    text = paragraph.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp * paragraph.indentLevel),
                )
            }
        }
    }
}

private fun loadParagraphs(context: Context, document: LegalDocument): List<LegalParagraph> =
    document.assets.flatMap { asset ->
        val text = context.assets.open(asset).bufferedReader().use { it.readText() }
        parseLegalParagraphs(text)
    }
