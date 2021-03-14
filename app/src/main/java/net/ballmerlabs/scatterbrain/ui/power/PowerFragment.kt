package net.ballmerlabs.scatterbrain.ui.power

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository
import net.ballmerlabs.scatterbrain.databinding.FragmentPowerBinding
import java.lang.IllegalStateException
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: ServiceConnectionRepository

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            GlobalScope.launch {
                try {
                    val v = if (b)
                        serviceConnectionRepository.bindService()
                    else
                        serviceConnectionRepository.unbindService()
                    compoundButton.isChecked = v
                } catch (e: IllegalStateException) {
                    compoundButton.isChecked = false
                    //TODO: update ui with error
                }
            }
        }
        return binding.root
    }

    companion object {
        private const val TAG = "PowerFragment"
    }
}