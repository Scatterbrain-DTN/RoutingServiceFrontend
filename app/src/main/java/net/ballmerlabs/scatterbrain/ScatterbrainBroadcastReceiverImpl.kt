package net.ballmerlabs.scatterbrain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import net.ballmerlabs.scatterbrainsdk.HandshakeResult
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import javax.inject.Inject

class ScatterbrainBroadcastReceiverImpl @Inject constructor(
        @ApplicationContext val context: Context
) : BroadcastReceiver(), ScatterbrainBroadcastReceiver {
    val intentFilter = IntentFilter(context.getString(R.string.broadcast_message))
    val callbackSet = mutableSetOf<suspend (HandshakeResult) -> Unit>()
    
    override fun onReceive(ctx: Context, intent: Intent) {
        val handshakeResult = intent.getParcelableExtra<HandshakeResult>(ScatterbrainApi.EXTRA_TRANSACTION_RESULT)!!
        callbackSet.forEach { h -> runBlocking {  h(handshakeResult) } }
    }

    override fun register() {
        context.registerReceiver(this, intentFilter)
    }

    override fun unregister() {
        context.unregisterReceiver(this)
    }

    override fun addOnReceiveCallback(r: suspend (HandshakeResult) -> Unit) {
        callbackSet.add(r)
    }

    override fun removeOnReceiveCallback(r: suspend (HandshakeResult) -> Unit) {
        callbackSet.remove(r)
    }
}