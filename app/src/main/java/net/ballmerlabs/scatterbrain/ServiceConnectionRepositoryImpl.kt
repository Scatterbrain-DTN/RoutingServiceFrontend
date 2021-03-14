package net.ballmerlabs.scatterbrain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository.Companion.TAG
import net.ballmerlabs.scatterbrainsdk.HandshakeResult
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainAPI
import net.ballmerlabs.uscatterbrain.ScatterRoutingService
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class ServiceConnectionRepositoryImpl @Inject constructor(
        @ApplicationContext val context: Context,
        private val broadcastReceiver: ScatterbrainBroadcastReceiver
) : ServiceConnectionRepository {

    private var binder: ScatterbrainAPI? = null
    private val bindCallbackSet: MutableSet<(Boolean?) -> Unit> = mutableSetOf()
    private val callback = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = ScatterbrainAPI.Stub.asInterface(service)
            Log.v(TAG, "connected to ScatterRoutingService binder")
            try {
                bindCallbackSet.forEach { c ->  c(true)}
            } catch (e: RemoteException) {
                Log.e(TAG, "RemoteException: $e")
                bindCallbackSet.forEach { c -> c(null) }
            } finally {
                bindCallbackSet.clear()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            binder = null
            bindCallbackSet.forEach { c -> c(false) }
            bindCallbackSet.clear()
        }
    }

    private fun registerCallback(c: (Boolean?) -> Unit) {
        bindCallbackSet.add(c)
    }
    
    override suspend fun bindService(): Boolean = suspendCoroutine { ret ->
        if (binder == null) {
            registerCallback { b ->
                if (b == null) throw IllegalStateException("failed to bind service")
                ret.resume(b)
            }
            val bindIntent = Intent(context, ScatterRoutingService::class.java)
            context.bindService(bindIntent, callback, 0)
            val startIntent = Intent(context, ScatterRoutingService::class.java)
            context.startForegroundService(startIntent)
        } else {
            ret.resume(true)
        }
    }

    private suspend fun autoBindService() {
        val r = bindService()
        if (!r) {
            throw IllegalStateException("service not bound")
        }
    }
    
    override suspend fun unbindService(): Boolean = suspendCoroutine { ret ->
        registerCallback { r ->
            if (r == null) throw IllegalStateException("failed to bind service")
            ret.resume(r)
        }
        if (binder != null) {
            context.unbindService(callback)
        }
        val stopIntent = Intent(context, ScatterRoutingService::class.java)
        context.stopService(stopIntent)
    }

    override suspend fun getIdentities(): List<Identity> {
        autoBindService()
        return binder!!.identities
    }

    @ExperimentalCoroutinesApi
    override suspend fun observeIdentities(): Flow<List<Identity>>  = callbackFlow {
        autoBindService()
        val callback: suspend (handshakeResult: HandshakeResult) -> Unit = { handshakeResult ->
            if (handshakeResult.identities > 0) {
                offer(getIdentities())
            }
        }
        broadcastReceiver.addOnReceiveCallback(callback)
        
        awaitClose { 
            broadcastReceiver.removeOnReceiveCallback(callback)
        }
    }

    override suspend fun getScatterMessages(application: String): List<ScatterMessage> {
        autoBindService()
        return binder!!.getByApplication(application)
    }

    @ExperimentalCoroutinesApi
    override suspend fun observeMessages(application: String): Flow<List<ScatterMessage>> = callbackFlow  {
        autoBindService()
        val callback: suspend (handshakeResult: HandshakeResult) -> Unit = { handshakeResult ->
            if (handshakeResult.messages > 0) {
                offer(getScatterMessages(application))
            }
        }

        broadcastReceiver.addOnReceiveCallback(callback)

        awaitClose {
            broadcastReceiver.removeOnReceiveCallback(callback)
        }
    }

    override suspend fun generateIdentity(name: String) {
        autoBindService()
        Log.e("debug", "generateIdentity wrapper called")
        try {
                binder!!.generateIdentity(name)
            } catch (re: RemoteException) {
                Log.e(TAG, "remoteException")
                re.printStackTrace()
            }
    }
    
    init {
        Log.v(TAG, "init called")
    }
}