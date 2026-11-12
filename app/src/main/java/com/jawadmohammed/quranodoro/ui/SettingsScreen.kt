package com.jawadmohammed.quranodoro.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable fun SettingsScreen(vm: AppViewModel,navigate: (String)->Unit) {
    val p by vm.preferences.collectAsStateWithLifecycle();val context=LocalContext.current;val scope=rememberCoroutineScope();var status by remember{mutableStateOf<String?>(null)}
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->vm.bool("notifications",granted);if(!granted)status="Notifications were not enabled. You can allow them in Android app settings."}
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch {
        runCatching {val text=vm.backup();kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(text.toByteArray())}?:error("Unable to open backup destination")}}.onSuccess{status="Progress backup saved."}.onFailure{status="Could not save backup. Please choose another destination."}
    }}
    Page {
        Heading("Make it yours","Preferences are saved automatically on this device.")
        Section("Appearance") {
            Picker("Theme",listOf("System","Light","Dark"),p.theme,onSelect={vm.string("theme",it)})
            Picker("Language",languages.keys.toList(),p.language,onSelect={vm.string("language",it)},text={languages[it]?:it})
            var name by remember(p.name){mutableStateOf(p.name)}
            OutlinedTextField(name,{name=it.take(80);vm.string("name",name)},label={QText("Your name (optional)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        }
        Section("Pomodoro") {
            Toggle("Auto-start next phase","While the app is open; background completion waits for your return",p.autoStart){vm.bool("autoStart",it)}
            Toggle("Quran during focus",value=p.focusAudio,onChange={vm.bool("focusAudio",it)})
            QText("Change durations or create a custom preset on the Focus tab.",style=MaterialTheme.typography.bodySmall)
        }
        Section("Notifications") {
            Toggle("Session completion reminders","Android may delay reminders in battery saving mode",p.notifications){enabled->if(enabled&&Build.VERSION.SDK_INT>=33)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.bool("notifications",enabled)}
            TextButton(onClick={context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))}){QText("Android notification settings")}
        }
        Section("Quran audio") {
            QText("Your reciter, Surah, volume and favorites are remembered. Playback supports media notifications, headset controls and screen locking.")
            TextButton(onClick={navigate("quran")}){QText("Recitation preferences")}
        }
        Section("Tasbih") {Toggle("Subtle haptic feedback",value=p.haptics,onChange={vm.bool("haptics",it)})}
        Section("Your local progress") {
            QText("Progress stays on this device. No account, ads, analytics or trackers. Uninstalling the app removes its data.")
            OutlinedButton(onClick={backup.launch("quranodoro-backup-${LocalDate.now()}.json")}){QText("Export progress backup")}
            QText("Exports preferences and progress as JSON. Import is not available in this version, matching the web backup feature.",style=MaterialTheme.typography.bodySmall)
        }
        status?.let {QText(it,color=MaterialTheme.colorScheme.primary)}
        Section("About Quranodoro") {
            QText("A peaceful Islamic productivity companion. Native Android edition 1.0.0 (1).")
            TextButton(onClick={navigate("about-farm")}){QText("About the Date Farm")}
            LinkButton("Quranodoro on the web","https://quranodoro.vercel.app/")
            LinkButton("Android source code","https://github.com/jawad-mohammed35/quranodoro-android")
        }
        Section("About the Developer") {QText("Created by Jawad Mohammed");TextButton(onClick={navigate("about")}){QText("Meet the developer")}}
    }
}
@Composable fun LinkButton(label: String,url: String) {val uri=androidx.compose.ui.platform.LocalUriHandler.current;TextButton(onClick={runCatching{uri.openUri(url)}}){QText(label)}}
@Composable fun AboutScreen() {
    Page {
        Heading("About the Developer","The person behind Quranodoro")
        Section("Jawad Mohammed") {QText("Miami, FL");QText("Master of Science in Computer Science\nFlorida International University")}
        Section("Experience") {
            QText("Testing Technician · RAZOR-Gov\nJune 2026 – Present")
            QText("Graduate Research Assistant · FIU EPSi Lab\nFebruary 2024 – December 2026")
            QText("Quality Assurance & IT Specialist · Wooo!\nApril 2025 – December 2025")
        }
        Section("Connect") {LinkButton("LinkedIn","https://www.linkedin.com/in/jawad-mohammed01/");LinkButton("jmoha027@fiu.edu","mailto:jmoha027@fiu.edu")}
    }
}
