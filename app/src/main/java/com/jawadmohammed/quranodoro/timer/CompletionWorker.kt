package com.jawadmohammed.quranodoro.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.jawadmohammed.quranodoro.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.jawadmohammed.quranodoro.audio.QuranService
import java.util.concurrent.TimeUnit

fun boot(context: Context)=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)
fun schedule(context: Context,token: String,delay: Long) {
    val work=OneTimeWorkRequestBuilder<CompletionWorker>().setInitialDelay(delay.coerceAtLeast(0),TimeUnit.MILLISECONDS).setInputData(workDataOf("token" to token)).build()
    WorkManager.getInstance(context).enqueueUniqueWork("timer-completion",ExistingWorkPolicy.REPLACE,work)
}
fun cancelCompletion(context: Context) { WorkManager.getInstance(context).cancelUniqueWork("timer-completion") }
fun notifyCompletion(context: Context,phase: String) {
    if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
    val manager=context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel("sessions","Session completion",NotificationManager.IMPORTANCE_DEFAULT))
    val pending=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    manager.notify(10,NotificationCompat.Builder(context,"sessions").setSmallIcon(com.jawadmohammed.quranodoro.R.drawable.brand).setContentTitle("$phase complete").setContentText("Take a breath. Your next session is ready.").setContentIntent(pending).setAutoCancel(true).build())
}
class CompletionWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val app=applicationContext as QuranodoroApp
        val dao=app.database.dao(); val t=dao.timer()?:return Result.success()
        val token=inputData.getString("token")?:return Result.failure()
        val completed=dao.complete(token,System.currentTimeMillis(),SystemClock.elapsedRealtime(),boot(applicationContext))
        val preferences=app.preferences.flow.first()
        if(completed && preferences.notifications) notifyCompletion(applicationContext,t.phase)
        if(completed && preferences.focusAudio && t.phase=="Focus") withContext(Dispatchers.Main) {
            val future=MediaController.Builder(app,SessionToken(app,ComponentName(app,QuranService::class.java))).buildAsync()
            try {
                val controller=suspendCancellableCoroutine<MediaController?> { continuation ->
                    future.addListener({if(continuation.isActive)continuation.resume(runCatching{future.get()}.getOrNull())},ContextCompat.getMainExecutor(app))
                }
                controller?.pause()
            } finally {MediaController.releaseFuture(future)}
        }
        return Result.success()
    }
}
