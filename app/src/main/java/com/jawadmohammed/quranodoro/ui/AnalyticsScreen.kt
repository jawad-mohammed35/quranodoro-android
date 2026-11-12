package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun WeeklySummary(sessions: List<FocusSession>) {
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val values = days.map { d ->
        sessions.filter { localDate(it.completedAt) == d }.sumOf { it.minutes }
    }
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { i, d ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                QText(values[i].toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier.height(80.dp).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier.fillMaxWidth(.7f)
                            .height((values[i].toFloat() / max * 76).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = .75f)),
                    )
                }
                QText(
                    d.format(DateTimeFormatter.ofPattern("EEEEE", java.util.Locale.ENGLISH)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    QText(
        "Minutes of focus · last 7 days",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun AnalyticsScreen(vm: AppViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val dhikr by vm.dhikr.collectAsStateWithLifecycle()
    val trees by vm.trees.collectAsStateWithLifecycle()
    Page {
        Heading("A little progress, every day", "Your time and intentions, reflected.")
        Section("Your practice") {
            QText(
                "${sessions.size} completed Pomodoros",
                style = MaterialTheme.typography.titleLarge,
            )
            QText("${sessions.sumOf{it.minutes}} focus minutes · ${streak(sessions)} day streak")
            QText(
                "Average session: ${if(sessions.isEmpty())0 else sessions.sumOf{it.minutes}/sessions.size} minutes",
            )
            QText(
                "${trees.count{it.maturedAt!=null}} premium palms · ${dhikr.size} completed dhikr sessions",
            )
        }
        Section("This week") { WeeklySummary(sessions) }
        Section("Monthly focus") {
            (11 downTo 0).forEach { offset ->
                val month = YearMonth.now().minusMonths(offset.toLong())
                val minutes =
                    sessions
                        .filter { YearMonth.from(localDate(it.completedAt)) == month }
                        .sumOf { it.minutes }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    QText(
                        month.format(
                            DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH),
                        ),
                    )
                    QText("$minutes min")
                }
            }
        }
        Section("Streak history") {
            QText(
                "Last 30 days · filled cells are days with completed focus",
                style = MaterialTheme.typography.bodySmall,
            )
            val days = sessions.map { localDate(it.completedAt) }.toSet()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (29 downTo 0).toList().chunked(10).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { offset ->
                            val d = LocalDate.now().minusDays(offset.toLong())
                            Box(
                                Modifier.weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (d in days) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer,
                                    ),
                            )
                        }
                    }
                }
            }
        }
        Section("Session history") {
            if (sessions.isEmpty()) QText("Your first completed focus session will appear here.")
            sessions.take(100).forEach {
                QText(
                    "${localDate(it.completedAt)} · ${it.minutes} min · ${it.preset}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
