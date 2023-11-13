package net.ballmerlabs.scatterroutingservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.internal.SCOPE_DEFAULT
import net.ballmerlabs.uscatterbrain.isActive
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@AndroidEntryPoint
@Singleton
class BootBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject lateinit var binderWrapper: BinderWrapper

    private val log by scatterLog()

    private suspend fun startService(context: Context) {
        try {
            binderWrapper.register()
            binderWrapper.startService()
            withContext(Dispatchers.Main) { binderWrapper.bindService() }
            val active = isActive(context)
            log.v("starting service at boot, active: $active")
            if (active) {
                binderWrapper.startDiscover()
            } else {
                binderWrapper.startPassive()
            }
        } catch (exc: Exception) {
            log.cry("failed to start scatterbrain on boot $exc")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val start = prefs.getBoolean(context.getString(R.string.pref_enabled), true)
            Log.v("boot", "attempting to start scatterbrain router ")
            if (start) {
                binderWrapper.coroutineScope.launch {  startService(context) }
            }
        }
    }

}