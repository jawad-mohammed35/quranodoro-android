package com.jawadmohammed.quranodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jawadmohammed.quranodoro.ui.Quranodoro

class MainActivity: ComponentActivity() {
    private val model: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if(resources.configuration.smallestScreenWidthDp<600) requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent { Quranodoro(model) }
    }
    override fun onStart() { super.onStart();model.foreground.value=true }
    override fun onStop() { model.foreground.value=false;super.onStop() }
}
