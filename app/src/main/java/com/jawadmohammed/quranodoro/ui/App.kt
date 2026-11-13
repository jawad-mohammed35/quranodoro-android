@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.jawadmohammed.quranodoro.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jawadmohammed.quranodoro.AppViewModel
data class Destination(
    val route: String,
    val title: String,
    val active: ImageVector,
    val inactive: ImageVector,
)
val destinations = listOf(
    Destination("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Destination("quran", "Quran", Icons.Filled.Book, Icons.Outlined.Book),
    Destination("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer),
    Destination("farm", "Farm", Icons.Filled.Park, Icons.Outlined.Park),
    Destination("tasbih", "Tasbih", Icons.Filled.Spa, Icons.Outlined.Spa),
)
@Composable
fun Quranodoro(vm: AppViewModel = viewModel()) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val oled = prefs.theme == "OLED Black"
    val dark = oled || prefs.theme == "Dark" || prefs.theme == "System" && isSystemInDarkTheme()
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            androidx.core.view.WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView,
                )
                .apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
        }
    }
    QuranodoroTheme(prefs.theme) {
        val pageLayer = rememberGraphicsLayer()
        var pagePosition by remember { mutableStateOf(Offset.Zero) }
        val blur = !oled && android.os.Build.VERSION.SDK_INT >= 31 &&
            !(context.getSystemService(android.app.ActivityManager::class.java)?.isLowRamDevice ?: false)
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: "home"
        val selectedTab =
            if (destinations.any { it.route == route }) route
            else entry?.savedStateHandle?.get<String>("parentTab") ?: "home"
        fun openPage(page: String) {
            nav.navigate(page) { launchSingleTop = true }
            nav.currentBackStackEntry?.savedStateHandle?.set("parentTab", selectedTab)
        }
        val requested by (context as com.jawadmohammed.quranodoro.MainActivity).requestedRoute.collectAsState()
        LaunchedEffect(requested) {
            if (requested == "focus") {
                nav.navigate("focus") { launchSingleTop = true; popUpTo(nav.graph.findStartDestination().id) { saveState = true }; restoreState = true }
                context.requestedRoute.value = null
            }
        }
        val playing by vm.playing.collectAsStateWithLifecycle()
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Box(Modifier.padding(horizontal = 12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
                if (blur) FrostedBackdrop(pageLayer, pagePosition, Modifier.matchParentSize())
                TopAppBar(
                    modifier = Modifier.glassSurface(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                    title = {
                        Column {
                            QText(if (route.startsWith("settings")) "Settings" else "Quranodoro", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.4f) QText("FOCUS · REFLECT · GROW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        if (route !in destinations.map { it.route })
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                    },
                    actions = {
                        if (route != "analytics") IconButton(
                            onClick = { openPage("analytics") },
                        ) {
                            Icon(Icons.Outlined.Insights, "Analytics")
                        }
                        if (!route.startsWith("settings")) IconButton(
                            onClick = { openPage("settings") },
                        ) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor =
                                androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                }
            },
            bottomBar = {
                Column(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (playing && route != "quran")
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier =
                                Modifier.fillMaxWidth().clickable {
                                    nav.navigate("quran") { launchSingleTop = true }
                                },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                QText(
                                    "Recitation is playing",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(onClick = vm::toggleAudio) {
                                    Icon(Icons.Filled.Pause, "Pause recitation")
                                }
                            }
                        }
                    GlassNavigation(
                        selectedTab,
                        prefs.haptics,
                        if (blur) pageLayer else null,
                        pagePosition,
                    ) { destination ->
                        if (destinations.none { it.route == nav.currentDestination?.route }) {
                            nav.popBackStack(selectedTab, inclusive = false)
                        }
                        if (destination.route == "home") {
                            nav.popBackStack("home", inclusive = false)
                        }
                        if (nav.currentDestination?.route != destination.route) {
                            nav.navigate(destination.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            },
        ) { insets ->
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { pagePosition = it.positionInRoot() }
                    .then(
                        if (blur)
                            Modifier.drawWithContent {
                                pageLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(pageLayer)
                            }
                        else Modifier,
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (oled) 0f else .50f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (oled) 0f else .28f),
                            ),
                        ),
                    ),
            ) {
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { it / 18 } },
                    exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) },
                    modifier = Modifier.fillMaxSize().padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()),
                ) {
                    composable("home") {
                        HomeScreen(vm) { nav.navigate(it) { launchSingleTop = true } }
                    }
                    composable("focus") { FocusScreen(vm) }
                    composable("quran") { QuranScreen(vm) }
                    composable("tasbih") { TasbihScreen(vm) }
                    composable("farm") { FarmScreen(vm) { openPage("about-farm") } }
                    composable("analytics") { AnalyticsScreen(vm) }
                    composable("settings") { SettingsScreen(vm, ::openPage) }
                    listOf("appearance", "focus", "tasbih", "audio", "progress", "about").forEach { section ->
                        composable("settings/$section") { SettingsScreen(vm, ::openPage, section) }
                    }
                    composable("about") { AboutScreen() }
                    composable("about-farm") { AboutFarmScreen() }
                }
            }
        }
    }
}
