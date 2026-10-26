package app.cybertracker.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.CyberShapes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberDatePickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = Cyber.colors
    // The Material date picker speaks "UTC midnight milliseconds"; convert strictly at that boundary.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = c.cyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = c.textDim) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTimePickerDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val c = Cyber.colors
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24)
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = CyberShapes.card, color = c.surfaceHigh) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Select time", color = c.text)
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = c.textDim) }
                    TextButton(onClick = {
                        onPick(LocalTime.of(state.hour, state.minute))
                        onDismiss()
                    }) { Text("OK", color = c.cyan) }
                }
            }
        }
    }
}

/** Single-line text prompt used to name a new shopping list or rename one. */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Cyber.colors
    var text by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CyberShapes.card,
        containerColor = c.surfaceHigh,
        titleContentColor = c.text,
        title = { Text(title) },
        text = {
            CyberTextField(
                value = text,
                onValueChange = { text = it.take(100) },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text.trim()) },
            ) { Text(confirmText, color = if (text.isNotBlank()) c.cyan else c.textDim) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = c.textDim) } },
    )
}
