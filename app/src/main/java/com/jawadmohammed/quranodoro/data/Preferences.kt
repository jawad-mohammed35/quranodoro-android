package com.jawadmohammed.quranodoro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.preferences by preferencesDataStore("quranodoro")

data class UserPreferences(
    val theme: String = "System",
    val name: String = "",
    val language: String = "en",
    val haptics: Boolean = false,
    val notifications: Boolean = false,
    val autoStart: Boolean = false,
    val focusAudio: Boolean = true,
    val reciter: Int = 0,
    val surah: Int = 1,
    val volume: Float = .7f,
    val favorites: Set<String> = emptySet(),
    val favoriteReciter: Int = -1,
    val order: List<String> = panels.keys.toList(),
    val hidden: Set<String> = emptySet(),
    val wide: Set<String> = setOf("focus", "week", "reciter", "inspiration"),
    val customPresets: String = "",
)

class Preferences(context: Context) {
    private val store = context.preferences
    val flow =
        store.data.map { p ->
            UserPreferences(
                theme = p[stringPreferencesKey("theme")] ?: "System",
                name = p[stringPreferencesKey("name")] ?: "",
                language = p[stringPreferencesKey("language")] ?: "en",
                haptics = p[booleanPreferencesKey("haptics")] ?: false,
                notifications = p[booleanPreferencesKey("notifications")] ?: false,
                autoStart = p[booleanPreferencesKey("autoStart")] ?: false,
                focusAudio = p[booleanPreferencesKey("focusAudio")] ?: true,
                reciter = p[intPreferencesKey("reciter")] ?: 0,
                surah = p[intPreferencesKey("surah")] ?: 1,
                volume = p[floatPreferencesKey("volume")] ?: .7f,
                favorites = p[stringSetPreferencesKey("favorites")] ?: emptySet(),
                favoriteReciter = p[intPreferencesKey("favoriteReciter")] ?: -1,
                order =
                    ((p[stringPreferencesKey("order")]?.split(",")?.filter { it in panels }
                            ?: emptyList()) + panels.keys)
                        .distinct(),
                hidden = p[stringSetPreferencesKey("hidden")] ?: emptySet(),
                wide =
                    p[stringSetPreferencesKey("wide")]
                        ?: setOf("focus", "week", "reciter", "inspiration"),
                customPresets = p[stringPreferencesKey("customPresets")] ?: "",
            )
        }

    suspend fun string(key: String, value: String) {
        store.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun bool(key: String, value: Boolean) {
        store.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun int(key: String, value: Int) {
        store.edit { it[intPreferencesKey(key)] = value }
    }

    suspend fun float(key: String, value: Float) {
        store.edit { it[floatPreferencesKey(key)] = value }
    }

    suspend fun set(key: String, value: Set<String>) {
        store.edit { it[stringSetPreferencesKey(key)] = value }
    }

    suspend fun resetDashboard() {
        store.edit {
            it.remove(stringPreferencesKey("order"))
            it.remove(stringSetPreferencesKey("hidden"))
            it.remove(stringSetPreferencesKey("wide"))
        }
    }
}
