package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
        error = Color(0xFFCF9186),
    )

@Composable
fun QuranodoroTheme(theme: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            if (theme == "Dark" || theme == "System" && isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
