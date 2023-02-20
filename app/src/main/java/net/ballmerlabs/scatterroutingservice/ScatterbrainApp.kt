package net.ballmerlabs.scatterroutingservice

import android.app.Application
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ScatterbrainApp  @Inject constructor(
) : Application() {
    fun component() : ScatterbrainModule {
        return EntryPoints.get(this, ScatterbrainModule::class.java)
    }
    companion object {
        const val UI_PREFS = "ui-prefs"
    }
}