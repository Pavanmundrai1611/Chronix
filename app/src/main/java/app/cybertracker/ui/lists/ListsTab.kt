package app.cybertracker.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.ui.components.EmptyState
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space

@Composable
fun ListsTab(vm: ShoppingListsViewModel, contentPadding: PaddingValues, onOpen: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.loading && state.lists.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "No shopping lists yet",
                    message = "Make a list, tick items off as you shop. Tap NEW LIST to start one.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(state.lists, key = { it.list.id }) { l ->
            val total = l.items.size
            val done = l.items.count { it.checked }
            val allDone = total > 0 && done == total
            GlassCard(
                modifier = Modifier.fillMaxWidth().animateItem(),
                accent = if (allDone) c.ok else null,
                onClick = { onOpen(l.list.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        l.list.title,
                        color = c.text,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (total == 0) "EMPTY" else if (allDone) "ALL DONE" else "$done/$total",
                        color = if (allDone) c.ok else c.cyan,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (total > 0) {
                    Spacer(Modifier.height(Space.s))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (allDone) c.ok else c.cyan,
                        trackColor = c.border,
                    )
                }
            }
        }
    }
}
