package net.ballmerlabs.scatterroutingservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.ui.DesktopObserver
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopAddrs
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopPower
import net.ballmerlabs.uscatterbrain.network.desktop.EXTRA_APPS
import net.ballmerlabs.uscatterbrain.network.desktop.EXTRA_DESKTOP_IP
import net.ballmerlabs.uscatterbrain.network.desktop.EXTRA_DESKTOP_POWER
import net.ballmerlabs.uscatterbrain.network.desktop.EXTRA_IDENTITY_IMPORT_STATE
import net.ballmerlabs.uscatterbrain.network.desktop.IdentityImportState
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject

@AndroidEntryPoint
class DesktopBroadcastReceiver @Inject constructor(): BroadcastReceiver() {

    private val LOG by scatterLog()

    @Inject
    lateinit var observer: DesktopObserver

    override fun onReceive(ctx: Context, intent: Intent) {
        val state = intent.getParcelableExtra<IdentityImportState>(EXTRA_IDENTITY_IMPORT_STATE)
        LOG.w("got state ${state?.handle}")
        if (state != null) {
            observer.currentImport.postValue(state)
        } else {
            observer.currentImport.postValue(null)
        }

        val power = intent.getParcelableExtra<DesktopPower>(EXTRA_DESKTOP_POWER)
        if (power != null)
            observer.desktopPower.postValue(power)

        val connectivity = intent.getParcelableExtra<DesktopAddrs>(EXTRA_DESKTOP_IP)

        if (connectivity != null)
            observer.connectivityState.postValue(connectivity)

        val apps = intent.getBooleanExtra(EXTRA_APPS, false)
        if (apps)
            observer.updateApps()
    }

}