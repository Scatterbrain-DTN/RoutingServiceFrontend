package net.ballmerlabs.scatterroutingservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
@Singleton
class BootBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject lateinit var binderWrapper: BinderWrapper


    private fun isPowersave(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(
                context.getString(R.string.pref_powersave),
                context.getString(R.string.powersave_active)
        )!! == context.getString(R.string.powersave_active)

    }

    private suspend fun startService(context: Context) {
        binderWrapper.register()
        binderWrapper.startService()
        if (isPowersave(context)) {
            binderWrapper.startDiscover()
        } else {
            binderWrapper.startPassive()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val start = prefs.getBoolean(context.getString(R.string.pref_enabled), false)
            Log.v("boot", "attempting to start scatterbrain router ")
            if (start) {
                runBlocking { startService(context) }
            }
        }
    }

}