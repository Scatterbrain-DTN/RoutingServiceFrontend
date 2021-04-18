package net.ballmerlabs.scatterroutingservice

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import net.ballmerlabs.uscatterbrain.RoutingServiceComponent

@HiltAndroidApp
class ScatterbrainApp : Application() {
    fun component() : ScatterbrainModule {

        return EntryPoints.get(this, ScatterbrainModule::class.java)
    }
}