package app.cybertracker.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.notifications.NotificationHelper
import app.cybertracker.ui.components.BackTopBar
import app.cybertracker.ui.components.CyberAlert
import app.cybertracker.ui.components.GhostButton
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.NeonButton
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    val context = LocalContext.current

    var notificationsOn by remember { mutableStateOf(NotificationHelper.areEnabled(context)) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notificationsOn = NotificationHelper.areEnabled(context) }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = NotificationHelper.areEnabled(context)
        // Denied permanently (or blocked by the system): the only way forward is the system settings.
        if (!granted) openNotificationSettings()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) vm.export(uri) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestore = uri }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { BackTopBar(title = "Settings & backup", onBack = onBack) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(Space.m),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader("NOTIFICATIONS", accent = c.pink)
                GlassCard(modifier = Modifier.fillMaxWidth(), accent = if (notificationsOn) null else c.pink) {
                    Text(
                        if (notificationsOn) "Reminders are enabled" else "Reminders are disabled",
                        color = if (notificationsOn) c.ok else c.pink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (notificationsOn) {
                            "You'll be notified 7 days before and on the day a date expires, at 9:00 AM."
                        } else {
                            "Notifications are off, so expiry reminders won't appear. The app still works normally."
                        },
                        color = c.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.xs),
                    )
                    Text(
                        "Android may delay reminders by a few minutes, especially in battery saver or Doze mode.",
                        color = c.textDim,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Space.s),
                    )
                    if (!notificationsOn) {
                        Spacer(Modifier.height(Space.m))
                        GhostButton(
                            text = "ENABLE NOTIFICATIONS",
                            icon = Icons.Default.Notifications,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33 && !NotificationHelper.hasPermission(context)) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openNotificationSettings()
                                }
                            },
                        )
                    }
                }

                SectionHeader("BACKUP", accent = c.cyan)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Save every entry, date and remark to a JSON file you choose, or restore from one. " +
                            "Restoring replaces all current data.",
                        color = c.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(Space.m))
                    NeonButton(
                        text = "EXPORT BACKUP",
                        enabled = !state.busy,
                        onClick = { exportLauncher.launch("cyber-dates-backup-${LocalDate.now()}.json") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Space.s))
                    GhostButton(
                        text = "RESTORE BACKUP",
                        onClick = { if (!state.busy) restoreLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val message = state.message
                if (message != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), accent = if (state.isError) c.danger else c.ok) {
                        Text(
                            message,
                            color = if (state.isError) c.danger else c.ok,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(Space.s))
                        GhostButton("DISMISS", onClick = vm::dismissMessage)
                    }
                }

                SectionHeader("PRIVACY", accent = c.purple)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "All data is stored locally on this device. There are no accounts, no cloud sync, no analytics " +
                            "and the app never uses the network.",
                        color = c.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    pendingRestore?.let { uri ->
        CyberAlert(
            title = "Replace all data?",
            text = "Restoring will replace everything currently stored on this device with the contents of the backup file. This cannot be undone.",
            confirmText = "RESTORE",
            dismissText = "CANCEL",
            danger = true,
            onConfirm = {
                pendingRestore = null
                vm.restore(uri)
            },
            onDismiss = { pendingRestore = null },
        )
    }
}
