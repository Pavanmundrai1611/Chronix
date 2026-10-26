package app.cybertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class CyberColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val text: Color,
    val textDim: Color,
    val cyan: Color,
    val purple: Color,
    val magenta: Color,
    val pink: Color,
    val danger: Color,
    val ok: Color,
    val onAccent: Color,
)

val DarkCyber = CyberColors(
    isDark = true,
    bg = Color(0xFF090A12),
    surface = Color(0xFF11131F),
    surfaceHigh = Color(0xFF191C2E),
    border = Color(0xFF2B3050),
    text = Color(0xFFEAF0FF),
    textDim = Color(0xFFA2ABD0),
    cyan = Color(0xFF19F0FF),
    purple = Color(0xFF9D5CFF),
    magenta = Color(0xFFFF2BD6),
    pink = Color(0xFFFF4D8D),
    danger = Color(0xFFFF5470),
    ok = Color(0xFF3DF2A5),
    onAccent = Color(0xFF05060C),
)

val LightCyber = CyberColors(
    isDark = false,
    bg = Color(0xFFF4F3FB),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFECEAF8),
    border = Color(0xFFCBC7E6),
    text = Color(0xFF14152A),
    textDim = Color(0xFF52567A),
    cyan = Color(0xFF007687),
    purple = Color(0xFF6A2BD9),
    magenta = Color(0xFFB0008F),
    pink = Color(0xFFC4124F),
    danger = Color(0xFFC4122F),
    ok = Color(0xFF0B7A55),
    onAccent = Color(0xFFFFFFFF),
)

val LocalCyber = staticCompositionLocalOf { DarkCyber }

object Cyber {
    val colors: CyberColors
        @Composable @ReadOnlyComposable get() = LocalCyber.current
}

object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}

object CyberShapes {
    val card = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
    val field = RoundedCornerShape(14.dp)
}

private val CyberTypography = Typography().let { base ->
    base.copy(
        headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.8.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.8.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
    )
}

@Composable
fun CyberTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkCyber else LightCyber
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.cyan, onPrimary = c.onAccent,
            secondary = c.purple, onSecondary = c.onAccent,
            tertiary = c.magenta, onTertiary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textDim,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerHighest = c.surfaceHigh,
            outline = c.border, outlineVariant = c.border,
            error = c.danger, onError = c.onAccent,
        )
    } else {
        lightColorScheme(
            primary = c.cyan, onPrimary = c.onAccent,
            secondary = c.purple, onSecondary = c.onAccent,
            tertiary = c.magenta, onTertiary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textDim,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerHighest = c.surfaceHigh,
            outline = c.border, outlineVariant = c.border,
            error = c.danger, onError = c.onAccent,
        )
    }
    CompositionLocalProvider(LocalCyber provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CyberTypography,
            shapes = Shapes(medium = CyberShapes.field, large = CyberShapes.card, extraLarge = CyberShapes.card),
            content = content,
        )
    }
}
