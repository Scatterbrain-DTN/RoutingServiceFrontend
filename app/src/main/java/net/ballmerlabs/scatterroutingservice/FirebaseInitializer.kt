package net.ballmerlabs.scatterroutingservice

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.startup.Initializer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import net.ballmerlabs.uscatterbrain.RoutingServiceComponent

class FirebaseInitializer : Initializer<FirebaseCrashlytics> {
    override fun create(context: Context): FirebaseCrashlytics {
        Log.e("debug", "initializing firebase")
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                        context.getString(R.string.pref_enable_crashlytics),
                        false
                )) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }
        return FirebaseCrashlytics.getInstance()
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

}