package app.cybertracker.ui.reminders

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.CyberApp
import app.cybertracker.notifications.NotificationHelper
import app.cybertracker.ui.components.BackTopBar
import app.cybertracker.ui.components.CyberAlert
import app.cybertracker.ui.components.CyberDatePickerDialog
import app.cybertracker.ui.components.CyberTextField
import app.cybertracker.ui.components.CyberTimePickerDialog
import app.cybertracker.ui.components.GhostButton
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.NeonButton
import app.cybertracker.ui.prettyDateLong
import app.cybertracker.ui.prettyTime
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space

private const val PREF_ASKED_NOTIF = "asked_notification_permission"

@Composable
fun ReminderEditorScreen(vm: ReminderEditorViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as CyberApp).container.prefs }

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    LaunchedEffect(state.missing) {
        if (state.missing) onClose()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.save(onClose)
    }

    fun onSaveClicked() {
        val needsAsk = !state.isInPast &&
            !NotificationHelper.hasPermission(context) &&
            !prefs.getBoolean(PREF_ASKED_NOTIF, false)
        if (needsAsk) showRationale = true else vm.save(onClose)
    }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(title = if (state.isNew) "New reminder" else "Edit reminder", onBack = onClose) {
                    if (!state.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = c.danger)
                        }
                    }
                }
            },
            bottomBar = {
                NeonButton(
                    text = if (state.saving) "SAVING…" else "SAVE REMINDER",
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
                item {
                    CyberTextField(
                        value = state.title,
                        onValueChange = vm::onTitle,
                        label = "What to remind you about",
                        isError = state.titleError,
                        errorText = "A title is required",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CyberTextField(
                        value = state.note,
                        onValueChange = vm::onNote,
                        label = "Note (optional)",
                        singleLine = false,
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        GhostButton(
                            text = prettyDateLong(state.date),
                            icon = Icons.Default.DateRange,
                            onClick = { showDate = true },
                            modifier = Modifier.weight(1f),
                        )
                        GhostButton(
                            text = prettyTime(state.time),
                            icon = Icons.Default.Notifications,
                            onClick = { showTime = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text(
                        if (state.isInPast) {
                            "This moment has already passed, so no notification will be sent."
                        } else {
                            "You'll get a notification at this exact time (Android may delay it by a few minutes)."
                        },
                        color = if (state.isInPast) c.pink else c.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.error != null) {
                    item { Text(state.error.orEmpty(), color = c.danger, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }

    if (showDate) {
        CyberDatePickerDialog(initial = state.date, onPick = vm::onDate, onDismiss = { showDate = false })
    }
    if (showTime) {
        CyberTimePickerDialog(initial = state.time, onPick = vm::onTime, onDismiss = { showTime = false })
    }
    if (showDelete) {
        CyberAlert(
            title = "Delete this reminder?",
            text = "\"${state.title}\" and its notification will be removed.",
            confirmText = "DELETE",
            dismissText = "CANCEL",
            danger = true,
            onConfirm = {
                showDelete = false
                vm.delete(onClose)
            },
            onDismiss = { showDelete = false },
        )
    }
    if (showRationale) {
        CyberAlert(
            title = "Allow notifications?",
            text = "To remind you at this time the app needs permission to show notifications. " +
                "Everything stays on this device. You can skip this and still use the app.",
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
