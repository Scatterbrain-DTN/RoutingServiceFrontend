package net.ballmerlabs.scatterbrain.ui.power

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository
import net.ballmerlabs.scatterbrain.databinding.FragmentPowerBinding
import net.ballmerlabs.scatterbrainsdk.ScatterbrainAPI
import net.ballmerlabs.uscatterbrain.ScatterRoutingService
import javax.inject.Inject

@AndroidEntryPoint
class PowerFragment : Fragment() {

    @Inject lateinit var serviceConnectionRepository: ServiceConnectionRepository

    private var _binding:  FragmentPowerBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private var binder: ScatterbrainAPI? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = ScatterbrainAPI.Stub.asInterface(service)
            Log.v(TAG, "connected to ScatterRoutingService binder")
            try {
                if (!binder!!.isDiscovering) binder!!.startDiscovery()
                updateCheckedStatus()
            } catch (e: RemoteException) {
                Log.e(TAG, "RemoteException: $e")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            updateCheckedStatus()
            binder = null
        }
    }

    @Synchronized
    private fun updateCheckedStatus() {
        try {
            if (binder != null) {
                 binding.toggleButton.isChecked = binder!!.isDiscovering || binder!!.isPassive
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException: $e")
            binding.toggleButton.isChecked = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_power, container, false)
        _binding = FragmentPowerBinding.inflate(layoutInflater)
        val bindIntent = Intent(requireActivity().applicationContext, ScatterRoutingService::class.java)
        requireActivity().bindService(bindIntent, mServiceConnection, 0)
        updateCheckedStatus()
        binding.toggleButton.setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            val startIntent = Intent(requireActivity().applicationContext, ScatterRoutingService::class.java)
            if (b) {
                requireActivity().startForegroundService(startIntent)
            } else {
                requireActivity().unbindService(mServiceConnection)
                requireActivity().stopService(startIntent)
            }
        }
        return root
    }

    companion object {
        private const val TAG = "PowerFragment"
    }
}