package io.wit117.thoughtweave.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class BannerAction(val text: String, val onClick: () -> Unit)

/** Full-width status message with optional progress and actions. */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    actions: List<BannerAction> = emptyList(),
    extraAction: @Composable () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(lineBreak = WordAwareLineBreak),
            )
            if (progress != null) LinearProgressIndicator({ progress }, Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                extraAction()
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) { Text(action.text) }
                }
            }
        }
    }
}
