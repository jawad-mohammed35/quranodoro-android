package com.jawadmohammed.quranodoro

import android.app.NotificationManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.jawadmohammed.quranodoro.data.*
import com.jawadmohammed.quranodoro.timer.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class RefinementTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(rule.activity)[AppViewModel::class.java]
    @Before fun prepare() {
        runBlocking {
            vm.prefs.bool("focusAudio", false)
            vm.prefs.bool("autoStart", false)
            vm.prefs.bool("timerPermissionAsked", true)
        }
    }
    @After fun cleanup() { vm.resetTimer() }

    @Test fun customTargetValidatesAndKeepsCount() {
        vm.resetTasbih()
        rule.waitUntil(5000) { vm.tasbih.value.count == 0 }
        rule.onNodeWithTag("tab-tasbih").performClick()
        repeat(8) { rule.onNodeWithText("Tap to count").performClick() }
        rule.waitUntil(5000) { vm.tasbih.value.count == 8 }
        rule.onNodeWithText("Custom dhikr").assertDoesNotExist()
        rule.onNodeWithText("Custom target").performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("0")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("99999")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("120")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5000) { vm.tasbih.value.target == 120 }
        assertEquals(8, vm.tasbih.value.count)
        rule.onNodeWithText("Custom target").performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("250")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(120, vm.tasbih.value.target)
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5000) { vm.tasbih.value.count == 8 && vm.tasbih.value.target == 120 }
    }

    @Test fun settingsHaveDedicatedPagesAndContextAwareHeader() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        rule.onNodeWithText("Theme").assertDoesNotExist()
        listOf("Appearance", "Focus & Notifications", "Tasbih", "Quran & Audio", "Local Progress", "About Quranodoro").forEach {
            rule.onNode(hasText(it) and hasAnyAncestor(hasScrollAction())).performScrollTo().performClick()
            rule.onNodeWithContentDescription("Settings").assertDoesNotExist()
            rule.onNodeWithContentDescription("Back").performClick()
            rule.onNodeWithText("Make it yours").assertIsDisplayed()
        }
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("Hijri date adjustment").performScrollTo().performClick()
        rule.onNodeWithText("+1 day(s)").performClick()
        rule.waitUntil(5000) { vm.preferences.value.hijriAdjustment == 1 }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText(hijriDate(java.time.LocalDate.now(), 1)).assertIsDisplayed()
        runBlocking { vm.prefs.int("hijriAdjustment", 0) }
    }

    private fun screenshot(name: String, dialog: Boolean = false) {
        rule.waitForIdle()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val folder = java.io.File(output ?: java.io.File(rule.activity.filesDir, "refinement-screenshots").absolutePath).apply { mkdirs() }
        val image = if (dialog && android.os.Build.VERSION.SDK_INT < 28)
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        else (if (dialog) rule.onNode(isDialog()) else rule.onRoot()).captureToImage().asAndroidBitmap()
        java.io.File(folder, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        if (dialog) {
            val full = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(folder, "full-$name.png").outputStream().use { full.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun compactScreensAndLargeTypeKeepImportantActionsReachable() {
        listOf("Light", "Dark", "OLED Black").forEach { theme ->
            runBlocking { vm.prefs.string("theme", theme) }
            rule.onNodeWithTag("tab-focus").performClick()
            rule.onNodeWithText("Start session").performScrollTo().assertIsDisplayed()
            screenshot("compact-focus-$theme")
            rule.onNodeWithText("Reset").performScrollTo().performClick()
            rule.onNodeWithText("Reset this session?").assertIsDisplayed()
            rule.onNodeWithText("Cancel").performScrollTo().performClick()
            rule.onNodeWithTag("tab-quran").performClick()
            rule.onNodeWithContentDescription("Play recitation").performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("tab-farm").performClick()
            rule.onNodeWithText("Plant Deglet Noor seed").performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("tab-tasbih").performClick()
            rule.onNodeWithText("Tap to count").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("Custom target").performScrollTo().performClick()
            rule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
            screenshot("compact-target-$theme", true)
            rule.onNodeWithText("Cancel").performScrollTo().performClick()
        }
    }

    @Test fun backgroundCompletionRemovesCountdownAndPostsReminder() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = rule.activity.applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33)
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
        val manager = context.getSystemService(NotificationManager::class.java)
        val model = vm
        runBlocking {
            vm.prefs.bool("notifications", true)
            vm.dao.save(TimerState(phase = "Short break", durationMs = 1800, remainingMs = 1800))
        }
        vm.toggleTimer()
        rule.waitUntil(5000) { manager.activeNotifications.any { it.id == TimerService.ID } }
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val deadline = System.currentTimeMillis() + 10000
        while (manager.activeNotifications.none { it.id == 10 } && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue(manager.activeNotifications.any { it.id == 10 })
        assertFalse(model.timer.value.running)
        assertEquals("Focus", model.timer.value.phase)
        val removalDeadline = System.currentTimeMillis() + 5000
        while (manager.activeNotifications.any { it.id == TimerService.ID } && System.currentTimeMillis() < removalDeadline) Thread.sleep(50)
        assertFalse(manager.activeNotifications.any { it.id == TimerService.ID })
        manager.cancel(10)
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        runBlocking { vm.prefs.bool("notifications", false) }
    }

    @Test fun activeNotificationPausesResumesStopsAndOpensFocus() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = rule.activity.applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        rule.onNodeWithTag("tab-focus").performClick()
        vm.resetTimer()
        rule.waitUntil(5000) { !vm.timer.value.running && vm.timer.value.remainingMs == vm.timer.value.durationMs }
        rule.onNodeWithText("Start session").performClick()
        fun posted() = manager.activeNotifications.firstOrNull { it.id == TimerService.ID }?.notification
        rule.waitUntil(5000) { posted()?.actions?.firstOrNull()?.title == "Pause" }
        assertTrue(posted()!!.extras.getBoolean("android.showChronometer"))
        assertTrue(posted()!!.extras.getBoolean("android.chronometerCountDown"))
        posted()!!.actions[0].actionIntent.send()
        rule.waitUntil(5000) { !vm.timer.value.running && posted()?.actions?.firstOrNull()?.title == "Resume" }
        val left = vm.timer.value.remainingMs
        posted()!!.actions[0].actionIntent.send()
        rule.waitUntil(5000) { vm.timer.value.running && posted()?.actions?.firstOrNull()?.title == "Pause" }
        assertTrue(vm.timer.value.endElapsed - SystemClock.elapsedRealtime() <= left)
        val beforeBackground = vm.left.value
        val model = vm
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 223")).use { it.readBytes() }
        Thread.sleep(1400)
        assertNotNull(posted())
        assertTrue(remaining(model.timer.value, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(context)) < beforeBackground)
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 224")).use { it.readBytes() }
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 82")).use { it.readBytes() }
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        rule.onNodeWithTag("tab-home").performClick()
        posted()!!.contentIntent.send()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("tab-focus").filter(isSelected()).fetchSemanticsNodes().isNotEmpty() }
        posted()!!.actions[1].actionIntent.send()
        rule.waitUntil(5000) { !vm.timer.value.running && posted() == null }
        assertEquals(vm.timer.value.durationMs, vm.timer.value.remainingMs)
    }
}
