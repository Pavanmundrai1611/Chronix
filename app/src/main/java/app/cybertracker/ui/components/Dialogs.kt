package app.cybertracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.CyberShapes

/** Themed confirmation dialog used for every destructive or permission-explaining step. */
@Composable
fun CyberAlert(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    val c = Cyber.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CyberShapes.card,
        containerColor = c.surfaceHigh,
        titleContentColor = c.text,
        textContentColor = c.textDim,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) c.danger else c.cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = c.textDim) }
        },
    )
}
