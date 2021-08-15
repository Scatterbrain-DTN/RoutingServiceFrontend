package net.ballmerlabs.scatterroutingservice.ui.power

import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: BinderWrapper

    @Inject lateinit var wifiManager: WifiManager

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    @InternalCoroutinesApi
    private val model: RoutingServiceViewModel by viewModels()

    private val DISABLED = "Disabled"

    private lateinit var sharedPreferences: SharedPreferences

    private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
        prefs: SharedPreferences, s: String ->
        if (s == requireContext().getString(R.string.pref_powersave)) {
            binding.statusText.text = getStatusText()
        } else if (s == requireContext().getString(R.string.pref_enabled)) {
            if (getEnabled()) {
                lifecycleScope.launch { startService() }
            } else {
                lifecycleScope.launch { stopService() }
            }

        }

    }

    private fun getStatusText(sharedPreferences: SharedPreferences): String {
        return if (binding.toggleButton.isChecked) {
            sharedPreferences.getString(requireContext().getString(R.string.pref_powersave), getString(R.string.powersave_active))!!
        } else {
            DISABLED
        }
    }

    private fun getStatusText(): String {
        return getStatusText(sharedPreferences)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        lifecycleScope.launch(Dispatchers.Main) {  binding.toggleButton.isChecked = serviceConnectionRepository.isConnected() }
    }

    fun showWifiSnackBar() {
        Snackbar.make(binding.root, R.string.wifi_disabled_snackbar, Snackbar.LENGTH_LONG)
                .show()
    }

    private suspend fun startService() {
        val powersave = getStatusText()
        serviceConnectionRepository.startService()
        if (powersave == getString(R.string.powersave_active)) {
            serviceConnectionRepository.startDiscover()
        } else {
            serviceConnectionRepository.startPassive()
        }
        binding.toggleButton.isChecked = true
    }

    private suspend fun stopService() {
        val powersave = getStatusText()
        if (powersave == getString(R.string.powersave_active)) {
            serviceConnectionRepository.stopPassive()
        } else {
            serviceConnectionRepository.stopPassive()
        }
        serviceConnectionRepository.unbindService()
        serviceConnectionRepository.stopService()
        binding.toggleButton.isChecked = false
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

    @InternalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        binding.statusText.text = getStatusText()
        lifecycleScope.launch(Dispatchers.Main) {
            binding.toggleButton.isChecked = serviceConnectionRepository.isConnected()
            if(getEnabled()) {
                startService()
            } else {
                stopService()
            }

        }
        model.observeAdapterState().observe(viewLifecycleOwner, { state ->
            binding.toggleButton.isEnabled = state == BluetoothState.STATE_ON
        })

        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            lifecycleScope.softCancelLaunch {
                if (isActive) {
                    if (model.adapterState != BluetoothState.STATE_ON) {
                        binding.toggleButton.isChecked = false
                        binding.toggleButton.isEnabled = false
                    } else {
                        if (!wifiManager.isWifiEnabled) {
                            showWifiSnackBar()
                        }
                        binding.toggleButton.isEnabled = true
                        try {
                            if (b) {
                                startService()
                            } else {
                                stopService()
                            }
                            setEnabled(b)
                        } catch (e: IllegalStateException) {
                            compoundButton.isChecked = false
                            e.printStackTrace()
                        } finally {
                            binding.statusText.text = getStatusText()
                        }
                    }
                }
            }
        }
        return binding.root
    }

    companion object {
        private const val TAG = "PowerFragment"
    }
}