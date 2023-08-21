package net.ballmerlabs.scatterroutingservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.uscatterbrain.isActive
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
@Singleton
class BootBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject lateinit var binderWrapper: BinderWrapper

    private val log by scatterLog()

    private suspend fun startService(context: Context) {
        binderWrapper.register()
        binderWrapper.startService()
        binderWrapper.bindService()
        val active = isActive(context)
        log.v("starting service at boot, active: $active")
        if (active) {
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