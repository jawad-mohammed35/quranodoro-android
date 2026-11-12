package com.jawadmohammed.quranodoro

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.jawadmohammed.quranodoro.timer.notifyCompletion
import org.junit.Assert.*
import org.junit.Test

class NotificationTest {
    @Test
    fun completionNotificationUsesChannelAndOpensApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val needed =
            Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand(command),
                )
                .use { it.readBytes() }
        }
        try {
            if (needed)
                shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
            notifyCompletion(context, "Focus")
            val posted = manager.activeNotifications.firstOrNull { it.id == 10 }
            assertNotNull("Completion notification should be posted", posted)
            assertEquals("sessions", posted!!.notification.channelId)
            assertNotNull(posted.notification.contentIntent)
            assertNotNull(manager.getNotificationChannel("sessions"))
        } finally {
            manager.cancel(10)
            if (needed)
                shell("pm revoke ${context.packageName} android.permission.POST_NOTIFICATIONS")
        }
    }
}
