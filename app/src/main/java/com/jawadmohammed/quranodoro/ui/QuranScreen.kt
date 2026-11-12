package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.*

@Composable
fun CompactPlayer(vm: AppViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val playing by vm.playing.collectAsStateWithLifecycle()
    val loading by vm.buffering.collectAsStateWithLifecycle()
    val error by vm.audioError.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            QText(
                surahs[prefs.surah.coerceIn(1, 114) - 1].name,
                style = MaterialTheme.typography.titleMedium,
            )
            QText(
                reciters[prefs.reciter.coerceIn(reciters.indices)].name,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = vm::toggleAudio) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            else
                Icon(
                    if (playing) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    if (playing) "Pause recitation" else "Play recitation",
                    Modifier.size(40.dp),
                )
        }
    }
    error?.let {
        QText(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = vm::play) { QText("Retry playback") }
    }
}

@Composable
fun QuranScreen(vm: AppViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val pos by vm.position.collectAsStateWithLifecycle()
    val duration by vm.duration.collectAsStateWithLifecycle()
    val s = surahs[prefs.surah.coerceIn(1, 114) - 1]
    val r = reciters[prefs.reciter.coerceIn(reciters.indices)]
    Page {
        Heading("Listen & reflect", "Let the words bring you back to stillness.")
        Glass(Modifier.fillMaxWidth()) {
            QText(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
            QText(
                s.arabic,
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            QText(
                "${s.name} · ${s.meaning}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            QText(
                "${s.ayahs} verses · ${s.revelation}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompactPlayer(vm)
            var seeking by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = seeking ?: pos.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { seeking = it },
                onValueChangeFinished = {
                    seeking?.let { vm.seek(it.toLong()) }
                    seeking = null
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = duration > 0,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QText(time(pos), style = MaterialTheme.typography.labelMedium)
                QText(time(duration), style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(
                    onClick = { vm.selectAudio(surah = (s.number - 1).coerceAtLeast(1)) },
                    enabled = s.number > 1,
                ) {
                    Icon(Icons.Filled.SkipPrevious, "Previous Surah")
                }
                IconButton(onClick = { vm.favorite(s.number) }) {
                    Icon(
                        if (s.number.toString() in prefs.favorites) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        "Favorite Surah",
                    )
                }
                IconButton(onClick = vm::stopAudio) { Icon(Icons.Filled.Stop, "Stop recitation") }
                IconButton(
                    onClick = { vm.selectAudio(surah = (s.number + 1).coerceAtMost(114)) },
                    enabled = s.number < 114,
                ) {
                    Icon(Icons.Filled.SkipNext, "Next Surah")
                }
            }
        }
        Section("Your recitation") {
            Picker(
                "Reciter",
                reciters,
                r,
                onSelect = { vm.selectAudio(reciter = reciters.indexOf(it)) },
                text = { "${it.name}\n${it.arabic}" },
            )
            TextButton(
                onClick = {
                    vm.number(
                        "favoriteReciter",
                        if (prefs.favoriteReciter == prefs.reciter) -1 else prefs.reciter,
                    )
                },
            ) {
                Icon(Icons.Filled.StarOutline, null)
                QText(
                    if (prefs.favoriteReciter == prefs.reciter) "Favorite reciter"
                    else "Set as favorite reciter",
                )
            }
            if (prefs.favoriteReciter in reciters.indices && prefs.favoriteReciter != prefs.reciter)
                TextButton(onClick = { vm.selectAudio(reciter = prefs.favoriteReciter) }) {
                    QText("Use ${reciters[prefs.favoriteReciter].name}")
                }
            Picker(
                "Surah",
                surahs,
                s,
                onSelect = { vm.selectAudio(surah = it.number) },
                text = { "${it.number}. ${it.name} — ${it.arabic}" },
            )
            QText("Volume", style = MaterialTheme.typography.labelLarge)
            Slider(prefs.volume, { vm.volume(it) }, valueRange = 0f..1f)
        }
        if (prefs.favorites.isNotEmpty())
            Section("Favorite Surahs") {
                surahs
                    .filter { it.number.toString() in prefs.favorites }
                    .forEach { surah ->
                        TextButton(onClick = { vm.selectAudio(surah = surah.number) }) {
                            QText("${surah.number}. ${surah.name}")
                        }
                    }
            }
        QText(
            "Audio streamed from mp3quran.net. An internet connection is required. Recitations remain with their respective providers; no audio files are bundled or redistributed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
