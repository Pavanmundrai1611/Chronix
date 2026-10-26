package app.cybertracker.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cybertracker.ui.components.BackTopBar
import app.cybertracker.ui.components.CyberAlert
import app.cybertracker.ui.components.CyberTextField
import app.cybertracker.ui.components.GhostButton
import app.cybertracker.ui.components.GlassCard
import app.cybertracker.ui.components.NeonBackground
import app.cybertracker.ui.components.SectionHeader
import app.cybertracker.ui.components.TextInputDialog
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space

@Composable
fun ListDetailScreen(vm: ListDetailViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Cyber.colors
    var newItem by rememberSaveable { mutableStateOf("") }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is ListDetailState.NotFound) onBack()
    }
    val loaded = (state as? ListDetailState.Loaded)?.data

    fun submit() {
        if (newItem.isNotBlank()) {
            vm.addItem(newItem)
            newItem = ""
        }
    }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(title = loaded?.list?.title ?: "List", onBack = onBack) {
                    if (loaded != null) {
                        IconButton(onClick = { showRename = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename list", tint = c.cyan)
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete list", tint = c.danger)
                        }
                    }
                }
            },
            bottomBar = {
                if (loaded != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(Space.m),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CyberTextField(
                            value = newItem,
                            onValueChange = { newItem = it.take(200) },
                            label = "Add an item",
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { submit() }, enabled = newItem.isNotBlank()) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add item",
                                tint = if (newItem.isNotBlank()) c.cyan else c.textDim,
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (loaded != null) {
                val sorted = loaded.items.sortedWith(compareBy({ it.createdAt }, { it.id }))
                val open = sorted.filter { !it.checked }
                val done = sorted.filter { it.checked }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.m, end = Space.m,
                        top = padding.calculateTopPadding() + Space.s,
                        bottom = padding.calculateBottomPadding() + Space.m,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (sorted.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "This list is empty. Type an item below and press add.",
                                color = c.textDim,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (open.isNotEmpty()) {
                        item(key = "h-open") { SectionHeader("TO BUY", open.size) }
                        items(open, key = { it.id }) { item ->
                            ItemRow(item.text, false, { vm.setChecked(item.id, it) }, { vm.deleteItem(item.id) }, Modifier.animateItem())
                        }
                    }
                    if (done.isNotEmpty()) {
                        item(key = "h-done") { SectionHeader("DONE", done.size, c.ok) }
                        items(done, key = { it.id }) { item ->
                            ItemRow(item.text, true, { vm.setChecked(item.id, it) }, { vm.deleteItem(item.id) }, Modifier.animateItem())
                        }
                        item(key = "clear") {
                            GhostButton(
                                text = "CLEAR CHECKED",
                                danger = true,
                                onClick = { showClear = true },
                                modifier = Modifier.fillMaxWidth().padding(top = Space.s),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRename && loaded != null) {
        TextInputDialog(
            title = "Rename list",
            label = "List name",
            initial = loaded.list.title,
            confirmText = "SAVE",
            onConfirm = {
                vm.rename(it)
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }
    if (showDelete && loaded != null) {
        CyberAlert(
            title = "Delete this list?",
            text = "\"${loaded.list.title}\" and its ${loaded.items.size} items will be permanently removed.",
            confirmText = "DELETE",
            dismissText = "CANCEL",
            danger = true,
            onConfirm = {
                showDelete = false
                vm.deleteList()
            },
            onDismiss = { showDelete = false },
        )
    }
    if (showClear) {
        CyberAlert(
            title = "Clear checked items?",
            text = "All ticked items will be removed from this list.",
            confirmText = "CLEAR",
            dismissText = "CANCEL",
            danger = true,
            onConfirm = {
                showClear = false
                vm.clearChecked()
            },
            onDismiss = { showClear = false },
        )
    }
}

@Composable
private fun ItemRow(
    text: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Cyber.colors
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onChecked,
                colors = CheckboxDefaults.colors(
                    checkedColor = c.cyan,
                    checkmarkColor = c.onAccent,
                    uncheckedColor = c.textDim,
                ),
            )
            Text(
                text,
                color = if (checked) c.textDim else c.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f).clickable { onChecked(!checked) },
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Remove $text", tint = c.textDim)
            }
        }
    }
}
