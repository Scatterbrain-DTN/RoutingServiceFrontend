package net.ballmerlabs.scatterroutingservice.ui.power

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.isActive
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ServiceConnectionRepository
import net.ballmerlabs.scatterroutingservice.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: ServiceConnectionRepository

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private val model: RoutingServiceViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            lifecycleScope.softCancelLaunch {
                if (isActive) {
                    try {
                        if (b) {
                            serviceConnectionRepository.startService()
                            serviceConnectionRepository.bindService()
                        } else {
                            serviceConnectionRepository.stopService()
                            serviceConnectionRepository.unbindService()
                        }
                    } catch (e: IllegalStateException) {
                        compoundButton.isChecked = false
                        e.printStackTrace()
                    }
                }
            }
        }
        model.serviceConnections
                .observe(viewLifecycleOwner) {b -> binding.toggleButton.isChecked = b}
        return binding.root
    }

    companion object {
        private const val TAG = "PowerFragment"
    }
}