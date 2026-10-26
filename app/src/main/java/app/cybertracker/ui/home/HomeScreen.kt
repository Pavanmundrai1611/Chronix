package app.cybertracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.ui.components.CyberTextField
import app.cybertracker.ui.components.EmptyState
import app.cybertracker.ui.components.EntryCard
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.NeonButton
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.components.StatTile
import app.cybertracker.ui.components.TextInputDialog
import app.cybertracker.ui.lists.ListsTab
import app.cybertracker.ui.lists.ShoppingListsViewModel
import app.cybertracker.ui.reminders.RemindersTab
import app.cybertracker.ui.reminders.RemindersViewModel
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.CyberShapes
import app.cybertracker.ui.theme.Space

private val TAB_NAMES = listOf("DATES", "LISTS", "REMINDERS")

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    listsVm: ShoppingListsViewModel,
    remindersVm: RemindersViewModel,
    onAddEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenList: (String) -> Unit,
    onAddReminder: () -> Unit,
    onOpenReminder: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showNewList by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.m, vertical = Space.s),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "CYBER//DATES",
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = Brush.horizontalGradient(listOf(c.cyan, c.magenta)),
                            ),
                        )
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = c.text)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.m)
                            .padding(bottom = Space.s),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TAB_NAMES.forEachIndexed { index, name ->
                            TabChip(name, selected = tab == index, onClick = { tab = index }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            floatingActionButton = {
                NeonButton(
                    text = when (tab) {
                        0 -> "ADD ENTRY"
                        1 -> "NEW LIST"
                        else -> "NEW REMINDER"
                    },
                    icon = Icons.Default.Add,
                    onClick = {
                        when (tab) {
                            0 -> onAddEntry()
                            1 -> showNewList = true
                            else -> onAddReminder()
                        }
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            },
        ) { padding ->
            val contentPadding = PaddingValues(
                start = Space.m, end = Space.m,
                top = padding.calculateTopPadding() + Space.s,
                bottom = padding.calculateBottomPadding() + 110.dp,
            )
            when (tab) {
                0 -> DatesTab(state, vm, contentPadding, onOpenEntry)
                1 -> ListsTab(listsVm, contentPadding, onOpenList)
                else -> RemindersTab(remindersVm, contentPadding, onOpenReminder)
            }
        }
    }

    if (showNewList) {
        TextInputDialog(
            title = "New shopping list",
            label = "List name (e.g. Groceries)",
            initial = "",
            confirmText = "CREATE",
            onConfirm = { name ->
                showNewList = false
                listsVm.create(name, onCreated = onOpenList)
            },
            onDismiss = { showNewList = false },
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Cyber.colors
    val bg = if (selected) Brush.horizontalGradient(listOf(c.cyan, c.purple)) else Brush.horizontalGradient(listOf(c.surface, c.surface))
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CyberShapes.pill)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else c.border, CyberShapes.pill)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) c.onAccent else c.textDim,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun DatesTab(
    state: HomeUiState,
    vm: HomeViewModel,
    contentPadding: PaddingValues,
    onOpen: (String) -> Unit,
) {
    val c = Cyber.colors
    val searching = state.query.isNotBlank()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("ENTRIES", state.total.toString(), c.cyan, Modifier.weight(1f))
                StatTile("UPCOMING", state.upcomingExpiryCount.toString(), c.purple, Modifier.weight(1f))
                StatTile("ATTENTION", state.attention.size.toString(), c.pink, Modifier.weight(1f))
            }
        }
        if (state.total > 0 || searching) {
            item(key = "search") {
                CyberTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    label = "Search titles, labels, remarks",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!state.loading && state.total == 0) {
            item(key = "empty") {
                EmptyState(
                    title = "No entries yet",
                    message = "Track anything with dates: groceries, insurance, servicing, warranties. " +
                        "Tap ADD ENTRY to create your first one.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (searching) {
            if (state.results.isEmpty()) {
                item(key = "nomatch") {
                    EmptyState(
                        title = "No matches",
                        message = "Nothing matches \"${state.query.trim()}\".",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item(key = "h-results") { SectionHeader("RESULTS", state.results.size) }
                items(state.results, key = { "r-" + it.item.entry.id }) { s ->
                    EntryCard(s, onClick = { onOpen(s.item.entry.id) }, modifier = Modifier.animateItem())
                }
            }
        } else {
            if (state.attention.isNotEmpty()) {
                item(key = "h-attention") { SectionHeader("NEEDS ATTENTION", state.attention.size, c.pink) }
                items(state.attention, key = { "a-" + it.item.entry.id }) { s ->
                    EntryCard(s, onClick = { onOpen(s.item.entry.id) }, modifier = Modifier.animateItem())
                }
            }
            if (state.others.isNotEmpty()) {
                item(key = "h-others") {
                    SectionHeader(if (state.attention.isEmpty()) "ALL ENTRIES" else "OTHER ENTRIES", state.others.size)
                }
                items(state.others, key = { "o-" + it.item.entry.id }) { s ->
                    EntryCard(s, onClick = { onOpen(s.item.entry.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}
