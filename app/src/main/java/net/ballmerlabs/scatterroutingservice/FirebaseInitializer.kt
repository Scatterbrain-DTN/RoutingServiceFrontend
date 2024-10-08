package net.ballmerlabs.scatterroutingservice

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.startup.Initializer
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseInitializer : Initializer<FirebaseCrashlytics> {
    override fun create(context: Context): FirebaseCrashlytics {
        val optOut = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                        context.getString(R.string.pref_optout_crashlytics),
                        false
                )
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!optOut)

        return FirebaseCrashlytics.getInstance()
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

}