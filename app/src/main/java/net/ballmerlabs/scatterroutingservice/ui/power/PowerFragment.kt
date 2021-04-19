package net.ballmerlabs.scatterroutingservice.ui.power

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.isActive
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: BinderWrapper

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private val model: RoutingServiceViewModel by viewModels()

    private val DISABLED = "Disabled"

    private lateinit var sharedPreferences: SharedPreferences

    private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
        sharedPreferences: SharedPreferences, s: String ->
        if (s == requireContext().getString(R.string.pref_powersave)) {
            binding.statusText.text = getStatusText()
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
        binding.toggleButton.isChecked = serviceConnectionRepository.isConnected()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        binding.statusText.text = getStatusText()
        binding.toggleButton.isChecked = serviceConnectionRepository.isConnected()
        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            lifecycleScope.softCancelLaunch {
                if (isActive) {
                    val powersave = getStatusText()
                    try {
                        if (b) {
                            serviceConnectionRepository.startService()
                            serviceConnectionRepository.bindService()
                            if (powersave == getString(R.string.powersave_active)) {
                                serviceConnectionRepository.startDiscover()
                            } else {
                                serviceConnectionRepository.startPassive()
                            }
                        } else {
                            if (powersave == getString(R.string.powersave_active)) {
                                serviceConnectionRepository.stopPassive()
                            } else {
                                serviceConnectionRepository.stopPassive()
                            }
                            serviceConnectionRepository.unbindService()
                            serviceConnectionRepository.stopService()
                        }
                    } catch (e: IllegalStateException) {
                        compoundButton.isChecked = false
                        e.printStackTrace()
                    } finally {
                        binding.statusText.text = powersave
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