package net.ballmerlabs.scatterbrain

import net.ballmerlabs.scatterbrainsdk.HandshakeResult

interface ScatterbrainBroadcastReceiver {
    fun register()
    fun unregister()
    fun addOnReceiveCallback(r: suspend (HandshakeResult) -> Unit)
    fun removeOnReceiveCallback(r: suspend (HandshakeResult) -> Unit)
}