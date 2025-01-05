package com.example.cue

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CueApplication : Application() {
    @Inject
    lateinit var environment: Environment
}
