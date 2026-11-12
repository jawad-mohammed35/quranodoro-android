package com.jawadmohammed.quranodoro.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class Preset(
    val name: String,
    val focus: Int,
    val shortBreak: Int,
    val longBreak: Int,
    val cycles: Int,
)

val presets =
    listOf(
        Preset("Classic 25 / 5", 25, 5, 15, 4),
        Preset("Extended 50 / 10", 50, 10, 25, 3),
        Preset("Long Form 90 / 20", 90, 20, 35, 2),
        Preset("Deep Work", 60, 12, 30, 3),
        Preset("Exam Mode", 40, 8, 20, 4),
    )
val stages =
    listOf(
        "Seed",
        "Sprout",
        "Small Tree",
        "Young Tree",
        "Mature Tree",
        "Fruiting Tree",
        "Premium Tree",
    )
val thresholds = listOf(0, 20, 50, 100, 170, 260, 380)
val treeTypes =
    linkedMapOf(
        "Ajwa" to 1.5,
        "Medjool" to 1.3,
        "Safawi" to 1.2,
        "Sukkari" to 1.1,
        "Deglet Noor" to 1.0,
        "Barhi" to 0.8,
    )
val panels =
    linkedMapOf(
        "focus" to "Today's focus",
        "streak" to "Current streak",
        "pomodoros" to "Total Pomodoros",
        "minutes" to "Total focus",
        "orchard" to "Orchard",
        "week" to "Weekly summary",
        "tree" to "Active tree",
        "reciter" to "Quran player",
        "inspiration" to "Daily reminder",
    )

fun growthPoints(minutes: Int) =
    if (minutes <= 0) 0
    else if (minutes <= 60) (minutes * .4).roundToInt()
    else (24 + (minutes - 60) * .3667).roundToInt()

fun stage(gp: Int, multiplier: Double) =
    thresholds.indexOfLast { gp >= (it * multiplier).roundToInt() }.coerceAtLeast(0)

fun growthProgress(gp: Int, multiplier: Double): Float {
    val s = stage(gp, multiplier)
    if (s == 6) return 1f
    val a = (thresholds[s] * multiplier).roundToInt()
    val b = (thresholds[s + 1] * multiplier).roundToInt()
    return ((gp - a).toFloat() / (b - a)).coerceIn(0f, 1f)
}

fun localDate(time: Long) =
    java.time.Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()

fun streak(sessions: List<FocusSession>, today: LocalDate = LocalDate.now()): Int {
    val days = sessions.map { localDate(it.completedAt) }.toSet()
    var day = if (today in days) today else today.minusDays(1)
    var n = 0
    while (day in days) {
        n++
        day = day.minusDays(1)
    }
    return n
}

fun remaining(timer: TimerState, wall: Long, elapsed: Long, boot: Int): Long =
    if (!timer.running) timer.remainingMs
    else
        ((if (timer.boot == boot) timer.endElapsed - elapsed else timer.endWall - wall))
            .coerceAtLeast(0)

data class Reciter(val id: String, val name: String, val arabic: String, val base: String) {
    fun url(surah: Int): String {
        require(surah in 1..114)
        return "$base/${surah.toString().padStart(3,'0')}.mp3"
    }
}

data class Surah(
    val number: Int,
    val name: String,
    val arabic: String,
    val meaning: String,
    val ayahs: Int,
    val revelation: String,
)

data class Reminder(val arabic: String, val english: String, val reference: String)
