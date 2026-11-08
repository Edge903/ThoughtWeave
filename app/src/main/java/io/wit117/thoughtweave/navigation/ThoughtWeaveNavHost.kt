package io.wit117.thoughtweave.navigation

import androidx.compose.runtime.Composable
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
                onOpenMemo = { navController.navigate(MemoEditorRoute(memoId = it)) },
                onOpenLicenses = { navController.navigate(LicensesRoute) },
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
