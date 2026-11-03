package io.wit117.thoughtweave.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Extended FAB used for each main screen's primary creation/AI action.
 *
 * Material 3's FloatingActionButton has no disabled state, but S03 requires one (fewer than two
 * memos selected), so this is built on [Surface] with explicit enabled/disabled styling.
 */
@Composable
fun ContextFab(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = FloatingActionButtonDefaults.extendedFabShape,
        color = if (enabled) colors.primaryContainer else colors.onSurface.copy(alpha = 0.12f),
        contentColor = if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = 0.38f),
        shadowElevation = if (enabled) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 80.dp, minHeight = 56.dp)
                .padding(start = 16.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
