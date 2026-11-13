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
            // Simulate an installation that previously selected Arabic.
            vm.prefs.string("language", "ar")
            vm.prefs.string("theme", "Light")
            vm.prefs.bool("focusAudio", false)
            vm.prefs.bool("timerPermissionAsked", true)
            vm.prefs.int("reciter", 0)
            vm.prefs.int("surah", 1)
        }
        rule.waitForIdle()
    }

    @Test
    fun holdAndSlideTracksTabAndCancelsCleanly() {
        val bar = rule.onNodeWithTag("tab-bar")
        bar.performTouchInput {
            down(percentOffset(.1f, .5f))
            advanceEventTime(40)
            moveTo(percentOffset(.3f, .5f))
            moveTo(percentOffset(.7f, .5f))
            up()
        }
        rule.onNodeWithTag("tab-farm").assertIsSelected()
        rule.onNodeWithText("Your date farm").assertIsDisplayed()
        bar.performTouchInput {
            down(percentOffset(.7f, .5f))
            advanceEventTime(800)
            moveTo(percentOffset(.9f, .5f))
            cancel()
        }
        rule.onNodeWithTag("tab-tasbih").assertIsSelected()
        rule.onNodeWithTag("tab-focus").performTouchInput { click() }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("tab-focus").assertIsSelected()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("tab-home").assertIsSelected()
    }

    @Test
    fun oledRoutesAndPickerUseBlackSurfaces() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Language").assertDoesNotExist()
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("Theme").performClick()
        rule.onNodeWithText("OLED Black").performClick()
        rule.waitUntil(5000) { vm.preferences.value.theme == "OLED Black" }
        rule.activityRule.scenario.recreate()
        fun inspect(name: String, heading: String) {
            rule.waitUntil(5000) {
                rule.onAllNodesWithText(heading).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText(heading).performScrollTo().assertIsDisplayed()
            rule.mainClock.advanceTimeBy(800)
            rule.waitForIdle()
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            // Sample a grid across the body, excluding the app and system bars.
            val pixels =
                (1..19).flatMap { x ->
                    (2..17).map { y ->
                        bitmap.getPixel(bitmap.width * x / 20, bitmap.height * y / 20)
                    }
                }
            Assert.assertTrue(
                "$name should have a predominantly pure-black body",
                pixels.count { it == android.graphics.Color.BLACK } > pixels.size / 2,
            )
            screenshot("oled-$name")
        }
        inspect("appearance", "Appearance")
        rule.onNodeWithText("Theme").performClick()
        // API 26/27 captures the entire device window, while later versions can
        // capture the dialog alone. One center pixel can land on a text glyph or
        // a not-yet-presented system frame. Check the central surface region once
        // the window has rendered, keeping the assertion on actual black pixels.
        rule.waitUntil(5000) {
            val dialog = captureDialog()
            val pixels = (2..8).flatMap { x -> (4..6).map { y ->
                dialog.getPixel(dialog.width * x / 10, dialog.height * y / 10)
            } }
            pixels.count { it == android.graphics.Color.BLACK } > pixels.size * 2 / 3
        }
        rule.onNodeWithText("Close").performClick()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("About the Developer").performScrollTo().performClick()
        inspect("developer", "About the Developer")
        rule.onNodeWithContentDescription("Analytics").performClick()
        inspect("analytics", "A little progress, every day")
        mapOf(
                "home" to "Assalamu Alaikum",
                "focus" to "Make room for focus",
                "quran" to "Listen & reflect",
                "tasbih" to "Remember, gently",
                "farm" to "Your date farm",
            )
            .forEach { (route, heading) ->
                rule.onNodeWithTag("tab-$route").performClick()
                inspect(route, heading)
            }
        rule.onNodeWithText("How the farm grows").performScrollTo().performClick()
        inspect("about-farm", "Small sessions, lasting roots")
    }

    @Test
    fun oledNestedControls() {
        runBlocking { vm.prefs.string("theme", "OLED Black") }
        fun dialog(trigger: String, close: String) {
            rule.onNodeWithText(trigger).performScrollTo().performClick()
            val root = rule.onNode(isDialog())
            root.assertIsDisplayed()
            saveScreenshot(
                "dialog-" + trigger.replace(' ', '-'),
                captureDialog(),
            )
            val dismiss = rule.onAllNodesWithText(close).onLast()
            if (trigger == "Create custom preset") dismiss.performScrollTo()
            dismiss.assertIsDisplayed().performClick()
            rule.waitUntil(5000) { rule.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        }
        rule.onNodeWithText("Edit Dashboard").performClick()
        dialog("Add Widgets", "Done")
        dialog("Reset Layout", "Cancel")
        rule.onNodeWithTag("tab-focus").performClick()
        dialog("Timer preset", "Close")
        dialog("Create custom preset", "Cancel")
        dialog("Reset", "Cancel")
        dialog("Skip", "Cancel")
        rule.onNodeWithTag("tab-quran").performClick()
        dialog("Reciter", "Close")
        dialog("Surah", "Close")
        rule.onNodeWithTag("tab-tasbih").performClick()
        dialog("Dhikr", "Close")
        dialog("Reset counter", "Cancel")
        rule.onNodeWithTag("tab-farm").performClick()
        dialog("Date variety", "Close")
        dialog("Plant Deglet Noor seed", "Cancel")
    }

    private fun captureDialog(): Bitmap {
        rule.waitForIdle()
        // Compose cannot capture a dialog window on API 26 or 27.
        if (android.os.Build.VERSION.SDK_INT < 28) {
            return InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        }
        return rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
    }

    private fun screenshot(name: String) =
        saveScreenshot(name, rule.onRoot().captureToImage().asAndroidBitmap())

    private fun saveScreenshot(name: String, image: Bitmap) {
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
    fun compactDashboardWidgetsShareTheirRow() {
        runBlocking { vm.prefs.resetDashboard() }
        rule.waitUntil(5000) {
            vm.preferences.value.order.first() == "focus" &&
                vm.preferences.value.hidden.isEmpty() &&
                "focus" in vm.preferences.value.wide
        }
        rule.waitForIdle()
        rule.onNodeWithTag("panel-streak").performScrollTo()
        if (rule.activity.resources.configuration.screenWidthDp >= 340) {
            val first = rule.onNodeWithTag("panel-streak").fetchSemanticsNode().boundsInRoot
            val second = rule.onNodeWithTag("panel-pomodoros").fetchSemanticsNode().boundsInRoot
            Assert.assertEquals(
                "Compact cards should occupy the same row",
                first.top,
                second.top,
                2f,
            )
            Assert.assertTrue("Compact cards must not overlap", second.left >= first.right)
        }
    }

    @Test
    fun settingsThemeAndBackNavigation() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("Theme").performScrollTo().performClick()
        rule.onNodeWithText("Dark").performClick()
        rule.waitUntil(5000) { vm.preferences.value.theme == "Dark" }
        rule.waitForIdle()
        screenshot("settings-dark")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Make it yours").assertIsDisplayed()
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
