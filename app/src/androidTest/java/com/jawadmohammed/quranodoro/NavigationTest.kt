package com.jawadmohammed.quranodoro

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*

class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val vm
        get() = ViewModelProvider(rule.activity)[AppViewModel::class.java]

    @Before
    fun english() {
        runBlocking {
            vm.prefs.string("language", "en")
            vm.prefs.string("theme", "Light")
            vm.prefs.bool("focusAudio", false)
        }
        rule.waitForIdle()
    }

    private fun screenshot(name: String) {
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory =
            File(output ?: File(rule.activity.filesDir, "screenshots").absolutePath).apply {
                mkdirs()
            }
        File(directory, "$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun allPrimaryDestinationsLaunch() {
        rule.onNodeWithText("Assalamu Alaikum").assertIsDisplayed()
        screenshot("home")
        rule.onNodeWithTag("tab-focus").performClick()
        rule.onNodeWithText("25:00").assertIsDisplayed()
        screenshot("focus")
        rule.onNodeWithTag("tab-quran").performClick()
        rule.onNodeWithText("الفاتحة").assertIsDisplayed()
        screenshot("quran")
        rule.onNodeWithTag("tab-tasbih").performClick()
        rule.onNodeWithText("Tap to count").assertIsDisplayed()
        screenshot("tasbih")
        rule.onNodeWithTag("tab-farm").performClick()
        rule.onNodeWithText("Plant an intention").assertIsDisplayed()
        screenshot("farm")
    }

    @Test
    fun targetSwitchPreservesTapsAndRecreation() {
        vm.resetTasbih()
        rule.waitUntil(5000) { vm.tasbih.value.count == 0 }
        rule.onNodeWithTag("tab-tasbih").performClick()
        repeat(5) { rule.onNodeWithText("Tap to count").performClick() }
        rule.waitUntil(5000) { vm.tasbih.value.count == 5 }
        rule.onNodeWithText("99").performScrollTo().performClick()
        rule.waitUntil(5000) { vm.tasbih.value.target == 99 }
        Assert.assertEquals(5, vm.tasbih.value.count)
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5000) { vm.tasbih.value.count == 5 && vm.tasbih.value.target == 99 }
    }

    @Test
    fun dashboardHideRestoreResizeAndReset() {
        runBlocking { vm.prefs.resetDashboard() }
        rule.waitForIdle()
        rule.onNodeWithText("Edit Dashboard").performClick()
        rule.onNodeWithContentDescription("Compact Today's focus").performScrollTo().performClick()
        rule.waitUntil(5000) { "focus" !in vm.preferences.value.wide }
        rule.onNodeWithContentDescription("Hide Today's focus").performScrollTo().performClick()
        rule.waitUntil(5000) { "focus" in vm.preferences.value.hidden }
        rule.onNodeWithText("Add Widgets").performScrollTo().performClick()
        rule.onNodeWithText("Today's focus").performClick()
        rule.onAllNodesWithText("Done").onLast().performClick()
        rule.waitUntil(5000) { "focus" !in vm.preferences.value.hidden }
        rule.onNodeWithText("Reset Layout").performScrollTo().performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(5000) { "focus" in vm.preferences.value.wide }
    }

    @Test
    fun timerPauseAndRecreationRetainState() {
        vm.resetTimer()
        rule.onNodeWithTag("tab-focus").performClick()
        rule.onNodeWithText("Start session").performScrollTo().performClick()
        rule.waitUntil(5000) { vm.timer.value.running }
        val before = vm.left.value
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5000) { vm.timer.value.running && vm.left.value < before }
        rule.onNodeWithText("Pause").performScrollTo().performClick()
        rule.waitUntil(5000) { !vm.timer.value.running }
        val paused = vm.timer.value.remainingMs
        Assert.assertTrue(paused in 1 until 1500000)
        vm.resetTimer()
    }

    @Test
    fun darkThemePersists() {
        runBlocking { vm.prefs.string("theme", "Dark") }
        rule.waitUntil(5000) { vm.preferences.value.theme == "Dark" }
        rule.waitForIdle()
        screenshot("home-dark")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5000) { vm.preferences.value.theme == "Dark" }
    }

    @Test
    fun settingsThemeAndBackNavigation() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Theme").performScrollTo().performClick()
        rule.onNodeWithText("Dark").performClick()
        rule.waitUntil(5000) { vm.preferences.value.theme == "Dark" }
        rule.waitForIdle()
        screenshot("settings-dark")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Assalamu Alaikum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Analytics").performClick()
        rule.onNodeWithText("Monthly focus").performScrollTo().assertIsDisplayed()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Assalamu Alaikum").assertIsDisplayed()
    }

    @Test
    fun dashboardDragReordersWidgets() {
        runBlocking {
            vm.prefs.resetDashboard()
            vm.prefs.string(
                "order",
                "streak,pomodoros,focus,minutes,orchard,week,tree,reciter,inspiration",
            )
        }
        rule.waitUntil(5000) { vm.preferences.value.order.first() == "streak" }
        rule.waitForIdle()
        rule.onNodeWithText("Edit Dashboard").performClick()
        rule.onNodeWithTag("panel-pomodoros").performScrollTo()
        val first = rule.onNodeWithTag("panel-streak").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithTag("panel-pomodoros").fetchSemanticsNode().boundsInRoot
        val delta = first.center - second.center
        screenshot("edit-before-drag")
        rule.onNodeWithContentDescription("Hold to drag Total Pomodoros").performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(delta)
            advanceEventTime(200)
            up()
        }
        screenshot("edit-after-drag")
        rule.waitUntil(5000) { vm.preferences.value.order.first() == "pomodoros" }
        runBlocking { vm.prefs.resetDashboard() }
    }
}
