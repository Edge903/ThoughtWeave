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

        // Phase 0 scaffolding: replaced by POP02/POP03 (Phase 4) and AI extraction (Phase 5c).
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
