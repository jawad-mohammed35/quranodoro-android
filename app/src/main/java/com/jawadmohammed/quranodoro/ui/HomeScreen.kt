package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.*
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable fun HomeScreen(vm: AppViewModel,navigate: (String)->Unit) {
    val prefs by vm.preferences.collectAsStateWithLifecycle();val sessions by vm.sessions.collectAsStateWithLifecycle();val trees by vm.trees.collectAsStateWithLifecycle()
    var editing by rememberSaveableState {mutableStateOf(false)};var reset by remember{mutableStateOf(false)};var add by remember{mutableStateOf(false)}
    val bounds=remember {mutableStateMapOf<String,Rect>()};var dragging by remember{mutableStateOf<String?>(null)};var offset by remember{mutableStateOf(Offset.Zero)};var origin by remember{mutableStateOf(Offset.Zero)};var target by remember{mutableStateOf<String?>(null)}
    val order=prefs.order.filter{it !in prefs.hidden};val today=sessions.filter{localDate(it.completedAt)==LocalDate.now()}.sumOf{it.minutes}
    var reminderIndex by remember {val date=LocalDate.now();mutableStateOf((date.year*372+(date.monthValue-1)*31+date.dayOfMonth)%reminders.size)}
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns=if(maxWidth<340.dp)1 else if(maxWidth>720.dp)3 else 2
        LazyVerticalGrid(columns=GridCells.Fixed(columns),contentPadding=PaddingValues(20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item(span={GridItemSpan(maxLineSpan)}) {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    QText(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Heading("Assalamu Alaikum"+if(prefs.name.isBlank())"" else ", ${prefs.name}","Nurture your focus. Nourish your soul.")
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedButton(onClick={editing=!editing}) {Icon(if(editing)Icons.Filled.Check else Icons.Filled.Tune,null);Spacer(Modifier.width(6.dp));QText(if(editing)"Done" else "Edit Dashboard")};if(!editing)TextButton(onClick={navigate("focus")}){QText("Begin focus")}}
                    if(editing) {
                        QText("Hold a drag handle to move a widget. Expand or compact it using the resize button.",style=MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton(onClick={add=true}){QText("Add Widgets")};TextButton(onClick={reset=true}){QText("Reset Layout")}}
                    }
                }
            }
            items(order,key={it},span={GridItemSpan(if(it in prefs.wide)maxLineSpan else 1)}) { id->
                val isDragging=dragging==id
                Glass(Modifier.testTag("panel-$id").animateItem().onGloballyPositioned {bounds[id]=it.boundsInRoot()}.zIndex(if(isDragging)1f else 0f).graphicsLayer {translationX=if(isDragging)offset.x else 0f;translationY=if(isDragging)offset.y else 0f;alpha=if(isDragging).85f else 1f}.then(if(target==id)Modifier.border(2.dp,MaterialTheme.colorScheme.primary,androidx.compose.foundation.shape.RoundedCornerShape(26.dp)) else Modifier)) {
                    if(editing) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Filled.DragIndicator,"Hold to drag ${panels[id]}",Modifier.size(48.dp).pointerInput(id) {
                                detectDragGesturesAfterLongPress(onDragStart={dragging=id;offset=Offset.Zero;origin=bounds[id]?.center?:Offset.Zero},onDragCancel={dragging=null;target=null;offset=Offset.Zero},onDragEnd={target?.let{vm.movePanel(id,it)};dragging=null;target=null;offset=Offset.Zero},onDrag={change,amount->change.consume();offset+=amount;val point=origin+offset;target=bounds.entries.firstOrNull{it.key!=id && it.key in order && it.value.contains(point)}?.key})
                            })
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick={vm.hidePanel(id)}){Icon(Icons.Filled.Close,"Hide ${panels[id]}")}
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            IconButton(enabled=order.indexOf(id)>0,onClick={vm.movePanel(id,order[order.indexOf(id)-1])}){Icon(Icons.Filled.ArrowUpward,"Move ${panels[id]} up")}
                            IconButton(onClick={vm.resizePanel(id)}){Icon(if(id in prefs.wide)Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,if(id in prefs.wide)"Compact ${panels[id]}" else "Expand ${panels[id]}")}
                        }
                    }
                    QText(panels[id]?:id,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    when(id) {
                        "focus"->{QText("$today",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Light);QText("minutes of intention today");LinearProgressIndicator(progress={(today/120f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth());TextButton(onClick={navigate("focus")}){QText("Make time for focus")}}
                        "streak"->Stat("${streak(sessions)}","days of consistency")
                        "pomodoros"->Stat("${sessions.size}","completed sessions")
                        "minutes"->Stat("${sessions.sumOf{it.minutes}}","minutes of focus")
                        "orchard"->{Stat("${trees.size}","palms planted");TextButton(onClick={navigate("farm")}){QText("Visit farm")}}
                        "week"->if(id in prefs.wide || columns==1)WeeklySummary(sessions) else Stat("${sessions.filter{localDate(it.completedAt)>=LocalDate.now().minusDays(6)}.sumOf{it.minutes}}","minutes this week")
                        "tree"->{val tree=trees.firstOrNull{it.active};if(tree==null){PalmIllustration(0);QText("Plant your next intention")}else{val s=stage(tree.gp,treeTypes[tree.type]?:1.0);PalmIllustration(s);QText("${tree.type} · ${stages[s]}");QText("${tree.gp} growth points")};TextButton(onClick={navigate("farm")}){QText("Tend your farm")}}
                        "reciter"->{CompactPlayer(vm);TextButton(onClick={navigate("quran")}){QText("Choose a recitation")}}
                        "inspiration"->{val reminder=reminders[reminderIndex];QText(reminder.arabic,fontSize=25.sp,lineHeight=38.sp);QText(reminder.english);QText(reminder.reference,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);TextButton(onClick={reminderIndex=(reminderIndex+1+ kotlin.random.Random.nextInt(reminders.size-1))%reminders.size}){Icon(Icons.Filled.Refresh,null);QText("Show another reminder")}}
                    }
                }
            }
            item(span={GridItemSpan(maxLineSpan)}){QText("A peaceful space for meaningful progress.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))}
        }
    }
    if(reset)Confirm("Restore your dashboard?","Your widgets will return to their default order and size. Your progress stays safe.",{reset=false},{vm.run{vm.prefs.resetDashboard()}})
    if(add) androidx.compose.ui.window.Dialog(onDismissRequest={add=false}){Glass{Heading("Add Widgets");if(prefs.hidden.isEmpty())QText("All widgets are already on your dashboard.");prefs.hidden.forEach{id->TextButton(onClick={vm.showPanel(id)}){Icon(Icons.Filled.Add,null);QText(panels[id]?:id)}};TextButton(onClick={add=false}){QText("Done")}}}
}
@Composable private fun Stat(value: String,label: String) {QText(value,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Light);QText(label,style=MaterialTheme.typography.bodySmall)}
@Composable private fun <T> rememberSaveableState(init: ()->T): T = remember {init()}
