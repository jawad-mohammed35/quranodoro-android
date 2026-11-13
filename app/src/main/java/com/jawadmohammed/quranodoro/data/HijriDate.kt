package com.jawadmohammed.quranodoro.data

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** JDK/Android Umm al-Qura chronology, evaluated against the user's local civil date. */
fun hijriDate(date: LocalDate, adjustment: Int = 0): String =
    HijrahDate.from(date.plusDays(adjustment.coerceIn(-2, 2).toLong()))
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy 'AH'", Locale.ENGLISH))
