package net.ballmerlabs.scatterroutingservice.ui.power

import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ui.Utils
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject
    lateinit var serviceConnectionRepository: BinderWrapper

    @Inject
    lateinit var wifiManager: WifiManager

    private val model by activityViewModels<RoutingServiceViewModel>()

    private val disabled = "Disabled"

    private val log by scatterLog()

    private lateinit var sharedPreferences: SharedPreferences

    private suspend fun startService() {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val powersave = sharedPreferences.getString(
                        getString(R.string.pref_powersave),
                        getString(R.string.powersave_active)
                    )
                    log.v("starting discovery: $powersave")
                    if (powersave == getString(R.string.powersave_active)) {
                        serviceConnectionRepository.startDiscover()
                    } else {
                        serviceConnectionRepository.startPassive()
                    }
                } catch (exc: Exception) {
                    log.e("failed to bind service $exc")
                    FirebaseCrashlytics.getInstance().recordException(exc)
                }
            }
        } catch (exc: Exception) {
            log.e("failed to start service")
            FirebaseCrashlytics.getInstance().recordException(exc)
        }
    }

    private suspend fun stopService() {
        serviceConnectionRepository.stopPassive()
        serviceConnectionRepository.stopDiscover()
    }

    private fun setEnabled(boolean: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(requireContext().getString(R.string.pref_enabled), boolean)
            apply()
        }
    }

    private fun getEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            requireContext().getString(R.string.pref_enabled),
            false
        )
    }


    private suspend fun toggleOn(compoundButton: CompoundButton, enable: Boolean) {
        withContext(Dispatchers.Main) {
            if (model.adapterState != BluetoothState.STATE_ON) {
                log.e("adapter disabled")
                //TODO
            } else {
                if (!wifiManager.isWifiEnabled) {
                    //TODO
                }
                try {
                    setEnabled(enable)
                    withContext(Dispatchers.IO) {
                        if (enable) {
                            startService()
                        } else {
                            stopService()
                        }
                    }
                } catch (e: IllegalStateException) {
                    compoundButton.isChecked = false
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startIfEnabled(button: Boolean? = null) {
        lifecycleScope.launch(Dispatchers.IO) {

            val enabled = getEnabled()
            if (enabled) {
                val perm = Utils.checkPermission(requireContext())
                if (perm.isPresent) {
                    val toast = Toast(requireContext())
                    toast.duration = Toast.LENGTH_LONG
                    toast.setText(getString(R.string.missing_permission, perm.get()))
                    toast.show()
                } else {
                    log.e("toggling")
                    //TODO
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        return ComposeView(requireContext()).apply {
            setContent { }
        }
    }
}