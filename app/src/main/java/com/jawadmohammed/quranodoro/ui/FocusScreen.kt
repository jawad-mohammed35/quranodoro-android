package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.*

@Composable fun FocusScreen(vm: AppViewModel) {
    val t by vm.timer.collectAsStateWithLifecycle();val remaining by vm.left.collectAsStateWithLifecycle();val prefs by vm.preferences.collectAsStateWithLifecycle()
    var custom by remember {mutableStateOf(false)};var reset by remember {mutableStateOf(false)};var skip by remember {mutableStateOf(false)}
    var pendingPreset by remember {mutableStateOf<Preset?>(null)}
    Page {
        Heading("Make room for focus","A little intention. A little progress.")
        Glass(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){QText(t.phase,style=MaterialTheme.typography.titleMedium);QText("Round ${t.completed%t.cycles+1} / ${t.cycles}",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                val diameter=minOf(maxWidth,280.dp)
                Box(Modifier.size(diameter).padding(8.dp),contentAlignment=Alignment.Center) {
                    CircularProgressIndicator(progress={1f-(remaining.toFloat()/t.durationMs.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxSize(),strokeWidth=8.dp,trackColor=MaterialTheme.colorScheme.primaryContainer)
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        QText(time(remaining),fontSize=48.sp,fontWeight=FontWeight.Light)
                        QText(if(t.running) "One moment at a time" else "Whenever you're ready",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Button(onClick=vm::toggleTimer,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=CircleShape) {Icon(if(t.running)Icons.Filled.Pause else Icons.Filled.PlayArrow,null);Spacer(Modifier.width(8.dp));QText(if(t.running)"Pause" else if(remaining<t.durationMs)"Resume" else "Start session")}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {TextButton(onClick={reset=true}){Icon(Icons.Filled.RestartAlt,null);QText("Reset")};TextButton(onClick={skip=true}){QText("Skip");Icon(Icons.Filled.SkipNext,null)}}
        }
        Section("Your rhythm") {
            val all=presets+vm.customPresets()
            Picker("Timer preset",all,all.find {it.name==t.presetName}?:Preset(t.presetName,t.focus,t.shortBreak,t.longBreak,t.cycles),onSelect={if(t.running||remaining<t.durationMs)pendingPreset=it else vm.selectPreset(it)},text={it.name})
            QText("${t.focus} min focus · ${t.shortBreak} min short break · ${t.longBreak} min long break",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={custom=true}){Icon(Icons.Filled.Add,null);QText("Create custom preset")}
            vm.customPresets().forEach { p -> Row(verticalAlignment=Alignment.CenterVertically) {QText(p.name,Modifier.weight(1f));IconButton(onClick={vm.removePreset(p)}){Icon(Icons.Filled.DeleteOutline,"Delete ${p.name}")}} }
        }
        Section("A calm companion") {
            Toggle("Quran during focus","Recitation follows your focus controls",prefs.focusAudio){vm.bool("focusAudio",it)}
            CompactPlayer(vm)
        }
        QText("Your timer uses elapsed time and restores after reopening. Completion reminders may be delayed by Android battery restrictions.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(reset) Confirm("Reset this session?","This unfinished session will not earn growth points.",{reset=false},vm::resetTimer)
    if(skip) Confirm("Skip this phase?","Move to the next phase without awarding focus points.",{skip=false},vm::skip)
    pendingPreset?.let { p -> Confirm("Change your rhythm?","Changing presets resets the current session.",{pendingPreset=null},{vm.selectPreset(p)}) }
    if(custom) PresetDialog({custom=false}){vm.addPreset(it);custom=false}
}
@Composable private fun PresetDialog(onDismiss: ()->Unit,onSave: (Preset)->Unit) {
    var name by remember {mutableStateOf("")};var focus by remember {mutableStateOf("25")};var short by remember {mutableStateOf("5")};var long by remember {mutableStateOf("15")};var cycles by remember {mutableStateOf("4")}
    Dialog(onDismissRequest=onDismiss) {Glass(Modifier.heightIn(max=620.dp).verticalScroll(rememberScrollState())){
        Heading("Your own rhythm")
        OutlinedTextField(name,{name=it.take(60)},label={QText("Preset name")},singleLine=true,modifier=Modifier.fillMaxWidth())
        listOf(Triple("Focus minutes",focus,{s:String->focus=s}),Triple("Short break minutes",short,{s:String->short=s}),Triple("Long break minutes",long,{s:String->long=s}),Triple("Focus sessions per cycle",cycles,{s:String->cycles=s})).forEach { (label,value,change) ->
            OutlinedTextField(value,{change(it.filter(Char::isDigit).take(3))},label={QText(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number))
        }
        val valid=name.isNotBlank()&&listOf(focus,short,long,cycles).all{(it.toIntOrNull()?:0) in 1..240}
        QText("Enter values from 1 to 240.",style=MaterialTheme.typography.bodySmall)
        Row{TextButton(onClick=onDismiss){QText("Cancel")};Button(enabled=valid,onClick={onSave(Preset(name.trim(),focus.toInt(),short.toInt(),long.toInt(),cycles.toInt()))}){QText("Save preset")}}
    }}
}
