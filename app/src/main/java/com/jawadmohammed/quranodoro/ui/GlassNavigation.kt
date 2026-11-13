package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
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
    val shape = RoundedCornerShape(32.dp)
    val feedback = LocalHapticFeedback.current
    val activate by rememberUpdatedState(navigate)
    val currentRoute by rememberUpdatedState(route)
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
            .border(.8.dp, Brush.verticalGradient(listOf(if (oled) colors.outlineVariant else Color.White.copy(alpha = .85f), colors.primary.copy(alpha = .22f))), shape)
            .testTag("tab-bar")
            .pointerInput(haptics) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Taps remain child selectables. Horizontal movement claims the gesture
                    // after touch slop; a vertical exit cancels without changing tabs.
                    val held = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        ?: return@awaitEachGesture
                    val cell = size.width.toFloat() / destinations.size
                    var last = destinations.indexOfFirst { it.route == currentRoute }
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
                            if (event.changes.count { it.pressed } > 1 || change.position.y !in 0f..size.height.toFloat()) break
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
                    // Sample the page edge while keeping the interactive scroll viewport clear.
                    translate(pagePosition.x - barPosition.x, pagePosition.y - barPosition.y) {
                        drawLayer(backdrop)
                    }
                },
            )
        }
        Spacer(
            Modifier.matchParentSize()
                .background(if (oled) Color.Black else colors.surface.copy(alpha = if (backdrop == null) .96f else .62f)),
        )
        val cell = maxWidth / destinations.size
        val target = finger ?: (selected + .5f)
        val center by
            animateFloatAsState(
                target,
                spring(dampingRatio = .82f, stiffness = if (finger == null) 520f else 1400f),
                label = "Tab position",
            )
        val lensHeight by animateDpAsState(if (finger == null) 52.dp else 58.dp, spring(dampingRatio = .75f), label = "Glass lens")
        if (selected >= 0 || finger != null) {
            Spacer(
                Modifier.align(Alignment.CenterStart)
                    .offset(x = cell * (center - .5f))
                    .width(cell)
                    .height(lensHeight)
                    .padding(horizontal = 3.dp)
                    .background(Brush.linearGradient(listOf(
                        colors.primary.copy(alpha = if (oled) .05f else .22f),
                        if (oled) Color.Black else colors.surface.copy(alpha = .55f),
                        colors.primary.copy(alpha = if (oled) .04f else .12f),
                    )), shape)
                    .border(1.dp, colors.primary.copy(alpha = .32f), shape),
            )
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                val active = selected == index
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by
                    animateFloatAsState(
                        if (finger?.toInt() == index || pressed) 1.20f else if (active) 1.06f else 1f,
                        animationSpec = spring(dampingRatio = .65f, stiffness = 650f),
                        label = "Icon scale",
                    )
                Column(
                    Modifier.weight(1f)
                        .testTag("tab-${destination.route}")
                        .selectable(selected = active, interactionSource = interaction, indication = null, role = Role.Tab, onClick = { activate(destination) })
                        .heightIn(min = 66.dp)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Crossfade(active, animationSpec = tween(120), label = "Tab icon") { filled ->
                        Icon(
                            if (filled) destination.active else destination.inactive,
                            null,
                            Modifier.size(if (destination.route in listOf("focus", "farm")) 26.dp else 22.dp).scale(scale),
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
