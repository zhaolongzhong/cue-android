package com.example.cue

import android.app.Application
import com.example.cue.utils.FileLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CueApplication : Application() {
    @Inject
    lateinit var environment: Environment

    override fun onCreate() {
        super.onCreate()

        // Initialize file logging system
        FileLogger.initialize(this)
        FileLogger.getInstance().info("CueApplication", "Application started - onCreate()")
    }
}
