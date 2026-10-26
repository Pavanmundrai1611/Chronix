package app.cybertracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cybertracker.domain.ExpiryStatus
import app.cybertracker.ui.statusLabel
import app.cybertracker.ui.theme.Cyber
import app.cybertracker.ui.theme.CyberShapes
import app.cybertracker.ui.theme.Space

@Composable
fun NeonBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Cyber.colors
    val purpleA = if (c.isDark) 0.24f else 0.10f
    val cyanA = if (c.isDark) 0.14f else 0.08f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(c.purple.copy(alpha = purpleA), Color.Transparent),
                        center = Offset(size.width * 0.95f, 0f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(c.cyan.copy(alpha = cyanA), Color.Transparent),
                        center = Offset(0f, size.height * 0.9f),
                        radius = size.width * 1.1f,
                    ),
                )
            },
        content = content,
    )
}

/** Layered glass-like card. Pass [accent] for a restrained glowing border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Cyber.colors
    val shape = CyberShapes.card
    val border = if (accent != null) {
        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f)))
    } else {
        SolidColor(c.border)
    }
    val glow = if (accent != null && c.isDark) {
        Modifier.shadow(10.dp, shape, ambientColor = accent, spotColor = accent)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(glow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.surfaceHigh, c.surface)))
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(Space.m),
        content = content,
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Cyber.colors
    val a = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(CyberShapes.pill)
            .background(Brush.horizontalGradient(listOf(c.cyan.copy(alpha = a), c.purple.copy(alpha = a))))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = c.onAccent)
            Text(text, color = c.onAccent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
) {
    val c = Cyber.colors
    val color = if (danger) c.danger else c.cyan
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CyberShapes.pill)
            .border(BorderStroke(1.5.dp, color), CyberShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = color)
            Text(text, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
) {
    val c = Cyber.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = isError,
        supportingText = if (isError && errorText != null) ({ Text(errorText) }) else null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = if (leadingIcon != null) ({ Icon(leadingIcon, contentDescription = null) }) else null,
        shape = CyberShapes.field,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            focusedBorderColor = c.cyan,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.cyan,
            unfocusedLabelColor = c.textDim,
            cursorColor = c.cyan,
            errorBorderColor = c.danger,
            errorLabelColor = c.danger,
            errorSupportingTextColor = c.danger,
            focusedLeadingIconColor = c.cyan,
            unfocusedLeadingIconColor = c.textDim,
            focusedContainerColor = c.surface.copy(alpha = 0.6f),
            unfocusedContainerColor = c.surface.copy(alpha = 0.4f),
        ),
    )
}

/** Status pill. Always carries a text label, so colour is never the only indicator. */
@Composable
fun StatusBadge(status: ExpiryStatus, modifier: Modifier = Modifier) {
    val c = Cyber.colors
    val color = when (status) {
        ExpiryStatus.EXPIRED -> c.danger
        ExpiryStatus.EXPIRES_TODAY -> c.magenta
        ExpiryStatus.EXPIRING_SOON -> c.pink
        ExpiryStatus.SAFE -> c.ok
    }
    val alert = status != ExpiryStatus.SAFE
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (alert) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Row(
        modifier = modifier
            .clip(CyberShapes.pill)
            .background(color.copy(alpha = if (alert) 0.18f else 0.12f))
            .border(1.dp, color, CyberShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = pulse)))
        Spacer(Modifier.width(6.dp))
        Text(statusLabel(status), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val c = Cyber.colors
    Column(
        modifier = modifier
            .clip(CyberShapes.card)
            .background(Brush.verticalGradient(listOf(c.surfaceHigh, c.surface)))
            .border(1.dp, accent.copy(alpha = 0.6f), CyberShapes.card)
            .padding(horizontal = Space.m, vertical = Space.s + 4.dp),
    ) {
        Text(value, color = accent, style = MaterialTheme.typography.headlineMedium)
        Text(label, color = c.textDim, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SectionHeader(text: String, count: Int? = null, accent: Color = Cyber.colors.cyan, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (count != null) "$text  ($count)" else text,
            color = Cyber.colors.text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Custom neon calendar illustration + message for empty lists. */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    val c = Cyber.colors
    Column(
        modifier = modifier.padding(Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Canvas(Modifier.size(140.dp)) {
            val stroke = Stroke(width = 3.dp.toPx())
            drawCircle(c.purple.copy(alpha = 0.25f), radius = size.minDimension / 2f, style = stroke)
            drawCircle(c.cyan.copy(alpha = 0.18f), radius = size.minDimension / 2.6f, style = stroke)
            val w = size.width * 0.52f
            val h = size.height * 0.46f
            val tl = Offset((size.width - w) / 2f, (size.height - h) / 2f + 6.dp.toPx())
            drawRoundRect(
                brush = Brush.linearGradient(listOf(c.cyan, c.magenta)),
                topLeft = tl,
                size = Size(w, h),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = stroke,
            )
            drawLine(c.magenta, Offset(tl.x, tl.y + h * 0.3f), Offset(tl.x + w, tl.y + h * 0.3f), strokeWidth = 3.dp.toPx())
            drawLine(c.cyan, Offset(tl.x + w * 0.25f, tl.y - 8.dp.toPx()), Offset(tl.x + w * 0.25f, tl.y + 6.dp.toPx()), strokeWidth = 3.dp.toPx())
            drawLine(c.cyan, Offset(tl.x + w * 0.75f, tl.y - 8.dp.toPx()), Offset(tl.x + w * 0.75f, tl.y + 6.dp.toPx()), strokeWidth = 3.dp.toPx())
            drawCircle(c.purple, radius = 5.dp.toPx(), center = Offset(tl.x + w * 0.5f, tl.y + h * 0.65f))
        }
        Text(title, color = c.text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, color = c.textDim, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (action != null) action()
    }
}
