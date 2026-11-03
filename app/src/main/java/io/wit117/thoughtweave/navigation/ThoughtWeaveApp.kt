package io.wit117.thoughtweave.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Root composable. The outer Scaffold owns only the bottom navigation bar; each screen owns its
 * own inner Scaffold (top app bar + context FAB), so FAB logic lives next to the screen state.
 * The bar is shown only on the three top-level destinations (S01, S03, S04).
 */
@Composable
fun ThoughtWeaveApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isTopLevel(it) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ThoughtWeaveNavigationBar(
                    currentDestination = currentDestination,
                    onSelect = { navController.navigateToTopLevel(it) },
                )
            }
        },
        // The bottom bar and each screen's own Scaffold handle window insets.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        ThoughtWeaveNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun ThoughtWeaveNavigationBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination.isTopLevel(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (selected) destination.selectedIconRes else destination.iconRes,
                        ),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
