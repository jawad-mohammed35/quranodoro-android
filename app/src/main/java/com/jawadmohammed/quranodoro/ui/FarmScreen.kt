package com.jawadmohammed.quranodoro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
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
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFFB7996B), Color(0xFF775B3E)),
                    center = Offset(cx, ground - 12),
                ),
                Offset(cx - 12, ground - 19),
                Size(24f, 15f),
            )
            return@Canvas
        }
        val top = ground - h * (.16f + growth * .08f)
        val trunkWidth = 8f + growth * 2f
        val trunk =
            Path().apply {
                moveTo(cx - trunkWidth, ground)
                quadraticBezierTo(cx + 6, top + h * .3f, cx - 2, top)
                lineTo(cx + trunkWidth * .5f, top)
                quadraticBezierTo(cx + trunkWidth + 12, top + h * .3f, cx + trunkWidth, ground)
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
                    quadraticBezierTo(
                        cx + (ex - cx) * .5f,
                        top - leafLength * .7f,
                        ex,
                        ey + leafLength * .2f,
                    )
                    quadraticBezierTo(cx + (ex - cx) * .6f, top - leafLength * .12f, cx, top)
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
    Page {
        Heading("Your date farm", "Good things grow with a little patience.")
        Section("Plant an intention") {
            Picker("Date variety", treeTypes.keys.toList(), type, onSelect = { type = it })
            QText(
                "${(380*(treeTypes[type]?:1.0)).roundToInt()} growth points to Premium Tree. Only your active tree receives focus rewards.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = { plant = true }, modifier = Modifier.fillMaxWidth()) {
                QText("Plant $type seed")
            }
            TextButton(onClick = onAbout) { QText("How the farm grows") }
        }
        if (trees.isEmpty())
            Glass(Modifier.fillMaxWidth()) {
                PalmIllustration(0)
                QText("Your orchard begins with a seed.")
                QText(
                    "Plant a date palm, then complete a focus session to help it grow.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        trees
            .sortedByDescending { it.active }
            .forEach { tree ->
                val multiplier = treeTypes[tree.type] ?: 1.0
                val s = stage(tree.gp, multiplier)
                Glass(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QText(tree.type, style = MaterialTheme.typography.titleLarge)
                        QText(
                            if (tree.maturedAt != null) "Matured"
                            else if (tree.active) "Actively growing" else "Resting",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    PalmIllustration(s)
                    QText(stages[s], style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { growthProgress(tree.gp, multiplier) },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor =
                            if (LocalOled.current) MaterialTheme.colorScheme.outlineVariant
                            else MaterialTheme.colorScheme.primaryContainer,
                    )
                    QText(
                        "${tree.gp} GP" +
                            if (s < 6)
                                " · ${(thresholds[s+1]*multiplier).roundToInt()-tree.gp} to ${stages[s+1]}"
                            else " · A beautiful milestone",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!tree.active && tree.maturedAt == null)
                        OutlinedButton(
                            onClick = { vm.run { vm.dao.selectTree(tree.id) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            QText("Grow this tree")
                        }
                }
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
