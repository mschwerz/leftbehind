package com.schwerzl.leftbehind

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
open class LeftBehindApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("LeftBehindApplication", "onCreate")
    }
}