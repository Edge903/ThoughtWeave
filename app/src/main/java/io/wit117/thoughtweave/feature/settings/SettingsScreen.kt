package io.wit117.thoughtweave.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.repository.UiLanguageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** S06 settings. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val uiLanguageRepository: UiLanguageRepository,
) : ViewModel() {

    private val _uiLanguage = MutableStateFlow(uiLanguageRepository.current())

    /** Null means the app follows the system language. */
    val uiLanguage: StateFlow<AppLanguage?> = _uiLanguage.asStateFlow()

    fun setUiLanguage(language: AppLanguage?) {
        if (language == _uiLanguage.value) return
        _uiLanguage.value = language
        uiLanguageRepository.set(language)
    }
}

/** S06 – 설정. Input and output language settings for AI arrive with language profiles (L2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiLanguage by viewModel.uiLanguage.collectAsStateWithLifecycle()
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
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
        Column(Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ui_language)) },
                supportingContent = { Text(uiLanguage?.nativeName ?: stringResource(R.string.language_system)) },
                modifier = Modifier.clickable { showLanguageDialog = true },
            )
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selected = uiLanguage,
            onSelect = {
                showLanguageDialog = false
                viewModel.setUiLanguage(it)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun LanguageDialog(
    selected: AppLanguage?,
    onSelect: (AppLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options: List<Pair<AppLanguage?, String>> =
        listOf(null to stringResource(R.string.language_system)) + AppLanguages.all.map { it to it.nativeName }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_ui_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (language, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == selected,
                                onClick = { onSelect(language) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
