package com.jawadmohammed.quranodoro

import android.app.Application
import androidx.room.Room
import com.jawadmohammed.quranodoro.data.*

class QuranodoroApp : Application() {
    val database by lazy {
        Room.databaseBuilder(this, QuranDatabase::class.java, "quranodoro.db").build()
    }
    val preferences by lazy { Preferences(this) }

    override fun onCreate() {
        super.onCreate()
        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    }
}
