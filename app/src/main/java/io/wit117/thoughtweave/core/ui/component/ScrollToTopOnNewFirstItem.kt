package io.wit117.thoughtweave.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Lazy lists stay anchored on the current first item's key, so an item inserted above it would be
 * hidden just out of view. A user near the top is moved to the new first item; a user scrolled
 * further down keeps their place.
 */
@Composable
fun ScrollToTopOnNewFirstItem(
    firstKey: String?,
    isNearTop: () -> Boolean,
    scrollToTop: suspend () -> Unit,
) {
    var previousKey by rememberSaveable { mutableStateOf(firstKey) }
    LaunchedEffect(firstKey) {
        if (firstKey != previousKey) {
            previousKey = firstKey
            if (isNearTop()) scrollToTop()
        }
    }
}
