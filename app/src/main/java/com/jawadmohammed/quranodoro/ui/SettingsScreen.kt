package com.jawadmohammed.quranodoro.ui
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
@Composable
fun SettingsScreen(vm: AppViewModel, navigate: (String) -> Unit, section: String? = null) {
    val p by vm.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.bool("notifications", granted)
            if (!granted)
                status =
                    "Notifications were not enabled. You can allow them in Android app settings."
        }
    val backup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching {
                        val text = vm.backup()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(text.toByteArray())
                            } ?: error("Unable to open backup destination")
                        }
                    }
                        .onSuccess { status = "Progress backup saved." }
                        .onFailure {
                            status = "Could not save backup. Please choose another destination."
                        }
                }
        }
    Page {
        if (section == null) {
            Heading("Make it yours", "A quieter app, shaped around you.")
            Glass(Modifier.fillMaxWidth()) {
                SettingsRow("Appearance", "Theme, personal details & Hijri date", Icons.Outlined.Palette) { navigate("settings/appearance") }
                SettingsRow("Focus & Notifications", "Your sessions, rhythm & reminders", Icons.Outlined.Timer) { navigate("settings/focus") }
                SettingsRow("Tasbih", "Touch feedback & daily practice", Icons.Outlined.Spa) { navigate("settings/tasbih") }
                SettingsRow("Quran & Audio", "Recitation & background listening", Icons.Outlined.Headphones) { navigate("settings/audio") }
                SettingsRow("Local Progress", "Your saved progress & backup", Icons.Outlined.Storage) { navigate("settings/progress") }
            }
            Glass(Modifier.fillMaxWidth()) {
                SettingsRow("About Quranodoro", "Version & project information", Icons.Outlined.Info) { navigate("settings/about") }
                SettingsRow("About the Developer", "Jawad Mohammed", Icons.Outlined.Person) { navigate("about") }
            }
        }
        if (section == "appearance") Section("Appearance") {
            Picker(
                "Theme",
                listOf("Light", "Dark", "OLED Black", "System"),
                p.theme,
                onSelect = { vm.string("theme", it) },
                text = { if (it == "System") "System Default" else it },
            )
            Picker("Hijri date adjustment", (-2..2).toList(), p.hijriAdjustment,
                onSelect = { vm.number("hijriAdjustment", it) },
                text = { if (it == 0) "Calculated date · no adjustment" else "%+d day(s)".format(it) })
            QText("Offline Umm al-Qura calendar. Adjust for your local moon sighting.", style = MaterialTheme.typography.bodySmall)
            var name by remember(p.name) { mutableStateOf(p.name) }
            OutlinedTextField(
                name,
                {
                    name = it.take(80)
                    vm.string("name", name)
                },
                label = { QText("Your name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (section == "focus") Section("Pomodoro") {
            Toggle(
                "Auto-start next phase",
                "While the app is open; background completion waits for your return",
                p.autoStart,
            ) {
                vm.bool("autoStart", it)
            }
            Toggle(
                "Quran during focus",
                value = p.focusAudio,
                onChange = { vm.bool("focusAudio", it) },
            )
            QText(
                "Change durations or create a custom preset on the Focus tab.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (section == "focus") Section("Notifications") {
            Toggle(
                "Session completion reminders",
                "Ongoing countdowns appear when a timer starts; this enables completion alerts",
                p.notifications,
            ) { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= 33)
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.bool("notifications", enabled)
            }
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(
                                android.provider.Settings.EXTRA_APP_PACKAGE,
                                context.packageName,
                            ),
                    )
                },
            ) {
                QText("Android notification settings")
            }
        }
        if (section == "audio") Section("Quran audio") {
            QText(
                "Your reciter, Surah, volume and favorites are remembered. Playback supports media notifications, headset controls and screen locking.",
            )
            TextButton(onClick = { navigate("quran") }) { QText("Recitation preferences") }
        }
        if (section == "tasbih") Section("Tasbih") {
            Toggle(
                "Subtle haptic feedback",
                value = p.haptics,
                onChange = { vm.bool("haptics", it) },
            )
        }
        if (section == "progress") Section("Your local progress") {
            QText(
                "Progress stays on this device. No account, ads, analytics or trackers. Uninstalling the app removes its data.",
            )
            OutlinedButton(
                onClick = { backup.launch("quranodoro-backup-${LocalDate.now()}.json") },
            ) {
                QText("Export progress backup")
            }
            QText(
                "Exports preferences and progress as JSON. Import is not available in this version, matching the web backup feature.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        status?.let { QText(it, color = MaterialTheme.colorScheme.primary) }
        if (section == "about") Section("About Quranodoro") {
            QText(
                "A peaceful Islamic productivity companion. Native Android edition ${com.jawadmohammed.quranodoro.BuildConfig.VERSION_NAME} (${com.jawadmohammed.quranodoro.BuildConfig.VERSION_CODE}).",
            )
            TextButton(onClick = { navigate("about-farm") }) { QText("About the Date Farm") }
            LinkButton("Quranodoro on the web", "https://quranodoro.vercel.app/")
            LinkButton(
                "Android source code",
                "https://github.com/jawad-mohammed35/quranodoro-android",
            )
        }
        if (section == "about") Section("About the Developer") {
            QText("Created by Jawad Mohammed")
            TextButton(onClick = { navigate("about") }) { QText("Meet the developer") }
        }
    }
}
@Composable
fun LinkButton(label: String, url: String) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    TextButton(onClick = { runCatching { uri.openUri(url) } }) { QText(label) }
}
@Composable
fun AboutScreen() {
    Page {
        Heading("About the Developer", "The person behind Quranodoro")
        Section("Jawad Mohammed") {
            QText("Miami, FL")
            QText("Master of Science in Computer Science\nFlorida International University")
        }
        Section("Experience") {
            QText("Testing Technician · RAZOR-Gov\nJune 2026 – Present")
            QText("Graduate Research Assistant · FIU EPSi Lab\nFebruary 2024 – December 2026")
            QText("Quality Assurance & IT Specialist · Wooo!\nApril 2025 – December 2025")
        }
        Section("Connect") {
            LinkButton("LinkedIn", "https://www.linkedin.com/in/jawad-mohammed01/")
            LinkButton("jmoha027@fiu.edu", "mailto:jmoha027@fiu.edu")
        }
    }
}
@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            QText(title, style = MaterialTheme.typography.titleMedium)
            QText(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp))
    }
}
