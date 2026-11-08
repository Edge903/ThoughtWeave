package io.wit117.thoughtweave.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.WordAwareLineBreak

/** POP04 – AI 기능 이용 동의. Acceptance requires ticking the checkbox on the loaded agreement. */
@Composable
fun AiConsentDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    var isChecked by rememberSaveable { mutableStateOf(false) }
    val textState by rememberLegalText(LegalDocument.AGREEMENT)
    val loaded = textState as? LegalTextState.Loaded

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_ai_consent)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (textState == LegalTextState.Failed) {
                        Text(stringResource(R.string.license_load_failed_title))
                    }
                    loaded?.paragraphs?.forEach { paragraph ->
                        Text(
                            text = paragraph.text,
                            style = MaterialTheme.typography.bodySmall.copy(lineBreak = WordAwareLineBreak),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isChecked,
                            enabled = loaded != null,
                            role = Role.Checkbox,
                            onValueChange = { isChecked = it },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = null, enabled = loaded != null)
                    Text(
                        text = stringResource(R.string.consent_checkbox),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept, enabled = isChecked && loaded != null) {
                Text(stringResource(R.string.action_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
