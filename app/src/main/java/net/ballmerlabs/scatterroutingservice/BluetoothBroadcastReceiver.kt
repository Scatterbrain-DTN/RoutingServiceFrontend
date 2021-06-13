package net.ballmerlabs.scatterroutingservice

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton


enum class BluetoothState {
    STATE_OFF,
    STATE_TURNING_OFF,
    STATE_ON,
    STATE_TURNING_ON,
    INVALID
}

@AndroidEntryPoint
@Singleton
class BluetoothBroadcastReceiver @Inject constructor(
        @ApplicationContext val context: Context,
        private val adapter: BluetoothAdapter
) : BroadcastReceiver() {

    val liveData = MutableLiveData(convertState(adapter.state))

    val state: BluetoothState
        get() = convertState(adapter.state)

    private val intentFilter = IntentFilter()
            .apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }


    private fun convertState(state: Int): BluetoothState {
        return when (state) {
            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothState.STATE_TURNING_OFF
            BluetoothAdapter.STATE_ON -> BluetoothState.STATE_ON
            BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.STATE_TURNING_ON
            BluetoothAdapter.STATE_OFF -> BluetoothState.STATE_OFF
            else -> BluetoothState.INVALID
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = convertState(
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            )
            liveData.value = state
        }
    }

    fun register() {
        context.registerReceiver(this, intentFilter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (exception: Exception) {
            Log.w(TAG, "unregistered existing receiver")
        }
    }

    companion object {
        const val TAG = "BluetoothReceiever"
    }
}