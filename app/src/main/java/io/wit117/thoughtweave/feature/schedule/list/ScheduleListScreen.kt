package io.wit117.thoughtweave.feature.schedule.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ContextFab
import io.wit117.thoughtweave.core.ui.component.EmptyState

/** S04 – 일정 확인. Phase 0: layout and FAB only; the date-grouped list arrives in Phase 4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onAddSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_schedules)) }) },
        floatingActionButton = {
            ContextFab(
                text = stringResource(R.string.fab_add_schedule),
                iconRes = R.drawable.ic_calendar_add_on,
                onClick = onAddSchedule,
            )
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.empty_schedules_title),
            message = stringResource(R.string.empty_schedules_message),
            iconRes = R.drawable.ic_event,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
