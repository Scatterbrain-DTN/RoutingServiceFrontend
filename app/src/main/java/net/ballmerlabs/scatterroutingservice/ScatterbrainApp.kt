package net.ballmerlabs.scatterroutingservice

import android.app.Application
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScatterbrainApp : Application() {
    fun component() : ScatterbrainModule {
        return EntryPoints.get(this, ScatterbrainModule::class.java)
    }
}