package io.wit117.thoughtweave.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.NotImplementedScreen
import io.wit117.thoughtweave.feature.memo.list.MemoListScreen
import io.wit117.thoughtweave.feature.schedule.list.ScheduleListScreen
import io.wit117.thoughtweave.feature.synthesis.SynthesisScreen

@Composable
fun ThoughtWeaveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MemoListRoute,
        modifier = modifier,
    ) {
        composable<MemoListRoute> {
            MemoListScreen(
                onCreateMemo = { navController.navigate(MemoEditorRoute(memoId = null)) },
            )
        }
        composable<SynthesisRoute> {
            // Selection state arrives in Phase 3; with nothing selected the FAB stays disabled.
            SynthesisScreen(selectedCount = 0, onSynthesize = {})
        }
        composable<ScheduleListRoute> {
            ScheduleListScreen(
                onAddSchedule = {
                    navController.navigate(ScheduleEditorRoute(mode = ScheduleEditorMode.MANUAL))
                },
            )
        }

        // Phase 0 scaffolding: replaced by S02-A/B (Phase 2) and POP02/POP03 (Phase 4).
        composable<MemoEditorRoute> {
            NotImplementedScreen(
                title = stringResource(R.string.title_new_memo),
                onBack = { navController.popBackStack() },
            )
        }
        composable<ScheduleEditorRoute> { entry ->
            val route = entry.toRoute<ScheduleEditorRoute>()
            val titleRes = if (route.mode == ScheduleEditorMode.EXISTING) {
                R.string.title_schedule
            } else {
                R.string.title_new_schedule
            }
            NotImplementedScreen(
                title = stringResource(titleRes),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
