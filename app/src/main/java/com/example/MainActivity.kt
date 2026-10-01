package com.example

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.VelvetApp
import com.example.ui.VelvetImageLoader
import com.example.ui.theme.VelvetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        VelvetImageLoader.get(this)
        enableEdgeToEdge()
        setContent {
            VelvetTheme {
                VelvetApp()
            }
        }
    }
}
