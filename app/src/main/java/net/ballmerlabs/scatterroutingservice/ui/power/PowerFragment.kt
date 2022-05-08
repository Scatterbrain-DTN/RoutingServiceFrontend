package net.ballmerlabs.scatterroutingservice.ui.power

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: BinderWrapper

    @Inject lateinit var wifiManager: WifiManager

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private val model: RoutingServiceViewModel by viewModels()

    private val disabled = "Disabled"

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

    private suspend fun checkPermission(permission: String): Boolean = suspendCancellableCoroutine { c ->
        if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        permission
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            c.resumeWith(Result.success(true))
        } else {

            c.resumeWith(Result.success(false))
        }

    }

    private suspend fun checkPermission(): Optional<String> = suspendCancellableCoroutine { c ->
        lifecycleScope.launch(Dispatchers.Main) {
            var res: Optional<String> = Optional.empty()
            if(!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                res = Optional.of(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    for (perm in arrayOf(
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN)
                    ) {
                        if (!checkPermission(perm)) {
                            res = Optional.of(perm)
                            break
                        }
                    }
                }
            }
            c.resumeWith(Result.success(res))
        }
    }

    private fun getStatusText(sharedPreferences: SharedPreferences): String {
        return if (binding.toggleButton.isChecked) {
            sharedPreferences.getString(requireContext().getString(R.string.pref_powersave), getString(R.string.powersave_active))!!
        } else {
            disabled
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
    }

    private fun showWifiSnackBar() {
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


    private suspend fun toggleOn(compoundButton: CompoundButton, enable: Boolean) {
        if (model.adapterState != BluetoothState.STATE_ON) {
            binding.toggleButton.isChecked = false
            binding.toggleButton.isEnabled = false
        } else {
            if (!wifiManager.isWifiEnabled) {
                showWifiSnackBar()
            }
            binding.toggleButton.isEnabled = true
            try {
                if (enable) {
                    startService()
                } else {
                    stopService()
                }
                setEnabled(enable)
            } catch (e: IllegalStateException) {
                compoundButton.isChecked = false
                e.printStackTrace()
            } finally {
                binding.statusText.text = getStatusText()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        binding.statusText.text = getStatusText()
        serviceConnectionRepository.observeBinderState().observe(viewLifecycleOwner) { state ->
            if (state == BinderWrapper.Companion.BinderState.STATE_CONNECTED) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.toggleButton.isChecked = serviceConnectionRepository.isConnected()
                }
            }
        }

        lifecycleScope.launch {
            if (getEnabled()) {
                startService()
            } else {
                stopService()
            }
        }
        model.observeAdapterState().observe(viewLifecycleOwner) { state ->
            binding.toggleButton.isEnabled = state == BluetoothState.STATE_ON
        }

        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            lifecycleScope.softCancelLaunch {
                if (isActive) {
                    val perm = checkPermission()
                    if (perm.isPresent) {
                        binding.toggleButton.isChecked = false
                        val toast = Toast(requireContext())
                        toast.duration = Toast.LENGTH_LONG
                        toast.setText("Failed to start, missing permission ${perm.get()}")
                        toast.show()
                    } else {
                        toggleOn(compoundButton, b)
                    }
                }
            }
        }
        return binding.root
    }
}