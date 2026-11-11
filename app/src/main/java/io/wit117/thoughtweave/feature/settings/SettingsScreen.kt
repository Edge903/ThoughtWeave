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
import io.wit117.thoughtweave.domain.model.InputLanguageSetting
import io.wit117.thoughtweave.domain.model.LanguageSettings
import io.wit117.thoughtweave.domain.model.OutputLanguageSetting
import io.wit117.thoughtweave.domain.repository.AiLanguageCatalog
import io.wit117.thoughtweave.domain.repository.LanguageSettingsRepository
import io.wit117.thoughtweave.domain.repository.UiLanguageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** S06 settings. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val uiLanguageRepository: UiLanguageRepository,
    private val languageSettingsRepository: LanguageSettingsRepository,
    catalog: AiLanguageCatalog,
) : ViewModel() {

    val inputLanguages: List<AppLanguage> = catalog.inputLanguages

    /** Only languages some verified translation can produce; empty hides the output setting. */
    val outputLanguages: List<AppLanguage> = catalog.translationTargets

    private val _uiLanguage = MutableStateFlow(uiLanguageRepository.current())

    /** Null means the app follows the system language. */
    val uiLanguage: StateFlow<AppLanguage?> = _uiLanguage.asStateFlow()

    private val _aiLanguages = MutableStateFlow(languageSettingsRepository.current())
    val aiLanguages: StateFlow<LanguageSettings> = _aiLanguages.asStateFlow()

    fun setUiLanguage(language: AppLanguage?) {
        if (language == _uiLanguage.value) return
        _uiLanguage.value = language
        uiLanguageRepository.set(language)
    }

    fun setInputLanguage(setting: InputLanguageSetting) {
        languageSettingsRepository.setInput(setting)
        _aiLanguages.value = languageSettingsRepository.current()
    }

    fun setOutputLanguage(setting: OutputLanguageSetting) {
        languageSettingsRepository.setOutput(setting)
        _aiLanguages.value = languageSettingsRepository.current()
    }
}

private enum class LanguageDialog { UI, INPUT, OUTPUT }

/** S06 – 설정. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiLanguage by viewModel.uiLanguage.collectAsStateWithLifecycle()
    val aiLanguages by viewModel.aiLanguages.collectAsStateWithLifecycle()
    var openDialog by rememberSaveable { mutableStateOf<LanguageDialog?>(null) }

    val systemLabel = stringResource(R.string.language_system)
    val autoLabel = stringResource(R.string.language_auto)
    val sameAsInputLabel = stringResource(R.string.language_same_as_input)
    val inputSetting = aiLanguages.input
    val outputSetting = aiLanguages.output

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
            SettingRow(
                title = stringResource(R.string.settings_ui_language),
                value = uiLanguage?.nativeName ?: systemLabel,
                onClick = { openDialog = LanguageDialog.UI },
            )
            SettingRow(
                title = stringResource(R.string.settings_ai_input_language),
                value = (inputSetting as? InputLanguageSetting.Specific)?.language?.nativeName ?: autoLabel,
                onClick = { openDialog = LanguageDialog.INPUT },
            )
            if (viewModel.outputLanguages.isNotEmpty()) {
                SettingRow(
                    title = stringResource(R.string.settings_ai_output_language),
                    value = (outputSetting as? OutputLanguageSetting.Specific)?.language?.nativeName
                        ?: sameAsInputLabel,
                    onClick = { openDialog = LanguageDialog.OUTPUT },
                )
            }
        }
    }

    when (openDialog) {
        LanguageDialog.UI -> ChoiceDialog(
            title = stringResource(R.string.settings_ui_language),
            options = listOf<AppLanguage?>(null) + AppLanguages.all,
            label = { it?.nativeName ?: systemLabel },
            selected = uiLanguage,
            onSelect = viewModel::setUiLanguage,
            onClose = { openDialog = null },
        )
        LanguageDialog.INPUT -> ChoiceDialog(
            title = stringResource(R.string.settings_ai_input_language),
            options = listOf(InputLanguageSetting.Auto) +
                viewModel.inputLanguages.map { InputLanguageSetting.Specific(it) },
            label = { (it as? InputLanguageSetting.Specific)?.language?.nativeName ?: autoLabel },
            selected = inputSetting,
            onSelect = viewModel::setInputLanguage,
            onClose = { openDialog = null },
        )
        LanguageDialog.OUTPUT -> ChoiceDialog(
            title = stringResource(R.string.settings_ai_output_language),
            options = listOf(OutputLanguageSetting.SameAsInput) +
                viewModel.outputLanguages.map { OutputLanguageSetting.Specific(it) },
            label = { (it as? OutputLanguageSetting.Specific)?.language?.nativeName ?: sameAsInputLabel },
            selected = outputSetting,
            onSelect = viewModel::setOutputLanguage,
            onClose = { openDialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = {
                                    onClose()
                                    onSelect(option)
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
