package io.wit117.thoughtweave.feature.schedule.list

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.core.ui.component.ContextFab
import io.wit117.thoughtweave.core.ui.component.EmptyState
import io.wit117.thoughtweave.core.ui.component.ScrollToTopOnNewFirstItem
import io.wit117.thoughtweave.core.ui.component.formatLocalDate
import io.wit117.thoughtweave.domain.model.Schedule
import io.wit117.thoughtweave.domain.model.toAllDayDate
import java.time.Instant

/** S04 – 일정 확인. */
@Composable
fun ScheduleListScreen(
    onAddSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleListContent(uiState, onAddSchedule, onOpenSchedule, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListContent(
    uiState: ScheduleListUiState,
    onAddSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
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
        val bodyModifier = Modifier.padding(innerPadding)
        when (uiState) {
            ScheduleListUiState.Loading -> Box(bodyModifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            ScheduleListUiState.Failed -> EmptyState(
                title = stringResource(R.string.schedule_list_error_title),
                message = stringResource(R.string.memo_list_error_message),
                iconRes = R.drawable.ic_event,
                modifier = bodyModifier,
            )

            is ScheduleListUiState.Loaded -> if (uiState.groups.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_schedules_title),
                    message = stringResource(R.string.empty_schedules_message),
                    iconRes = R.drawable.ic_event,
                    modifier = bodyModifier,
                )
            } else {
                ScheduleGroups(uiState.groups, onOpenSchedule, bodyModifier)
            }
        }
    }
}

@Composable
private fun ScheduleGroups(
    groups: List<ScheduleGroup>,
    onOpenSchedule: (Long) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    ScrollToTopOnNewFirstItem(
        firstKey = groups.first().let { "${it.date}-${it.schedules.first().id}" },
        isNearTop = { listState.firstVisibleItemIndex <= 2 },
        scrollToTop = { listState.scrollToItem(0) },
    )
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // The bottom inset keeps the last card clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "date-${group.date}") {
                Text(
                    text = formatLocalDate(context, group.date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(group.schedules, key = Schedule::id) { schedule ->
                ScheduleCard(schedule, onClick = { onOpenSchedule(schedule.id) })
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = schedule.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = scheduleTimeText(schedule),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (schedule.description.isNotBlank()) {
                Text(
                    text = schedule.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun scheduleTimeText(schedule: Schedule): String {
    val context = LocalContext.current
    val start = schedule.startAt
    return when {
        schedule.isAllDay && start == null -> stringResource(R.string.schedule_all_day)
        schedule.isAllDay && start != null -> stringResource(
            R.string.schedule_all_day_from,
            formatLocalDate(context, start.toAllDayDate()),
        )
        start == null -> stringResource(R.string.schedule_due_at, formatTime(context, schedule.endAt))
        else -> stringResource(
            R.string.schedule_time_range,
            formatDateAndTime(context, start),
            formatTime(context, schedule.endAt),
        )
    }
}

private fun formatTime(context: Context, instant: Instant): String =
    DateUtils.formatDateTime(context, instant.toEpochMilli(), DateUtils.FORMAT_SHOW_TIME)

private fun formatDateAndTime(context: Context, instant: Instant): String =
    DateUtils.formatDateTime(
        context,
        instant.toEpochMilli(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
