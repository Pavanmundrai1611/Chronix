package app.cybertracker.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.ui.components.EmptyState
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.prettyDateLong
import app.cybertracker.ui.prettyTime
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space

@Composable
fun RemindersTab(vm: RemindersViewModel, contentPadding: PaddingValues, onOpen: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshNow() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.loading && state.upcoming.isEmpty() && state.past.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "No reminders yet",
                    message = "Get a notification at an exact date and time, like booking movie tickets. " +
                        "Tap NEW REMINDER to add one.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item(key = "h-up") { SectionHeader("UPCOMING", state.upcoming.size, c.cyan) }
            items(state.upcoming, key = { "u-" + it.id }) { r ->
                ReminderCard(r, past = false, onClick = { onOpen(r.id) }, modifier = Modifier.animateItem())
            }
        }
        if (state.past.isNotEmpty()) {
            item(key = "h-past") { SectionHeader("PAST", state.past.size, c.textDim) }
            items(state.past, key = { "p-" + it.id }) { r ->
                ReminderCard(r, past = true, onClick = { onOpen(r.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ReminderCard(r: CustomReminderEntity, past: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Cyber.colors
    GlassCard(modifier = modifier.fillMaxWidth(), accent = if (past) null else c.purple, onClick = onClick) {
        Text(
            r.title,
            color = if (past) c.textDim else c.text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            (if (past) "Passed · " else "") + prettyDateLong(r.date) + " at " + prettyTime(r.time),
            color = if (past) c.textDim else c.cyan,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Space.xs),
        )
        if (!r.note.isNullOrBlank()) {
            Text(
                r.note,
                color = c.textDim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Space.xs),
            )
        }
    }
}
