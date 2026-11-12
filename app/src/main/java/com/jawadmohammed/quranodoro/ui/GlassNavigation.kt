package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

val LocalBarInset = staticCompositionLocalOf { 0.dp }

@Composable
fun GlassNavigation(
    route: String,
    haptics: Boolean,
    backdrop: GraphicsLayer?,
    pagePosition: Offset,
    navigate: (Destination) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val oled = LocalOled.current
    val shape = RoundedCornerShape(26.dp)
    val feedback = LocalHapticFeedback.current
    val activate by rememberUpdatedState(navigate)
    var finger by remember { mutableStateOf<Float?>(null) }
    var barPosition by remember { mutableStateOf(Offset.Zero) }
    val selected = destinations.indexOfFirst { it.route == route }
    BoxWithConstraints(
        Modifier.navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .widthIn(max = 620.dp)
            .fillMaxWidth()
            .onGloballyPositioned { barPosition = it.positionInRoot() }
            .shadow(if (oled) 0.dp else 5.dp, shape)
            .clip(shape)
            .border(1.dp, colors.outlineVariant, shape)
            .testTag("tab-bar")
            .pointerInput(haptics) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    val cell = size.width.toFloat() / destinations.size
                    var last = -1
                    fun track(position: Offset) {
                        if (position.y !in 0f..size.height.toFloat()) return
                        val x = (position.x / cell).coerceIn(.5f, destinations.size - .5f)
                        finger = x
                        val index = x.toInt()
                        if (index != last) {
                            if (last >= 0 && haptics)
                                feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            last = index
                            activate(destinations[index])
                        }
                    }
                    try {
                        track(held.position)
                        do {
                            val event =
                                awaitPointerEvent(
                                    androidx.compose.ui.input.pointer.PointerEventPass.Initial,
                                )
                            val change = event.changes.firstOrNull { it.id == held.id } ?: break
                            if (change.isConsumed || event.changes.count { it.pressed } > 1) break
                            if (change.pressed) track(change.position)
                            change.consume()
                        } while (change.pressed)
                    } finally {
                        finger = null
                    }
                }
            },
    ) {
        if (backdrop != null) {
            Spacer(
                Modifier.matchParentSize().blur(12.dp).drawBehind {
                    val offset = pagePosition - barPosition
                    translate(offset.x, offset.y) { drawLayer(backdrop) }
                },
            )
        }
        Spacer(
            Modifier.matchParentSize()
                .background(if (oled) Color.Black else colors.surface.copy(alpha = .86f)),
        )
        val cell = maxWidth / destinations.size
        val target = finger ?: (selected + .5f)
        val center by
            animateFloatAsState(
                target,
                tween(if (finger == null) 180 else 65),
                label = "Tab position",
            )
        if (selected >= 0 || finger != null) {
            Spacer(
                Modifier.align(Alignment.CenterStart)
                    .offset(x = cell * (center - .5f))
                    .width(cell)
                    .height(44.dp)
                    .padding(horizontal = 3.dp)
                    .background(colors.primary.copy(alpha = if (oled) .08f else .14f), shape)
                    .border(1.dp, colors.primary.copy(alpha = .32f), shape),
            )
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                val active = selected == index
                val scale by
                    animateFloatAsState(
                        if (finger?.toInt() == index) 1.15f else 1f,
                        label = "Icon scale",
                    )
                Column(
                    Modifier.weight(1f)
                        .testTag("tab-${destination.route}")
                        .selectable(active, role = Role.Tab) { activate(destination) }
                        .heightIn(min = 58.dp)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Crossfade(active, animationSpec = tween(120), label = "Tab icon") { filled ->
                        Icon(
                            if (filled) destination.active else destination.inactive,
                            null,
                            Modifier.size(22.dp).scale(scale),
                            tint = if (filled) colors.primary else colors.onSurfaceVariant,
                        )
                    }
                    QText(
                        destination.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (active) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
