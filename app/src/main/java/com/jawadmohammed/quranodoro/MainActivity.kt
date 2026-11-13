package com.jawadmohammed.quranodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jawadmohammed.quranodoro.ui.Quranodoro

class MainActivity : ComponentActivity() {
    val requestedRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        requestedRoute.value = intent.getStringExtra("route")
    }
    private val model: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = android.content.res.Configuration(newBase.resources.configuration)
        configuration.setLocale(java.util.Locale.ENGLISH)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) requestedRoute.value = intent.getStringExtra("route")
        intent.removeExtra("route")
        splash.setKeepOnScreenCondition { !model.initialized.value }
        if (resources.configuration.smallestScreenWidthDp < 600)
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent {
            val ready = model.initialized.collectAsStateWithLifecycle()
            if (ready.value) {
                val theme = model.preferences.collectAsStateWithLifecycle().value.theme
                androidx.compose.runtime.LaunchedEffect(theme) {
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        splashScreen.setSplashScreenTheme(
                            when (theme) {
                                "Light" -> R.style.Theme_Quranodoro_Splash_Light
                                "Dark" -> R.style.Theme_Quranodoro_Splash_Dark
                                "OLED Black" -> R.style.Theme_Quranodoro_Splash_Oled
                                else -> R.style.Theme_Quranodoro_Splash
                            },
                        )
                    }
                }
                Quranodoro(model)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        model.foreground.value = true
    }

    override fun onStop() {
        model.foreground.value = false
        super.onStop()
    }
}
