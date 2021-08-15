package net.ballmerlabs.scatterroutingservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import net.ballmerlabs.scatterbrainsdk.BinderWrapper

class BootBroadcastReceiver : BroadcastReceiver() {


    private fun isPowersave(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(
                context.getString(R.string.pref_powersave),
                context.getString(R.string.powersave_active)
        )!! == context.getString(R.string.powersave_active)

    }

    private fun startService(context: Context) {
       val startIntent = Intent(BinderWrapper.BIND_ACTION)
        startIntent.`package` = BinderWrapper.BIND_PACKAGE
        ContextCompat.startForegroundService(context, startIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val start = prefs.getBoolean(context.getString(R.string.pref_enabled), false)
        Log.e("debug", "starting scatterbrain router on boot $start")
        if (start) {
            startService(context)
        }
    }

}