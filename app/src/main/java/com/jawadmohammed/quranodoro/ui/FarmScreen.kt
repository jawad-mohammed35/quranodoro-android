package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.*
import kotlin.math.*

@Composable
fun PalmIllustration(stage: Int, modifier: Modifier = Modifier) {
    val growth by animateFloatAsState(stage.toFloat(), label = "palm growth")
    Canvas(modifier.height(180.dp).fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val ground = h * .84f
        drawOval(
            Brush.radialGradient(listOf(Color(0x33536E49), Color.Transparent)),
            Offset(cx - w * .3f, ground - 5),
            Size(w * .6f, 26f),
        )
        drawOval(
            Brush.verticalGradient(listOf(Color(0xFFE9DBC0), Color(0xFFBCAE8C))),
            Offset(cx - w * .32f, ground - 12),
            Size(w * .64f, 28f),
        )
        if (stage == 0) {
            // A close view makes even the first planted intention feel tangible.
            val pit = h * .23f
            val center = Offset(cx, ground - pit * .55f)
            rotate(-24f, center) {
                drawOval(Brush.horizontalGradient(listOf(Color(0xFF775037), Color(0xFFD0AD74), Color(0xFF927044)), cx - pit * .30f, cx + pit * .30f),
                    Offset(cx - pit * .30f, center.y - pit * .50f), Size(pit * .60f, pit))
                drawLine(Color(0xFF6F5035), Offset(cx, center.y - pit * .34f), Offset(cx + pit * .05f, center.y + pit * .34f), pit * .055f, StrokeCap.Round)
                drawLine(Color(0x66F5DEAC), Offset(cx - pit * .13f, center.y - pit * .20f), Offset(cx - pit * .12f, center.y + pit * .20f), pit * .035f, StrokeCap.Round)
            }
            return@Canvas
        }
        val top = ground - h * (.16f + growth * .08f)
        val trunkWidth = 8f + growth * 2f
        val trunk =
            Path().apply {
                moveTo(cx - trunkWidth, ground)
                quadraticTo(cx + 6, top + h * .3f, cx - 2, top)
                lineTo(cx + trunkWidth * .5f, top)
                quadraticTo(cx + trunkWidth + 12, top + h * .3f, cx + trunkWidth, ground)
                close()
            }
        drawPath(
            trunk,
            Brush.horizontalGradient(
                listOf(Color(0xFF8A7156), Color(0xFFC8AD85), Color(0xFF9B8060)),
                cx - trunkWidth,
                cx + trunkWidth,
            ),
        )
        val leafCount = if (stage < 3) 4 else 8
        val leafLength = h * (.15f + growth * .025f)
        for (i in 0 until leafCount) {
            val a = (PI * (.1 + i.toDouble() / (leafCount - 1) * .8))
            val ex = cx + cos(a).toFloat() * leafLength
            val ey = top - sin(a).toFloat() * leafLength * .55f
            val leaf =
                Path().apply {
                    moveTo(cx, top)
                    quadraticTo(
                        cx + (ex - cx) * .5f,
                        top - leafLength * .7f,
                        ex,
                        ey + leafLength * .2f,
                    )
                    quadraticTo(cx + (ex - cx) * .6f, top - leafLength * .12f, cx, top)
                    close()
                }
            drawPath(
                leaf,
                Brush.verticalGradient(
                    listOf(Color(0xFFA6C59A), Color(0xFF507B5B)),
                    top - leafLength,
                    top + 16,
                ),
            )
        }
        if (stage >= 5)
            for (i in 0..5) {
                drawOval(
                    Brush.verticalGradient(listOf(Color(0xFFE0B67B), Color(0xFF99723C))),
                    Offset(cx + (i - 3) * 8f, top + 14 + (i % 2) * 10),
                    Size(12f, 18f),
                )
            }
        if (stage == 6)
            for (i in 0..4) {
                val x = cx - 70 + i * 35
                val y = top - 40 + (i % 2) * 30
                drawLine(Color(0xFFC5B974), Offset(x - 5, y), Offset(x + 5, y), 2f)
                drawLine(Color(0xFFC5B974), Offset(x, y - 5), Offset(x, y + 5), 2f)
            }
    }
}

