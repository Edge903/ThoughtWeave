package io.wit117.thoughtweave.feature.memo.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ContextFab
import io.wit117.thoughtweave.core.ui.component.EmptyState
import io.wit117.thoughtweave.core.ui.component.MemoCard
import io.wit117.thoughtweave.core.ui.component.ScrollToTopOnNewFirstItem
import io.wit117.thoughtweave.domain.model.Memo

/** S01 – 메모 목록. */
@Composable
fun MemoListScreen(
    onCreateMemo: () -> Unit,
    onOpenMemo: (Long) -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pinFailedMessage = stringResource(R.string.memo_pin_failed)

    LaunchedEffect(viewModel) {
        viewModel.pinFailures.collect { snackbarHostState.showSnackbar(pinFailedMessage) }
    }

    MemoListContent(
        uiState = uiState,
        searchQuery = searchQuery,
        snackbarHostState = snackbarHostState,
        onCreateMemo = onCreateMemo,
        onOpenMemo = onOpenMemo,
        onOpenLicenses = onOpenLicenses,
        onOpenSettings = onOpenSettings,
        onTogglePin = viewModel::togglePin,
        onOpenSearch = viewModel::openSearch,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCloseSearch = viewModel::closeSearch,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListContent(
    uiState: MemoListUiState,
    searchQuery: String?,
    snackbarHostState: SnackbarHostState,
    onCreateMemo: () -> Unit,
    onOpenMemo: (Long) -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePin: (Memo) -> Unit,
    onOpenSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = searchQuery != null, onBack = onCloseSearch)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (searchQuery == null) {
                        Text(stringResource(R.string.title_memo_list))
                    } else {
                        MemoSearchField(query = searchQuery, onQueryChange = onSearchQueryChange)
                    }
                },
                actions = {
                    if (searchQuery == null) {
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                        OverflowMenu(onOpenSettings = onOpenSettings, onOpenLicenses = onOpenLicenses)
                    } else {
                        IconButton(onClick = onCloseSearch) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.action_close_search),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ContextFab(
                text = stringResource(R.string.fab_new_memo),
                iconRes = R.drawable.ic_edit_square,
                onClick = onCreateMemo,
            )
        },
    ) { innerPadding ->
        val bodyModifier = Modifier.padding(innerPadding)
        when (uiState) {
            MemoListUiState.Loading -> Box(bodyModifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            MemoListUiState.Failed -> EmptyState(
                title = stringResource(R.string.memo_list_error_title),
                message = stringResource(R.string.memo_list_error_message),
                iconRes = R.drawable.ic_description,
                modifier = bodyModifier,
            )

            is MemoListUiState.Loaded -> if (uiState.memos.isEmpty()) {
                val isSearching = !searchQuery.isNullOrBlank()
                EmptyState(
                    title = stringResource(
                        if (isSearching) R.string.empty_search_title else R.string.empty_memos_title,
                    ),
                    message = stringResource(
                        if (isSearching) R.string.empty_search_message else R.string.empty_memos_message,
                    ),
                    iconRes = if (isSearching) R.drawable.ic_search else R.drawable.ic_description,
                    modifier = bodyModifier,
                )
            } else {
                val listState = rememberLazyListState()
                ScrollToTopOnNewFirstItem(
                    firstKey = uiState.memos.first().id.toString(),
                    isNearTop = { listState.firstVisibleItemIndex <= 1 },
                    scrollToTop = { listState.scrollToItem(0) },
                )
                LazyColumn(
                    modifier = bodyModifier.fillMaxSize(),
                    state = listState,
                    // The bottom inset keeps the last card clear of the FAB.
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.memos, key = Memo::id) { memo ->
                        MemoCard(
                            memo = memo,
                            onClick = { onOpenMemo(memo.id) },
                            onTogglePin = { onTogglePin(memo) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoSearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.memo_search_hint)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun OverflowMenu(onOpenSettings: () -> Unit, onOpenLicenses: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.action_more),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.title_settings)) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.title_licenses)) },
                onClick = {
                    expanded = false
                    onOpenLicenses()
                },
            )
        }
    }
}
