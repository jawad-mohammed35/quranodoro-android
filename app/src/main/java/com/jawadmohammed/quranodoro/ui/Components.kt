package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.glassSurface(shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun Heading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        QText(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        subtitle?.let {
            QText(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Glass(Modifier.fillMaxWidth()) {
        QText(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 840.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
fun Confirm(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    GlassDialog(onDismissRequest = onDismiss) {
        Glass(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            QText(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            QText(text, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { QText("Cancel") }
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                ) {
                    QText("Confirm")
                }
            }
        }
    }
}

@Composable
fun Toggle(title: String, subtitle: String? = null, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            QText(title)
            subtitle?.let {
                QText(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun <T> Picker(
    label: String,
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    text: (T) -> String = { it.toString() },
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            QText(label, style = MaterialTheme.typography.labelSmall)
            QText(text(selected), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    if (open)
        GlassDialog(onDismissRequest = { open = false }) {
            Glass(Modifier.heightIn(max = 580.dp)) {
                QText(label, style = MaterialTheme.typography.titleLarge)
                if (items.size > 10)
                    OutlinedTextField(
                        query,
                        { query = it },
                        label = { QText("Search") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                Column(Modifier.weight(1f, false).verticalScroll(rememberScrollState())) {
                    items
                        .filter { text(it).contains(query, true) }
                        .forEach { item ->
                            TextButton(
                                onClick = {
                                    onSelect(item)
                                    open = false
                                    query = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                QText(text(item), modifier = Modifier.fillMaxWidth())
                            }
                        }
                }
                TextButton(onClick = { open = false }) { QText("Close") }
            }
        }
}

fun time(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(java.util.Locale.ENGLISH, seconds / 60, seconds % 60)
}

/** Optical treatment shared by cards and chrome; OLED keeps a true black fill. */
@Composable
fun Modifier.glassSurface(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val c = MaterialTheme.colorScheme
    val oled = LocalOled.current
    return clip(shape)
        .background(if (oled) Color.Black else c.surface.copy(alpha = .80f))
        .background(Brush.linearGradient(listOf(
            if (oled) Color.Transparent else Color.White.copy(alpha = .12f),
            Color.Transparent,
            if (oled) Color.Transparent else c.primary.copy(alpha = .045f),
        )))
        .border(.8.dp, Brush.verticalGradient(listOf(
            if (oled) c.outlineVariant else c.surface.copy(alpha = .95f),
            c.outlineVariant.copy(alpha = .55f),
            c.primary.copy(alpha = .22f),
        )), shape)
}

/** Samples a separately recorded page layer; foreground text and icons stay sharp. */
@Composable
fun FrostedBackdrop(layer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    root: androidx.compose.ui.geometry.Offset, modifier: Modifier = Modifier) {
    var position by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Spacer(modifier.onGloballyPositioned { position = it.positionInRoot() }
        .blur(20.dp).drawBehind {
            translate(root.x - position.x, root.y - position.y) {
                drawLayer(layer)
            }
        })
}

/** Uses Android's compositor blur when supported, with the same readable fallback rim. */
@Composable
fun GlassDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val oled = LocalOled.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        val view = androidx.compose.ui.platform.LocalView.current
        val context = androidx.compose.ui.platform.LocalContext.current
        DisposableEffect(view, oled) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val enabled = android.os.Build.VERSION.SDK_INT >= 31 && !oled &&
                context.getSystemService(android.app.ActivityManager::class.java)?.isLowRamDevice != true
            if (enabled && android.os.Build.VERSION.SDK_INT >= 31) {
                window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window?.attributes = window.attributes.apply { blurBehindRadius = (16 * context.resources.displayMetrics.density).toInt() }
            }
            onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND) }
        }
        content()
    }
}
