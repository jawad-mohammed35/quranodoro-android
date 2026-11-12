package com.jawadmohammed.quranodoro.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import org.json.JSONObject

val languages =
    linkedMapOf(
        "en" to "English",
        "ar" to "العربية",
        "fr" to "Français",
        "id" to "Bahasa Indonesia",
        "tr" to "Türkçe",
        "zh" to "中文",
    )
private val LocalTranslations = staticCompositionLocalOf { emptyMap<String, String>() }
private val LocalLanguage = staticCompositionLocalOf { "en" }
private val aliases =
    mapOf(
        "Home" to "Dashboard",
        "Farm" to "Date Farm",
        "Quran" to "Quran Player",
        "Your date farm" to "Date Farm",
        "Make room for focus" to "Pomodoro",
        "Listen & reflect" to "Quran Player",
        "Remember, gently" to "Tasbih",
        "Make it yours" to "Settings",
        "Your name (optional)" to "Your name",
        "System" to "Match system",
        "Touch feedback" to "Feedback",
        "Auto-start next phase" to "Auto-start next session",
        "Quran during focus" to "Play Quran during focus",
        "Begin focus" to "Start focus",
        "Start session" to "Start focus",
        "Your rhythm" to "Presets",
        "Timer preset" to "Presets",
        "Create custom preset" to "Custom preset",
        "Focus minutes" to "Focus (minutes)",
        "Short break minutes" to "Short break (minutes)",
        "Long break minutes" to "Long break (minutes)",
        "Focus sessions per cycle" to "Focus sessions before long break",
        "A little progress, every day" to "Analytics",
        "Your recitation" to "Recitation",
        "A calm companion" to "Recitation",
        "How your date farm grows" to "About Date Farm",
        "Meet the developer" to "About the Developer",
    )

@Composable
fun LocaleProvider(language: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val translated =
        remember(language) {
            fun read(lang: String): Map<String, String> {
                val o =
                    JSONObject(
                        context.assets.open("locales/$lang.json").bufferedReader().use {
                            it.readText()
                        },
                    )
                return o.keys().asSequence().associateWith { o.getString(it) }
            }
            val english = read("en")
            val locale = read(if (language in languages) language else "en")
            english.mapNotNull { (key, value) -> locale[key]?.let { value to it } }.toMap()
        }
    CompositionLocalProvider(
        LocalTranslations provides translated,
        LocalLanguage provides language,
        LocalLayoutDirection provides
            if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

/** Shared web strings translate directly. Android-specific copy has explicit English fallback. */
@Composable
fun QText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current,
) {
    val strings = LocalTranslations.current
    val translated =
        if (LocalLanguage.current == "en" || text.startsWith("Assalamu Alaikum")) text
        else strings[aliases[text] ?: text] ?: text
    androidx.compose.material3.Text(
        translated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
    )
}
