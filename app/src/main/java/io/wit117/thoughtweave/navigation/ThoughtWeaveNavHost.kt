package io.wit117.thoughtweave.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.NotImplementedScreen
import io.wit117.thoughtweave.feature.legal.LicensesScreen
import io.wit117.thoughtweave.feature.memo.editor.MemoEditorScreen
import io.wit117.thoughtweave.feature.memo.editor.MemoEditorViewModel
import io.wit117.thoughtweave.feature.memo.editor.MemoUnavailableScreen
import io.wit117.thoughtweave.feature.memo.list.MemoListScreen
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorInit
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorScreen
import io.wit117.thoughtweave.feature.schedule.editor.ScheduleEditorViewModel
import io.wit117.thoughtweave.feature.schedule.list.ScheduleListScreen
import io.wit117.thoughtweave.feature.synthesis.SynthesisResultScreen
import io.wit117.thoughtweave.feature.synthesis.SynthesisResultViewModel
import io.wit117.thoughtweave.feature.synthesis.SynthesisScreen
import io.wit117.thoughtweave.feature.synthesis.SynthesisViewModel

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
                onOpenMemo = { navController.navigate(MemoEditorRoute(memoId = it)) },
                onOpenLicenses = { navController.navigate(LicensesRoute) },
            )
        }
        composable<SynthesisRoute> {
            SynthesisScreen(
                onSynthesize = { navController.navigate(SynthesisResultRoute(sourceIds = it)) },
            )
        }
        composable<SynthesisResultRoute> { entry ->
            val sourceIds = entry.toRoute<SynthesisResultRoute>().sourceIds
            // S02-C is only reachable from S03, so S03's entry is always below it on the stack.
            val synthesisEntry = remember(entry) { navController.getBackStackEntry<SynthesisRoute>() }
            val synthesisViewModel = hiltViewModel<SynthesisViewModel>(synthesisEntry)
            SynthesisResultScreen(
                viewModel = hiltViewModel<SynthesisResultViewModel, SynthesisResultViewModel.Factory>(
                    creationCallback = { it.create(sourceIds) },
                ),
                onBack = { navController.popBackStack() },
                onSaved = {
                    synthesisViewModel.clearSelection()
                    // Pop first so S02-C is not saved into the S03 tab's restored back stack.
                    navController.popBackStack()
                    navController.navigateToTopLevel(TopLevelDestination.MEMOS)
                },
            )
        }
        composable<ScheduleListRoute> {
            ScheduleListScreen(
                onAddSchedule = {
                    navController.navigate(ScheduleEditorRoute(mode = ScheduleEditorMode.MANUAL))
                },
                onOpenSchedule = {
                    navController.navigate(
                        ScheduleEditorRoute(mode = ScheduleEditorMode.EXISTING, scheduleId = it),
                    )
                },
            )
        }

        composable<MemoEditorRoute> { entry ->
            val memoId = entry.toRoute<MemoEditorRoute>().memoId
            MemoEditorScreen(
                viewModel = hiltViewModel<MemoEditorViewModel, MemoEditorViewModel.Factory>(
                    creationCallback = { it.create(memoId) },
                ),
                memoId = memoId,
                onBack = { navController.popBackStack() },
                onOpenExtraction = { id ->
                    navController.navigate(
                        ScheduleEditorRoute(mode = ScheduleEditorMode.AI_EXTRACTED, memoId = id),
                    )
                },
                onUnavailable = {
                    MemoUnavailableScreen(onBack = { navController.popBackStack() })
                },
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = { navController.popBackStack() })
        }

        composable<ScheduleEditorRoute> { entry ->
            val route = entry.toRoute<ScheduleEditorRoute>()
            val init = when (route.mode) {
                ScheduleEditorMode.MANUAL -> ScheduleEditorInit.Manual
                ScheduleEditorMode.EXISTING -> ScheduleEditorInit.Existing(checkNotNull(route.scheduleId))
                // Phase 0 scaffolding: replaced by AI extraction in Phase 5c.
                ScheduleEditorMode.AI_EXTRACTED -> null
            }
            if (init == null) {
                NotImplementedScreen(
                    title = stringResource(R.string.title_new_schedule),
                    onBack = { navController.popBackStack() },
                )
            } else {
                ScheduleEditorScreen(
                    viewModel = hiltViewModel<ScheduleEditorViewModel, ScheduleEditorViewModel.Factory>(
                        creationCallback = { it.create(init) },
                    ),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
