package app.cybertracker.ui.editor

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.CyberApp
import app.cybertracker.data.entity.DateType
import app.cybertracker.notifications.NotificationHelper
import app.cybertracker.ui.components.BackTopBar
import app.cybertracker.ui.components.CyberAlert
import app.cybertracker.ui.components.CyberTextField
import app.cybertracker.ui.components.GhostButton
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.NeonButton
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.prettyDate
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val PREF_ASKED_NOTIF = "asked_notification_permission"

@Composable
fun EditorScreen(vm: EditorViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as CyberApp).container.prefs }

    var showDiscard by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Whatever the answer, the entry is saved; the app works without notification access.
        vm.save(onClose)
    }

    fun requestClose() {
        if (state.dirty) showDiscard = true else onClose()
    }

    fun onSaveClicked() {
        val needsAsk = state.wantsNotifications &&
            !NotificationHelper.hasPermission(context) &&
            !prefs.getBoolean(PREF_ASKED_NOTIF, false)
        if (needsAsk) showRationale = true else vm.save(onClose)
    }

    BackHandler { requestClose() }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = if (state.isNew) "New entry" else "Edit entry",
                    onBack = ::requestClose,
                )
            },
            bottomBar = {
                NeonButton(
                    text = if (state.saving) "SAVING…" else "SAVE ENTRY",
                    icon = Icons.Default.Check,
                    enabled = !state.saving && !state.loading,
                    onClick = ::onSaveClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(Space.m),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.m, end = Space.m,
                    top = padding.calculateTopPadding() + Space.s,
                    bottom = padding.calculateBottomPadding() + Space.m,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "title") {
                    CyberTextField(
                        value = state.title,
                        onValueChange = vm::onTitle,
                        label = "Title",
                        isError = state.titleError,
                        errorText = "A title is required",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "remarks") {
                    CyberTextField(
                        value = state.remarks,
                        onValueChange = vm::onRemarks,
                        label = "Remarks (optional)",
                        singleLine = false,
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "dates-header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader("DATES", state.rows.size, c.purple)
                        GhostButton("ADD DATE", onClick = vm::addRow, icon = Icons.Default.Add)
                    }
                }
                if (state.rows.isEmpty()) {
                    item(key = "dates-empty") {
                        Text(
                            "No dates yet. Add any date you like, such as \"Bought on\", \"Last washed\" or \"Expires on\".",
                            color = c.textDim,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(state.rows, key = { it.id }) { row ->
                    DateRowCard(
                        row = row,
                        onLabel = { vm.onLabel(row.id, it) },
                        onDate = { vm.onDate(row.id, it) },
                        onType = { vm.onType(row.id, it) },
                        onNotify = { vm.onNotify(row.id, it) },
                        onRemove = { vm.removeRow(row.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (state.error != null) {
                    item(key = "error") { Text(state.error.orEmpty(), color = c.danger, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }

    if (showDiscard) {
        CyberAlert(
            title = "Discard changes?",
            text = "Your unsaved changes to this entry will be lost.",
            confirmText = "DISCARD",
            dismissText = "KEEP EDITING",
            danger = true,
            onConfirm = {
                showDiscard = false
                vm.discardDraft()
                onClose()
            },
            onDismiss = { showDiscard = false },
        )
    }
    if (showRationale) {
        CyberAlert(
            title = "Get expiry reminders?",
            text = "This entry has a reminder date. Allow notifications and the app will remind you 7 days " +
                "before and on the day it expires. Everything stays on this device. You can skip this and still use the app.",
            confirmText = "CONTINUE",
            dismissText = "NOT NOW",
            onConfirm = {
                showRationale = false
                prefs.edit().putBoolean(PREF_ASKED_NOTIF, true).apply()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showRationale = false
                prefs.edit().putBoolean(PREF_ASKED_NOTIF, true).apply()
                vm.save(onClose)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DateRowCard(
    row: DateRowState,
    onLabel: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onType: (DateType) -> Unit,
    onNotify: (Boolean) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Cyber.colors
    var showPicker by remember { mutableStateOf(false) }

    GlassCard(modifier = modifier.fillMaxWidth(), accent = if (row.type == DateType.EXPIRY) c.pink else null) {
        Row(verticalAlignment = Alignment.Top) {
            CyberTextField(
                value = row.label,
                onValueChange = onLabel,
                label = "Date label (e.g. Expires on)",
                isError = row.labelError,
                errorText = "A label is required",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove date row", tint = c.danger)
            }
        }
        Spacer(Modifier.height(Space.s))
        GhostButton(
            text = prettyDate(row.date),
            icon = Icons.Default.DateRange,
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chipColors = FilterChipDefaults.filterChipColors(
                labelColor = c.textDim,
                selectedContainerColor = c.cyan.copy(alpha = 0.2f),
                selectedLabelColor = c.cyan,
            )
            FilterChip(
                selected = row.type == DateType.ORDINARY,
                onClick = { onType(DateType.ORDINARY) },
                label = { Text("Ordinary date") },
                colors = chipColors,
            )
            FilterChip(
                selected = row.type == DateType.EXPIRY,
                onClick = { onType(DateType.EXPIRY) },
                label = { Text("Expiry / reminder") },
                colors = chipColors,
            )
        }
        AnimatedVisibility(visible = row.type == DateType.EXPIRY) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Notify 7 days before and on the day",
                    color = c.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = row.notify,
                    onCheckedChange = onNotify,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onAccent,
                        checkedTrackColor = c.cyan,
                        uncheckedThumbColor = c.textDim,
                        uncheckedTrackColor = c.surface,
                        uncheckedBorderColor = c.border,
                    ),
                )
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toUtcMillis(row.date))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDate(fromUtcMillis(it)) }
                    showPicker = false
                }) { Text("OK", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("CANCEL", color = c.textDim) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// The Material date picker speaks "UTC midnight milliseconds"; convert strictly at that boundary.
private fun toUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun fromUtcMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
