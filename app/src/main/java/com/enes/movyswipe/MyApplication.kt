package com.enes.movyswipe

import android.app.Application
import com.enes.movyswipe.util.FirebaseSetup
import com.enes.movyswipe.util.ThemePrefs
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseSetup.configure()
        ThemePrefs.apply(this)
    }
}
