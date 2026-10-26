package app.cybertracker.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.domain.ExpiryStatus
import app.cybertracker.domain.daysUntil
import app.cybertracker.domain.expiryStatus
import app.cybertracker.ui.components.BackTopBar
import app.cybertracker.ui.components.CyberAlert
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.components.StatusBadge
import app.cybertracker.ui.daysText
import app.cybertracker.ui.prettyDate
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space
import java.time.LocalDate

@Composable
fun DetailsScreen(
    vm: DetailsViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    var confirmDelete by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }

    // The entry may have been deleted (here or via restore); leave the screen gracefully.
    LaunchedEffect(state) {
        if (state is DetailsState.NotFound) onBack()
    }

    val loaded = state as? DetailsState.Loaded

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(title = "Entry details", onBack = onBack) {
                    if (loaded != null) {
                        IconButton(onClick = { onEdit(loaded.item.entry.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit entry", tint = c.cyan)
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry", tint = c.danger)
                        }
                    }
                }
            },
        ) { padding ->
            if (loaded != null) {
                val entry = loaded.item.entry
                val dates = loaded.item.dates.sortedWith(compareBy({ it.date }, { it.id }))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.m, end = Space.m,
                        top = padding.calculateTopPadding() + Space.s,
                        bottom = padding.calculateBottomPadding() + Space.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "title") {
                        Text(entry.title, color = c.text, style = MaterialTheme.typography.headlineLarge)
                    }
                    if (!entry.remarks.isNullOrBlank()) {
                        item(key = "remarks") {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("REMARKS", color = c.textDim, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(Space.xs))
                                Text(entry.remarks, color = c.text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    item(key = "dates-header") { SectionHeader("DATES", dates.size, c.purple) }
                    if (dates.isEmpty()) {
                        item(key = "no-dates") {
                            Text("This entry has no dates.", color = c.textDim, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(dates, key = { it.id }) { d -> DateRowDetail(d, loaded.today) }
                }
            }
        }
    }

    if (confirmDelete && loaded != null) {
        CyberAlert(
            title = "Delete this entry?",
            text = "\"${loaded.item.entry.title}\" and all of its dates and reminders will be permanently removed.",
            confirmText = "DELETE",
            dismissText = "CANCEL",
            danger = true,
            onConfirm = {
                confirmDelete = false
                vm.delete(onDone = {})
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DateRowDetail(d: EntryDateEntity, today: LocalDate) {
    val c = Cyber.colors
    val isExpiry = d.type == DateType.EXPIRY
    val status = if (isExpiry) expiryStatus(d.date, today) else null
    val accent = when (status) {
        ExpiryStatus.EXPIRED -> c.danger
        ExpiryStatus.EXPIRES_TODAY -> c.magenta
        ExpiryStatus.EXPIRING_SOON -> c.pink
        else -> null
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(d.label, color = c.textDim, style = MaterialTheme.typography.labelMedium)
                Text(prettyDate(d.date), color = c.text, style = MaterialTheme.typography.titleMedium)
                if (isExpiry) {
                    Text(
                        daysText(daysUntil(d.date, today)).replaceFirstChar { it.uppercase() },
                        color = accent ?: c.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (status != null) {
                Spacer(Modifier.padding(start = Space.s))
                StatusBadge(status)
            }
        }
        if (isExpiry) {
            Text(
                if (d.notificationEnabled) "Reminders on: 7 days before and on the day, 9:00 AM" else "Reminders off",
                color = c.textDim,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = Space.s),
            )
        }
    }
}
