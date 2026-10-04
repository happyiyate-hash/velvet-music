package com.example

import android.app.Application
import com.example.ads.AdMobManager

class VelvetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdMobManager.initialize(this)
    }
}