@Composable
fun FarmScreen(vm: AppViewModel, onAbout: () -> Unit) {
    val trees by vm.trees.collectAsStateWithLifecycle()
    var type by remember { mutableStateOf("Deglet Noor") }
    var plant by remember { mutableStateOf(false) }
    val featured = trees.firstOrNull { it.active } ?: trees.lastOrNull()
    Page {
        Heading("Your date farm", "Small moments of focus. Roots that last.")
        Glass(Modifier.fillMaxWidth().animateContentSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    QText(if (featured == null) "YOUR FIRST SEED" else "YOUR GROWING INTENTION",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    QText(featured?.type ?: "A little focus, a little life", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onAbout) { Icon(Icons.Outlined.Info, "How the farm grows") }
            }
            val stage = featured?.let { stage(it.gp, treeTypes[it.type] ?: 1.0) } ?: 0
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
                Brush.radialGradient(listOf(if (LocalOled.current) Color.Black else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f), Color.Transparent)))) {
                PalmIllustration(stage, Modifier.height(if (stage == 0) 140.dp else 220.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QText(stages[stage], style = MaterialTheme.typography.titleMedium)
                QText("${stage + 1} / 7", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            val multiplier = featured?.let { treeTypes[it.type] } ?: 1.0
            val progress by animateFloatAsState(growthProgress(featured?.gp ?: 0, multiplier), label = "Farm reward")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = if (LocalOled.current) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primaryContainer)
            QText(if (featured == null) "Plant a date palm, then complete a focus session to help it grow."
                else if (stage == 6) "A beautiful milestone. Plant a new intention when you're ready."
                else "${featured.gp} GP · ${(thresholds[stage + 1] * multiplier).roundToInt() - featured.gp} to ${stages[stage + 1]}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trees.size > 1) {
            QText("Your orchard · ${trees.size} palms", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trees.size, key = { trees[it].id }) { index ->
                    val tree = trees[index]
                    Glass(Modifier.width(210.dp)) {
                        QText(tree.type, style = MaterialTheme.typography.titleMedium)
                        PalmIllustration(stage(tree.gp, treeTypes[tree.type] ?: 1.0), Modifier.height(110.dp))
                        QText("${tree.gp} GP · ${if (tree.active) "Growing" else if (tree.maturedAt != null) "Matured" else "Resting"}", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(enabled = !tree.active && tree.maturedAt == null,
                            onClick = { vm.run { vm.dao.selectTree(tree.id) } }, modifier = Modifier.fillMaxWidth()) {
                            QText(if (tree.active) "Active tree" else if (tree.maturedAt != null) "Premium tree" else "Grow this tree")
                        }
                    }
                }
            }
        }
        Section("Plant an intention") {
            Picker("Date variety", treeTypes.keys.toList(), type, onSelect = { type = it })
            QText("${(380 * (treeTypes[type] ?: 1.0)).roundToInt()} GP to Premium · Focus nourishes your active palm.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { plant = true }, modifier = Modifier.fillMaxWidth()) { QText("Plant $type seed") }
            TextButton(onClick = onAbout) { QText("How the farm grows") }
        }
    }

    if (plant)
        Confirm(
            "Plant a $type seed?",
            "Your existing trees will stay in your orchard.",
            { plant = false },
            { vm.run { vm.dao.plant(type) } },
        )
}

@Composable
fun AboutFarmScreen() {
    Page {
        Heading("Small sessions, lasting roots", "How your date farm grows")
        Section("Focus becomes growth") {
            QText(
                "Finish a focus session to earn growth points for the tree selected when that session began. Paused sessions can be resumed; reset and skipped sessions earn no points. Breaks do not earn points.",
            )
            QText("25 minutes = 10 GP · 50 minutes = 20 GP · 90 minutes = 35 GP")
        }
        Section("Seven stages") {
            stages.forEachIndexed { i, name -> QText("${i+1}. $name — ${thresholds[i]} base GP") }
            QText(
                "Thresholds scale by variety. Premium trees stop growing, leaving you free to choose another seed.",
            )
        }
        Section("Find your variety") {
            treeTypes.forEach { (name, multiplier) ->
                QText("$name · ${(380*multiplier).roundToInt()} GP to Premium")
            }
        }
    }
}
