package com.jawadmohammed.quranodoro.timer

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jawadmohammed.quranodoro.*
import com.jawadmohammed.quranodoro.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** User-started countdown. System chronometer redraws without per-second notification posts. */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as QuranodoroApp
    private var observer: Job? = null
    private var finishing: Job? = null
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        // A subsequent command may arrive after the Room observer already posted
        // its new state. Never overwrite that notification with the startup placeholder.
        if (observer == null) ServiceCompat.startForeground(this, ID, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.notification_timer).setContentTitle("Restoring your session")
            .setSilent(true).setOnlyAlertOnce(true).setOngoing(true).build(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        scope.launch {
            val action = intent?.action
            val token = intent?.getStringExtra("token")
            if (action != null && token != null) {
                val updated = app.database.dao().notificationAction(token, action,
                    System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(this@TimerService))
                if (updated != null) {
                    cancelCompletion(this@TimerService)
                    if (updated.running) schedule(this@TimerService, updated.token, updated.remainingMs)
                    if (app.preferences.flow.first().focusAudio) setFocusPlayback(app, updated.running && updated.phase == "Focus")
                }
            }
            if (observer == null) observer = scope.launch {
                app.database.dao().timerFlow().filterNotNull().collectLatest { timer ->
                    val active = timer.running || timer.remainingMs < timer.durationMs
                    if (!active) {
                        finishing?.join()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else if (timer.running) {
                        ServiceCompat.startForeground(this@TimerService, ID, notification(timer, true),
                            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
                        val left = remaining(timer, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(this@TimerService))
                        delay(left)
                        // Completion emits a new Room value. Run outside collectLatest so the
                        // emission cannot cancel the completion alert/audio cleanup halfway through.
                        finishing = scope.launch { completeSession(this@TimerService, timer.token) }
                    } else {
                        // A paused session remains controllable in the foreground. No ticker,
                        // polling or wake lock runs here. Keeping its lifecycle avoids racing
                        // stopSelf/onDestroy against a quick Resume action on slower devices.
                        ServiceCompat.startForeground(this@TimerService, ID, notification(timer, false),
                            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Active focus sessions", NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(t: TimerState, running: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 21,
            Intent(this, MainActivity::class.java).putExtra("route", "focus")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(value: String) = PendingIntent.getForegroundService(this, if (value == "stop") 23 else 22,
            Intent(this, TimerService::class.java).setAction(value).putExtra("token", t.token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val left = remaining(t, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(this))
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.notification_timer)
            .setContentTitle("${t.phase} ${if (running) "in progress" else "paused"}")
            .setContentText(if (running) "A little intention. A little progress." else "${(left + 59999) / 60000} min remaining · resume when ready")
            .setContentIntent(open).setOnlyAlertOnce(true).setSilent(true)
            .setOngoing(running).setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setWhen(System.currentTimeMillis() + left).setShowWhen(running)
            .setUsesChronometer(running).setChronometerCountDown(true)
            .addAction(0, if (running) "Pause" else "Resume", action(if (running) "pause" else "resume"))
            .addAction(0, "Stop", action("stop"))
            .build()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        const val ID = 11
        const val CHANNEL = "active-timer"
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }
        fun clear(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
            context.getSystemService(NotificationManager::class.java).cancel(ID)
        }
    }
}
