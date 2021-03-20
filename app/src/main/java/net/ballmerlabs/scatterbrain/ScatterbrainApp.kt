package net.ballmerlabs.scatterbrain

import android.app.Application
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope

@HiltAndroidApp
class ScatterbrainApp : Application() {
    fun component() : ScatterbrainModule {
        return EntryPoints.get(this, ScatterbrainModule::class.java)
    }
}