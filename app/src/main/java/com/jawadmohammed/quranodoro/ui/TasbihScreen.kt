package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.localDate
import java.time.LocalDate

@Composable
fun TasbihScreen(vm: AppViewModel) {
    val s by vm.tasbih.collectAsStateWithLifecycle()
    val p by vm.preferences.collectAsStateWithLifecycle()
    val sessions by vm.dhikr.collectAsStateWithLifecycle()
    var reset by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    var phrase by remember(s.phrase) { mutableStateOf(s.phrase) }
    val haptic = LocalHapticFeedback.current
    val progress by
        animateFloatAsState(
            (s.count.toFloat() / s.target).coerceIn(0f, 1f),
            label = "dhikr progress",
        )
    Page {
        Heading("Remember, gently", "A quiet moment for your heart.")
        Glass(Modifier.fillMaxWidth()) {
            Picker(
                "Dhikr",
                listOf(
                        "SubhanAllah",
                        "Alhamdulillah",
                        "Allahu Akbar",
                        "La ilaha illa Allah",
                        "Astaghfirullah",
                        s.phrase,
                    )
                    .distinct(),
                s.phrase,
                onSelect = vm::phrase,
            )
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val diameter = minOf(maxWidth, 280.dp)
                Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 7.dp,
                        trackColor =
                            if (LocalOled.current) MaterialTheme.colorScheme.outlineVariant
                            else MaterialTheme.colorScheme.primaryContainer,
                    )
                    Surface(
                        onClick = {
                            if (p.haptics)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.increment()
                        },
                        shape = CircleShape,
                        border =
                            if (LocalOled.current)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                )
                            else null,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            QText(
                                s.count.toString(),
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Light,
                            )
                            QText("of ${s.target}")
                            Spacer(Modifier.height(14.dp))
                            QText("Tap to count", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (s.count >= s.target)
                QText(
                    "Alhamdulillah. Your target is complete.",
                    color = MaterialTheme.colorScheme.primary,
                )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { n ->
                    FilterChip(
                        selected = s.target == n,
                        onClick = { vm.target(n) },
                        label = { QText(n.toString()) },
                    )
                }
            }
            OutlinedTextField(
                custom,
                { custom = it.filter(Char::isDigit).take(4) },
                label = { QText("Custom target (1–9999)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                trailingIcon = {
                    TextButton(
                        enabled = (custom.toIntOrNull() ?: 0) > 0,
                        onClick = {
                            vm.target(custom.toInt())
                            custom = ""
                        },
                    ) {
                        QText("Set")
                    }
                },
            )
            TextButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth()) {
                QText("Reset counter")
            }
        }
        Section("Your practice") {
            Toggle("Touch feedback", value = p.haptics, onChange = { vm.bool("haptics", it) })
            OutlinedTextField(
                phrase,
                { phrase = it.take(160) },
                label = { QText("Custom dhikr") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { vm.phrase(phrase) }) { QText("Use custom dhikr") }
            QText(
                "${sessions.count{localDate(it.completedAt)==LocalDate.now()}} completed today · ${sessions.size} all time",
                style = MaterialTheme.typography.bodyMedium,
            )
            QText(
                "${sessions.count { localDate(it.completedAt)>=LocalDate.now().minusDays(6) }} completed in the last 7 days",
                style = MaterialTheme.typography.bodyMedium,
            )
            sessions.take(10).forEach {
                QText(
                    "${localDate(it.completedAt)} · ${it.target} ${it.phrase}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (reset)
        Confirm(
            "Begin again?",
            "Reset the current count to zero. Completed dhikr sessions stay in your history.",
            { reset = false },
            vm::resetTasbih,
        )
}
