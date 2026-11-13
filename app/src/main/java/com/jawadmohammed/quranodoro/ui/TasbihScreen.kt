package com.jawadmohammed.quranodoro.ui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.AppViewModel
import com.jawadmohammed.quranodoro.data.localDate
import java.time.LocalDate
@Composable
fun TasbihScreen(vm: AppViewModel) {
    val s by vm.tasbih.collectAsStateWithLifecycle()
    val p by vm.preferences.collectAsStateWithLifecycle()
    val sessions by vm.dhikr.collectAsStateWithLifecycle()
    var reset by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    val view = LocalView.current
    val touch = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val counterScale by animateFloatAsState(if (pressed) .96f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = .65f), label = "Tasbih touch")
    val progress by
        animateFloatAsState(
            (s.count.toFloat() / s.target).coerceIn(0f, 1f),
            label = "dhikr progress",
        )
    Page {
        Heading("Remember, gently", "A quiet moment for your heart.")
        Glass(Modifier.fillMaxWidth()) {
            Picker(
                "Dhikr",
                listOf(
                        "SubhanAllah",
                        "Alhamdulillah",
                        "Allahu Akbar",
                        "La ilaha illa Allah",
                        "Astaghfirullah",
                        s.phrase,
                    )
                    .distinct(),
                s.phrase,
                onSelect = vm::phrase,
            )
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val diameter = minOf(maxWidth, 280.dp)
                Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 7.dp,
                        trackColor =
                            if (LocalOled.current) MaterialTheme.colorScheme.outlineVariant
                            else MaterialTheme.colorScheme.primaryContainer,
                    )
                    Surface(
                        onClick = {
                            if (p.haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            vm.increment()
                        },
                        interactionSource = touch,
                        shape = CircleShape,
                        border =
                            if (LocalOled.current)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                )
                            else null,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize().padding(18.dp).graphicsLayer { scaleX = counterScale; scaleY = counterScale }.semantics {
                            contentDescription = "Tasbih counter"
                            stateDescription = "${s.count} of ${s.target}"
                        },
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            QText(
                                s.count.toString(),
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Light,
                            )
                            QText("of ${s.target}")
                            Spacer(Modifier.height(14.dp))
                            QText("Tap to count", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (s.count >= s.target)
                QText(
                    "Alhamdulillah. Your target is complete.",
                    color = MaterialTheme.colorScheme.primary,
                )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { n ->
                    FilterChip(
                        selected = s.target == n,
                        onClick = { vm.target(n) },
                        label = { QText(n.toString()) },
                    )
                }
            }
            OutlinedButton(onClick = { custom = true }, modifier = Modifier.fillMaxWidth()) {
                QText("Custom target")
            }
            TextButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth()) {
                QText("Reset counter")
            }
        }
        Section("Your practice") {
            Toggle("Touch feedback", value = p.haptics, onChange = { vm.bool("haptics", it) })
            QText(
                "${sessions.count{localDate(it.completedAt)==LocalDate.now()}} completed today · ${sessions.size} all time",
                style = MaterialTheme.typography.bodyMedium,
            )
            QText(
                "${sessions.count { localDate(it.completedAt)>=LocalDate.now().minusDays(6) }} completed in the last 7 days",
                style = MaterialTheme.typography.bodyMedium,
            )
            sessions.take(10).forEach {
                QText(
                    "${localDate(it.completedAt)} · ${it.target} ${it.phrase}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (custom) TargetDialog(s.target, { custom = false }) { vm.target(it); custom = false }
    if (reset)
        Confirm(
            "Begin again?",
            "Reset the current count to zero. Completed dhikr sessions stay in your history.",
            { reset = false },
            vm::resetTasbih,
        )
}
@Composable
private fun TargetDialog(target: Int, dismiss: () -> Unit, save: (Int) -> Unit) {
    var input by remember { mutableStateOf(target.toString()) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val valid = input.toIntOrNull()?.let { it in 1..9999 } == true
    fun close() { keyboard?.hide(); dismiss() }
    fun submit() { if (valid) { keyboard?.hide(); save(input.toInt()) } }
    GlassDialog(onDismissRequest = ::close) {
        Glass(Modifier.imePadding().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            QText("Your target", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            QText("Choose 1–9,999. Your current count stays with you.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                input, { input = it.filter { c -> c in '0'..'9' }.take(5) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = { QText("Target count") }, singleLine = true, isError = !valid,
                supportingText = { if (!valid) QText("Enter a number from 1 to 9,999") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = ::close) { QText("Cancel") }
                Button(enabled = valid, onClick = ::submit) { QText("Save") }
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    }
}
