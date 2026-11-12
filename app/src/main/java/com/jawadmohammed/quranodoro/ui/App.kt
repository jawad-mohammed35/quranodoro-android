@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.jawadmohammed.quranodoro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jawadmohammed.quranodoro.AppViewModel

data class Destination(val route: String,val title: String,val active: ImageVector,val inactive: ImageVector)
val destinations=listOf(Destination("home","Home",Icons.Filled.Home,Icons.Outlined.Home),Destination("focus","Focus",Icons.Filled.Timer,Icons.Outlined.Timer),Destination("quran","Quran",Icons.Filled.MenuBook,Icons.Outlined.MenuBook),Destination("tasbih","Tasbih",Icons.Filled.RadioButtonChecked,Icons.Outlined.RadioButtonUnchecked),Destination("farm","Farm",Icons.Filled.Park,Icons.Outlined.Park))
@Composable fun Quranodoro(vm: AppViewModel=viewModel()) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val dark=prefs.theme=="Dark" || prefs.theme=="System"&&isSystemInDarkTheme()
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window,activity.window.decorView).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark }
        }
    }
    QuranodoroTheme(prefs.theme) {
        LocaleProvider(prefs.language) {
        val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route?:"home"
        val playing by vm.playing.collectAsStateWithLifecycle()
        Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={
            TopAppBar(title={QText("Quranodoro",style=MaterialTheme.typography.titleLarge)},navigationIcon={if(route !in destinations.map { it.route }) IconButton(onClick={nav.popBackStack()}) {Icon(Icons.Filled.ArrowBack,"Back")}},actions={IconButton(onClick={nav.navigate("analytics"){launchSingleTop=true}}){Icon(Icons.Outlined.Insights,"Analytics")};IconButton(onClick={nav.navigate("settings"){launchSingleTop=true}}){Icon(Icons.Outlined.Settings,"Settings")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background.copy(alpha=.95f)))
        },bottomBar={
            Column {
                if(playing && route!="quran") Surface(color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth().clickable {nav.navigate("quran"){launchSingleTop=true}}) {
                    Row(Modifier.padding(horizontal=20.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){QText("Recitation is playing",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium);IconButton(onClick=vm::toggleAudio){Icon(Icons.Filled.Pause,"Pause recitation")}}
                }
                NavigationBar(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.94f),tonalElevation=0.dp) {
                    destinations.forEach { d -> NavigationBarItem(modifier=Modifier.testTag("tab-${d.route}"),selected=route==d.route,onClick={nav.navigate(d.route){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(if(route==d.route)d.active else d.inactive,d.title)},label={QText(d.title,maxLines=1,overflow=TextOverflow.Ellipsis)}) }
                }
            }
        }) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha=.28f),MaterialTheme.colorScheme.background)))) {
                NavHost(navController=nav,startDestination="home",modifier=Modifier.fillMaxSize()) {
                    composable("home"){HomeScreen(vm){nav.navigate(it){launchSingleTop=true}}}
                    composable("focus"){FocusScreen(vm)}
                    composable("quran"){QuranScreen(vm)}
                    composable("tasbih"){TasbihScreen(vm)}
                    composable("farm"){FarmScreen(vm){nav.navigate("about-farm")}}
                    composable("analytics"){AnalyticsScreen(vm)}
                    composable("settings"){SettingsScreen(vm){nav.navigate(it)}}
                    composable("about"){AboutScreen()}
                    composable("about-farm"){AboutFarmScreen()}
                }
            }
        }
        }
    }
}
