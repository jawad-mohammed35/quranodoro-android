package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val Light =
    lightColorScheme(
        primary = Color(0xFF52775A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDFEADE),
        onPrimaryContainer = Color(0xFF333F30),
        secondary = Color(0xFF777B50),
        secondaryContainer = Color(0xFFDFEADE),
        onSecondaryContainer = Color(0xFF333F30),
        background = Color(0xFFFAF8F3),
        onBackground = Color(0xFF333F30),
        surface = Color.White,
        onSurface = Color(0xFF333F30),
        surfaceVariant = Color(0xFFF1EEE6),
        onSurfaceVariant = Color(0xFF626C5D),
        outlineVariant = Color(0xFFE7E3D7),
        outline = Color(0xFFB2BAAA),
        surfaceContainerLowest = Color(0xFFFAF8F3), surfaceContainerLow = Color(0xFFF7F5EF),
        surfaceContainer = Color(0xFFF1F2E9), surfaceContainerHigh = Color(0xFFEFF1E7),
        surfaceContainerHighest = Color(0xFFE7EDE1),
        error = Color(0xFF9C5549),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFF8FB996),
        onPrimary = Color(0xFF1C221C),
        primaryContainer = Color(0xFF313B32),
        onPrimaryContainer = Color(0xFFDCE1D6),
        secondary = Color(0xFFAAAD83),
        secondaryContainer = Color(0xFF313B32),
        onSecondaryContainer = Color(0xFFDCE1D6),
        background = Color(0xFF1E211E),
        onBackground = Color(0xFFDCE1D6),
        surface = Color(0xFF272B26),
        onSurface = Color(0xFFDCE1D6),
        surfaceVariant = Color(0xFF2F342E),
        onSurfaceVariant = Color(0xFFB1B9AA),
        outlineVariant = Color(0xFF383D37),
        outline = Color(0xFF7B8B7A),
        surfaceContainerLowest = Color(0xFF1A1E1A), surfaceContainerLow = Color(0xFF232923),
        surfaceContainer = Color(0xFF272B26), surfaceContainerHigh = Color(0xFF2F352E),
        surfaceContainerHighest = Color(0xFF363D34),
        error = Color(0xFFCF9186),
    )

// Appearance changes at runtime. Tracked locals keep separately composed windows
// on the current palette even when their parent composition context is retained.
val LocalOled = androidx.compose.runtime.compositionLocalOf { false }
val LocalAppColors = androidx.compose.runtime.compositionLocalOf { Light }

private val Oled =
    Dark.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color.Black,
        surfaceTint = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
        primaryContainer = Color.Black,
        secondaryContainer = Color.Black,
        tertiaryContainer = Color.Black,
        outlineVariant = Color(0xFF465449),
    )

@Composable
fun QuranodoroTheme(theme: String, content: @Composable () -> Unit) {
    val target =
        when (theme) {
            "OLED Black" -> Oled
            "Dark" -> Dark
            "Light" -> Light
            else -> if (isSystemInDarkTheme()) Dark else Light
        }
    // OLED changes immediately to black; other palettes ease between their shared tones.
    val colors = if (target === Oled) target else target.copy(
        background = animatedTone(target.background), surface = animatedTone(target.surface),
        primary = animatedTone(target.primary), onSurface = animatedTone(target.onSurface),
        primaryContainer = animatedTone(target.primaryContainer), onBackground = animatedTone(target.onBackground),
        outlineVariant = animatedTone(target.outlineVariant), onSurfaceVariant = animatedTone(target.onSurfaceVariant),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalOled provides (target === Oled), LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colors, shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)), typography = Typography(
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        ), content = content)
    }
}

@Composable
private fun animatedTone(target: Color): Color {
    val tone by animateColorAsState(target, tween(320), label = "Pastel theme")
    return tone
}
