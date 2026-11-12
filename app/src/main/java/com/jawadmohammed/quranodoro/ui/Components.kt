package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog

@Composable fun Glass(modifier: Modifier=Modifier,content: @Composable ColumnScope.()->Unit) {
    val c=MaterialTheme.colorScheme
    Surface(modifier.animateContentSize(),shape=RoundedCornerShape(26.dp),color=c.surface.copy(alpha=.93f),border=BorderStroke(1.dp,Brush.verticalGradient(listOf(c.outlineVariant.copy(alpha=.6f),c.outlineVariant))),shadowElevation=2.dp) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun Heading(title: String,subtitle: String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        QText(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold)
        subtitle?.let { QText(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable fun Section(title: String,content: @Composable ColumnScope.()->Unit) {
    Glass(Modifier.fillMaxWidth()) { QText(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);content() }
}
@Composable fun Page(content: @Composable ColumnScope.()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=840.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
    }
}
@Composable fun Confirm(title: String,text: String,onDismiss: ()->Unit,onConfirm: ()->Unit) {
    Dialog(onDismissRequest=onDismiss) { Glass {
        Heading(title);QText(text)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) { TextButton(onClick=onDismiss){QText("Cancel")};Button(onClick={onConfirm();onDismiss()}){QText("Confirm")} }
    } }
}
@Composable fun Toggle(title: String,subtitle: String?=null,value: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { QText(title);subtitle?.let { QText(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } };Switch(checked=value,onCheckedChange=onChange)
    }
}
@Composable fun <T> Picker(label: String,items: List<T>,selected: T,onSelect: (T)->Unit,text: (T)->String={it.toString()}) {
    var open by remember { mutableStateOf(false) }; var query by remember { mutableStateOf("") }
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(vertical=6.dp)) {QText(label,style=MaterialTheme.typography.labelSmall);QText(text(selected),style=MaterialTheme.typography.bodyLarge)} }
    if(open) Dialog(onDismissRequest={open=false}) { Glass(Modifier.heightIn(max=580.dp)) {
        QText(label,style=MaterialTheme.typography.titleLarge)
        if(items.size>10) OutlinedTextField(query,{query=it},label={QText("Search")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState())) { items.filter { text(it).contains(query,true) }.forEach { item -> TextButton(onClick={onSelect(item);open=false;query=""},modifier=Modifier.fillMaxWidth()) { QText(text(item),modifier=Modifier.fillMaxWidth()) } } }
        TextButton(onClick={open=false}) { QText("Close") }
    } }
}
fun time(ms: Long): String { val seconds=(ms.coerceAtLeast(0)+999)/1000;return "%02d:%02d".format(seconds/60,seconds%60) }
