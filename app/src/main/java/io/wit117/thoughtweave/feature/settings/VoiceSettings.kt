package io.wit117.thoughtweave.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.domain.model.AppLanguage
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.SpeechDownloadEvent
import io.wit117.thoughtweave.domain.model.SpeechLanguageState
import io.wit117.thoughtweave.domain.repository.SpeechInputRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoiceSupport {
    /** Below Android 12 or no on-device recognizer: voice input is unavailable. */
    UNSUPPORTED,

    /** Android 12: voice input works, but language states cannot be read. */
    BASIC,

    /** Android 13+: language states and downloads. */
    MANAGED,
}

data class VoiceLanguageRow(
    val language: AppLanguage,
    /** Null until checked, or when the check failed. */
    val state: SpeechLanguageState? = null,
    /** The latest event of a download requested on this screen. */
    val download: SpeechDownloadEvent? = null,
)

data class VoiceSettingsUiState(
    val support: VoiceSupport,
    val rows: List<VoiceLanguageRow> = emptyList(),
    val checkFailed: Boolean = false,
)

/** What a language row says, decided from its state and any download in progress. */
sealed interface VoiceRowLabel {
    data object Checking : VoiceRowLabel
    data object Installed : VoiceRowLabel
    data class Downloading(val percent: Int?) : VoiceRowLabel
    data object NotInstalled : VoiceRowLabel
    data object Unsupported : VoiceRowLabel
    data object Requested : VoiceRowLabel
    data object Scheduled : VoiceRowLabel
    data object Failed : VoiceRowLabel
}

fun VoiceLanguageRow.label(): VoiceRowLabel {
    if (state == SpeechLanguageState.INSTALLED) return VoiceRowLabel.Installed
    return when (download) {
        is SpeechDownloadEvent.Progress -> VoiceRowLabel.Downloading(download.percent)
        SpeechDownloadEvent.Requested -> VoiceRowLabel.Requested
        SpeechDownloadEvent.Scheduled -> VoiceRowLabel.Scheduled
        SpeechDownloadEvent.Failed -> VoiceRowLabel.Failed
        SpeechDownloadEvent.Done, null -> when (state) {
            null -> VoiceRowLabel.Checking
            SpeechLanguageState.INSTALLED -> VoiceRowLabel.Installed
            SpeechLanguageState.DOWNLOADING -> VoiceRowLabel.Downloading(null)
            SpeechLanguageState.DOWNLOADABLE -> VoiceRowLabel.NotInstalled
            SpeechLanguageState.UNSUPPORTED -> VoiceRowLabel.Unsupported
        }
    }
}

/** A download can be (re)requested when the language is downloadable and nothing is under way. */
val VoiceLanguageRow.canDownload: Boolean
    get() = state == SpeechLanguageState.DOWNLOADABLE &&
        (download == null || download == SpeechDownloadEvent.Failed)

/** S06 voice input section. */
@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val speech: SpeechInputRepository,
) : ViewModel() {

    private val support = when {
        !speech.isSupported() -> VoiceSupport.UNSUPPORTED
        speech.canManageLanguages() -> VoiceSupport.MANAGED
        else -> VoiceSupport.BASIC
    }

    private val _uiState = MutableStateFlow(
        VoiceSettingsUiState(
            support = support,
            rows = if (support == VoiceSupport.MANAGED) AppLanguages.all.map(::VoiceLanguageRow) else emptyList(),
        ),
    )
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()

    private val downloads = mutableMapOf<AppLanguage, Job>()

    /** Called whenever the screen resumes: downloads finish, or are approved, outside the app. */
    fun refresh() {
        if (support != VoiceSupport.MANAGED) return
        viewModelScope.launch {
            val states = speech.languageStates(AppLanguages.all.map { it.speechTag })
            _uiState.update { current ->
                current.copy(
                    checkFailed = states == null,
                    rows = current.rows.map { it.copy(state = states?.get(it.language.speechTag)) },
                )
            }
        }
    }

    fun download(language: AppLanguage) {
        if (support != VoiceSupport.MANAGED || downloads[language]?.isActive == true) return
        downloads[language] = viewModelScope.launch {
            speech.download(language.speechTag).collect { event ->
                updateRow(language) { it.copy(download = event) }
                if (event == SpeechDownloadEvent.Done) refresh()
            }
        }
    }

    private fun updateRow(language: AppLanguage, transform: (VoiceLanguageRow) -> VoiceLanguageRow) =
        _uiState.update { current ->
            current.copy(rows = current.rows.map { if (it.language == language) transform(it) else it })
        }
}

@Composable
fun VoiceInputSection(modifier: Modifier = Modifier, viewModel: VoiceSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(modifier) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_voice_input),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        val note = when {
            state.support == VoiceSupport.UNSUPPORTED -> R.string.settings_voice_unsupported
            else -> R.string.settings_voice_description
        }
        Text(
            text = stringResource(note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when {
            state.support == VoiceSupport.BASIC -> StatusNote(R.string.settings_voice_status_unknown)
            state.checkFailed -> StatusNote(R.string.settings_voice_status_failed)
        }
        state.rows.forEach { row ->
            ListItem(
                headlineContent = { Text(row.language.nativeName) },
                supportingContent = { Text(labelText(row.label())) },
                trailingContent = if (row.canDownload) {
                    {
                        TextButton(onClick = { viewModel.download(row.language) }) {
                            Text(stringResource(R.string.action_download))
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun StatusNote(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun labelText(label: VoiceRowLabel): String = when (label) {
    VoiceRowLabel.Checking -> stringResource(R.string.voice_language_checking)
    VoiceRowLabel.Installed -> stringResource(R.string.voice_language_installed)
    is VoiceRowLabel.Downloading -> label.percent
        ?.let { stringResource(R.string.voice_language_downloading_percent, it) }
        ?: stringResource(R.string.voice_language_downloading)
    VoiceRowLabel.NotInstalled -> stringResource(R.string.voice_language_downloadable)
    VoiceRowLabel.Unsupported -> stringResource(R.string.voice_language_unsupported)
    VoiceRowLabel.Requested -> stringResource(R.string.voice_language_requested)
    VoiceRowLabel.Scheduled -> stringResource(R.string.voice_language_scheduled)
    VoiceRowLabel.Failed -> stringResource(R.string.voice_language_failed)
}
