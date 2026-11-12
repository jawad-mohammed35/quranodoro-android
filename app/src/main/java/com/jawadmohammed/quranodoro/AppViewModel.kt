package com.jawadmohammed.quranodoro

import android.app.Application
import android.content.ComponentName
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.session.*
import com.jawadmohammed.quranodoro.audio.QuranService
import com.jawadmohammed.quranodoro.data.*
import com.jawadmohammed.quranodoro.timer.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as QuranodoroApp
    val dao = app.database.dao()
    val prefs = app.preferences
    val preferences = prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())
    val sessions = dao.sessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val trees = dao.trees().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dhikr = dao.dhikr().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tasbih =
        dao.tasbihFlow()
            .map { it ?: TasbihState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TasbihState())
    val timer =
        dao.timerFlow()
            .map { it ?: TimerState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TimerState())
    val left = MutableStateFlow(1500000L)
    val playing = MutableStateFlow(false)
    val buffering = MutableStateFlow(false)
    val audioError = MutableStateFlow<String?>(null)
    val position = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)
    val initialized = MutableStateFlow(false)
    private val timerMutex = Mutex()
    val foreground = MutableStateFlow(false)
    private var controller: MediaController? = null
    private val future =
        MediaController.Builder(
                app,
                SessionToken(app, ComponentName(app, QuranService::class.java)),
            )
            .buildAsync()

    init {
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { player ->
                        controller = player
                        player.volume = preferences.value.volume
                        player.addListener(
                            object : Player.Listener {
                                override fun onIsPlayingChanged(isPlaying: Boolean) {
                                    playing.value = isPlaying
                                }

                                override fun onPlaybackStateChanged(state: Int) {
                                    buffering.value = state == Player.STATE_BUFFERING
                                }

                                override fun onPlayerError(error: PlaybackException) {
                                    audioError.value =
                                        "Unable to stream this recitation. Check your connection and try again."
                                    buffering.value = false
                                }

                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    val number =
                                        mediaItem?.mediaId?.substringAfterLast('-')?.toIntOrNull()
                                            ?: return
                                    this@AppViewModel.run {
                                        prefs.int("surah", number.coerceIn(1, 114))
                                    }
                                }
                            },
                        )
                    }
                    .onFailure {
                        audioError.value = "Audio service unavailable. Reopen the app to retry."
                    }
            },
            ContextCompat.getMainExecutor(app),
        )
        viewModelScope.launch {
            if (dao.timer() == null) dao.save(TimerState())
            if (dao.tasbih() == null) dao.save(TasbihState())
            val loadedPreferences = prefs.flow.first()
            preferences.first { it == loadedPreferences }
            initialized.value = true
            while (isActive) {
                foreground.first { it }
                timerMutex.withLock {
                    val t = timer.value
                    left.value =
                        remaining(
                            t,
                            System.currentTimeMillis(),
                            SystemClock.elapsedRealtime(),
                            boot(app),
                        )
                    if (t.running && left.value == 0L) {
                        if (
                            dao.complete(
                                t.token,
                                System.currentTimeMillis(),
                                SystemClock.elapsedRealtime(),
                                boot(app),
                            )
                        ) {
                            if (preferences.value.notifications) notifyCompletion(app, t.phase)
                            if (preferences.value.focusAudio) controller?.pause()
                            if (preferences.value.autoStart) startInternal()
                        }
                    }
                }
                controller?.let {
                    position.value = it.currentPosition.coerceAtLeast(0)
                    duration.value = it.duration.coerceAtLeast(0)
                }
                delay(250)
            }
        }
        viewModelScope.launch { preferences.collect { controller?.volume = it.volume } }
    }

    fun run(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun string(key: String, value: String) = run { prefs.string(key, value) }

    fun bool(key: String, value: Boolean) = run { prefs.bool(key, value) }

    fun number(key: String, value: Int) = run { prefs.int(key, value) }

    fun volume(value: Float) = run { prefs.float("volume", value) }

    fun favorite(n: Int) = run {
        val p = preferences.value.favorites
        prefs.set("favorites", if (n.toString() in p) p - n.toString() else p + n.toString())
    }

    fun increment() = run { dao.increment() }

    fun target(n: Int) = run { dao.setTarget(n) }

    fun phrase(text: String) = run { dao.setPhrase(text) }

    fun resetTasbih() = run { dao.resetTasbih() }

    private suspend fun startInternal() {
        val t = dao.timer() ?: TimerState()
        if (t.running) return
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val next =
            t.copy(
                running = true,
                endWall = now + t.remainingMs,
                endElapsed = elapsed + t.remainingMs,
                boot = boot(app),
                treeId = if (t.endWall == 0L) dao.activeTree()?.id else t.treeId,
            )
        dao.save(next)
        schedule(app, next.token, next.remainingMs)
        if (preferences.value.focusAudio && next.phase == "Focus") play()
    }

    fun toggleTimer() = run {
        timerMutex.withLock {
            val t = dao.timer() ?: TimerState()
            if (t.running) {
                val rem =
                    remaining(
                        t,
                        System.currentTimeMillis(),
                        SystemClock.elapsedRealtime(),
                        boot(app),
                    )
                if (rem == 0L)
                    dao.complete(
                        t.token,
                        System.currentTimeMillis(),
                        SystemClock.elapsedRealtime(),
                        boot(app),
                    )
                else dao.save(t.copy(running = false, remainingMs = rem))
                cancelCompletion(app)
                if (preferences.value.focusAudio) controller?.pause()
            } else startInternal()
        }
    }

    fun resetTimer() = run {
        timerMutex.withLock {
            val t = dao.timer() ?: TimerState()
            dao.save(
                t.copy(
                    token = UUID.randomUUID().toString(),
                    running = false,
                    remainingMs = t.durationMs,
                    treeId = null,
                    endWall = 0,
                    endElapsed = 0,
                ),
            )
            cancelCompletion(app)
            if (preferences.value.focusAudio) controller?.pause()
        }
    }

    fun skip() = run {
        timerMutex.withLock {
            val t = dao.timer() ?: TimerState()
            val phase = if (t.phase == "Focus") "Short break" else "Focus"
            val ms = (if (phase == "Focus") t.focus else t.shortBreak) * 60000L
            dao.save(
                t.copy(
                    token = UUID.randomUUID().toString(),
                    phase = phase,
                    running = false,
                    remainingMs = ms,
                    durationMs = ms,
                    treeId = null,
                    endWall = 0,
                    endElapsed = 0,
                ),
            )
            cancelCompletion(app)
            if (preferences.value.focusAudio) controller?.pause()
        }
    }

    fun selectPreset(p: Preset) = run {
        timerMutex.withLock {
            cancelCompletion(app)
            dao.save(
                TimerState(
                    presetName = p.name,
                    focus = p.focus,
                    shortBreak = p.shortBreak,
                    longBreak = p.longBreak,
                    cycles = p.cycles,
                    remainingMs = p.focus * 60000L,
                    durationMs = p.focus * 60000L,
                ),
            )
            if (preferences.value.focusAudio) controller?.pause()
        }
    }

    fun customPresets(): List<Preset> = runCatching {
        val array = JSONArray(preferences.value.customPresets.ifBlank { "[]" })
        (0 until array.length()).map {
            val o = array.getJSONObject(it)
            Preset(
                o.getString("name"),
                o.getInt("focus"),
                o.getInt("short"),
                o.getInt("long"),
                o.getInt("cycles"),
            )
        }
    }
        .getOrDefault(emptyList())

    fun addPreset(p: Preset) {
        val a = JSONArray(preferences.value.customPresets.ifBlank { "[]" })
        a.put(
            JSONObject()
                .put("name", p.name)
                .put("focus", p.focus)
                .put("short", p.shortBreak)
                .put("long", p.longBreak)
                .put("cycles", p.cycles),
        )
        string("customPresets", a.toString())
        selectPreset(p)
    }

    fun removePreset(p: Preset) {
        val a = JSONArray()
        customPresets()
            .filter { it != p }
            .forEach {
                a.put(
                    JSONObject()
                        .put("name", it.name)
                        .put("focus", it.focus)
                        .put("short", it.shortBreak)
                        .put("long", it.longBreak)
                        .put("cycles", it.cycles),
                )
            }
        string("customPresets", a.toString())
    }

    fun play() {
        val p = preferences.value
        val reciter = reciters[p.reciter.coerceIn(reciters.indices)]
        val surah = surahs[p.surah.coerceIn(1, 114) - 1]
        val c = controller ?: return
        audioError.value = null
        c.volume = p.volume
        val id = "${reciter.id}-${surah.number}"
        if (c.currentMediaItem?.mediaId != id || c.playbackState == Player.STATE_IDLE) {
            val list = surahs.map { s ->
                MediaItem.Builder()
                    .setMediaId("${reciter.id}-${s.number}")
                    .setUri(reciter.url(s.number))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("${s.number}. ${s.name}")
                            .setArtist(reciter.name)
                            .setAlbumTitle("Quranodoro")
                            .build(),
                    )
                    .build()
            }
            c.setMediaItems(list, surah.number - 1, 0)
            c.prepare()
        }
        c.play()
    }

    fun toggleAudio() {
        if (playing.value) controller?.pause() else play()
    }

    fun stopAudio() {
        controller?.stop()
        controller?.seekTo(0)
        position.value = 0
        audioError.value = null
    }

    fun selectAudio(
        reciter: Int = preferences.value.reciter,
        surah: Int = preferences.value.surah,
    ) = run {
        val resume = playing.value
        controller?.pause()
        prefs.int("reciter", reciter.coerceIn(reciters.indices))
        prefs.int("surah", surah.coerceIn(1, 114))
        if (resume) {
            preferences.first { it.reciter == reciter && it.surah == surah }
            play()
        }
    }

    fun seek(ms: Long) {
        controller?.seekTo(ms)
    }

    fun movePanel(id: String, before: String) = run {
        val order = preferences.value.order.toMutableList()
        if (id != before && id in order && before in order) {
            order.remove(id)
            order.add(order.indexOf(before), id)
            prefs.string("order", order.joinToString(","))
        }
    }

    fun hidePanel(id: String) = run { prefs.set("hidden", preferences.value.hidden + id) }

    fun showPanel(id: String) = run { prefs.set("hidden", preferences.value.hidden - id) }

    fun resizePanel(id: String) = run {
        val s = preferences.value.wide
        prefs.set("wide", if (id in s) s - id else s + id)
    }

    suspend fun backup(): String {
        val json =
            JSONObject()
                .put("format", "quranodoro-android")
                .put("version", 1)
                .put("exportedAt", System.currentTimeMillis())
        fun objects(items: List<Any>): JSONArray {
            val a = JSONArray()
            items.forEach { item ->
                val o = JSONObject()
                item.javaClass.declaredFields
                    .filter { !it.isSynthetic }
                    .forEach { f ->
                        f.isAccessible = true
                        o.put(f.name, f.get(item) ?: JSONObject.NULL)
                    }
                a.put(o)
            }
            return a
        }
        json
            .put("sessions", objects(dao.sessions().first()))
            .put("trees", objects(dao.trees().first()))
            .put("dhikr", objects(dao.dhikr().first()))
            .put("tasbih", objects(listOf(dao.tasbih() ?: TasbihState())))
            .put("timer", objects(listOf(dao.timer() ?: TimerState())))
        val settings = JSONObject()
        val raw = app.applicationContext.preferences.data.first()
        raw.asMap().forEach { (k, v) ->
            settings.put(k.name, if (v is Set<*>) JSONArray(v.toList()) else v)
        }
        json.put("preferences", settings)
        return json.toString(2)
    }

    override fun onCleared() {
        MediaController.releaseFuture(future)
        super.onCleared()
    }
}
