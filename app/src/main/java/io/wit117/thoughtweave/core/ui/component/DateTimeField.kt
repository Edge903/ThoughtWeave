package io.wit117.thoughtweave.core.ui.component

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.domain.model.toAllDayDate
import io.wit117.thoughtweave.domain.model.toAllDayInstant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Locale-aware date, independent of the device time zone. */
fun formatLocalDate(context: Context, date: LocalDate): String = DateUtils.formatDateTime(
    context,
    date.toAllDayInstant().toEpochMilli(),
    DateUtils.FORMAT_UTC or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or
        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY,
)

/** Follows the device's 12/24-hour setting. */
fun formatLocalTime(context: Context, time: LocalTime): String = DateUtils.formatDateTime(
    context,
    LocalDate.of(2000, 1, 1).atTime(time).toInstant(ZoneOffset.UTC).toEpochMilli(),
    DateUtils.FORMAT_UTC or DateUtils.FORMAT_SHOW_TIME,
)

/** Labelled date button, optional time button and optional clear action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    date: LocalDate?,
    time: LocalTime,
    showTime: Boolean,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    errorText: String? = null,
) {
    val context = LocalContext.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(date?.let { formatLocalDate(context, it) } ?: stringResource(R.string.schedule_pick_date))
            }
            if (showTime && date != null) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(formatLocalTime(context, time))
                }
            }
            if (onClear != null && date != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.schedule_clear_date, label),
                    )
                }
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall.copy(lineBreak = WordAwareLineBreak),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDatePicker) {
        // DatePicker works in UTC midnights, the same representation as all-day values.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).toAllDayInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onDateChange(Instant.ofEpochMilli(it).toAllDayDate())
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) { DatePicker(state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(state.hour, state.minute))
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
