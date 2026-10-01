package com.example

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.media.DeviceMediaManager
import com.example.media.VelvetArtworkCache
import com.example.model.SampleData
import com.example.ui.VelvetApp
import com.example.ui.VelvetImageLoader
import com.example.ui.theme.VelvetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        VelvetImageLoader.get(this)
        // Immediate warm-cache on app mount: pre-caches all starter and device tracks
        VelvetArtworkCache.warmCache(this, SampleData.starterTracks)
        val cachedTracks = DeviceMediaManager.getCachedTracks(this)
        if (cachedTracks.isNotEmpty()) {
            VelvetArtworkCache.warmCache(this, cachedTracks)
        }
        enableEdgeToEdge()
        setContent {
            VelvetTheme {
                VelvetApp()
            }
        }
    }
}
