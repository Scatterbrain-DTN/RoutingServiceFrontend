package net.ballmerlabs.scatterroutingservice.ui.power

import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterroutingservice.ui.Utils
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: BinderWrapper

    @Inject lateinit var wifiManager: WifiManager

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private val model by activityViewModels<RoutingServiceViewModel>()

    private val disabled = "Disabled"

    private val log by scatterLog()

    private lateinit var sharedPreferences: SharedPreferences

    private fun showWifiSnackBar() {
        Snackbar.make(binding.root, R.string.wifi_disabled_snackbar, Snackbar.LENGTH_LONG)
                .show()
    }

    private suspend fun startService() {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val powersave = sharedPreferences.getString(getString(R.string.pref_powersave), getString(R.string.powersave_active))
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
                binding.toggleButton.isChecked = false
                binding.toggleButton.isEnabled = false
            } else {
                if (!wifiManager.isWifiEnabled) {
                    showWifiSnackBar()
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
                    toggleOn(binding.toggleButton, button?:enabled)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        model.observeAdapterState().observe(viewLifecycleOwner) { state ->
            binding.toggleButton.isEnabled = state == BluetoothState.STATE_ON
        }

        serviceConnectionRepository.observeBinderState().observe(viewLifecycleOwner) { state ->
            binding.toggleButton.isEnabled =
                    state == BinderWrapper.Companion.BinderState.STATE_CONNECTED && model.permissionGranted.value?:false

            if(state == BinderWrapper.Companion.BinderState.STATE_CONNECTED)
                startIfEnabled()
        }

        serviceConnectionRepository.observeRouterState().observe(viewLifecycleOwner) { state ->
            log.v("router state ${state.state}")
            binding.statusText.text = state.state
            binding.toggleButton.isChecked = state == RouterState.DISCOVERING
        }

        model.permissionGranted.observe(viewLifecycleOwner) { granted ->
            binding.toggleButton.isEnabled = granted
                    && serviceConnectionRepository.observeBinderState().value == BinderWrapper.Companion.BinderState.STATE_CONNECTED
        }

        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            lifecycleScope.launch {
                if(serviceConnectionRepository.observeBinderState().value == BinderWrapper.Companion.BinderState.STATE_CONNECTED) {
                    toggleOn(compoundButton, b)
                } else {
                    log.e("not connected, disabling button")
                    compoundButton.isChecked = false
                }
            }
        }
        return binding.root
    }
}