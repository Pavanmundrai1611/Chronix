package app.cybertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cybertracker.domain.EntrySummary
import app.cybertracker.domain.ExpiryStatus
import app.cybertracker.ui.daysText
import app.cybertracker.ui.prettyDate
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.Space

@Composable
fun EntryCard(summary: EntrySummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Cyber.colors
    val entry = summary.item.entry
    val status = summary.status
    val accent = when (status) {
        ExpiryStatus.EXPIRED -> c.danger
        ExpiryStatus.EXPIRES_TODAY -> c.magenta
        ExpiryStatus.EXPIRING_SOON -> c.pink
        else -> null
    }
    GlassCard(modifier = modifier, accent = accent, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                entry.title,
                color = c.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (status != null) {
                Spacer(Modifier.width(Space.s))
                StatusBadge(status)
            }
        }
        val highlight = summary.highlight
        Spacer(Modifier.height(Space.s))
        if (highlight != null) {
            val days = summary.daysUntil
            val suffix = if (days != null) "  ·  ${daysText(days)}" else ""
            Text(
                "${highlight.label}: ${prettyDate(highlight.date)}$suffix",
                color = if (accent != null) accent else c.cyan,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text("No dates added", color = c.textDim, style = MaterialTheme.typography.bodyMedium)
        }
        if (!entry.remarks.isNullOrBlank()) {
            Text(
                entry.remarks,
                color = c.textDim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Space.xs),
            )
        }
    }
}
