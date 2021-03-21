package net.ballmerlabs.scatterbrain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository.Companion.TAG
import net.ballmerlabs.scatterbrainsdk.HandshakeResult
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainAPI
import net.ballmerlabs.uscatterbrain.ScatterRoutingService
import java.lang.Exception
import java.lang.IllegalArgumentException
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
    private val pm = context.packageManager

    private lateinit var callback: ServiceConnection

    @ExperimentalCoroutinesApi
    override val serviceConnections = callbackFlow {
        offer(false)
        callback  = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = ScatterbrainAPI.Stub.asInterface(service)
                Log.v(TAG, "connected to ScatterRoutingService binder")
                try {
                    bindCallbackSet.forEach { c ->  c(true)}
                    offer(true)
                } catch (e: RemoteException) {
                    Log.e(TAG, "RemoteException: $e")
                    bindCallbackSet.forEach { c -> c(null) }
                    offer(false)
                } finally {
                    bindCallbackSet.clear()
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                binder = null
                bindCallbackSet.forEach { c -> c(false) }
                bindCallbackSet.clear()
                offer(false)
            }
        }

        awaitClose {  }
    }

    private fun registerCallback(c: (Boolean?) -> Unit) {
        bindCallbackSet.add(c)
    }
    
    private fun unregisterCallback(c: (Boolean?) -> Unit) {
        bindCallbackSet.remove(c)
    }

    private suspend fun bindServiceWithoutTimeout(): Unit = suspendCoroutine { ret ->
        if (binder == null) {
            registerCallback { b ->
                if (b == null || b == false) throw IllegalStateException("failed to bind service")
                ret.resume(Unit)
            }
            val bindIntent = Intent(context, ScatterRoutingService::class.java)
            context.bindService(bindIntent, callback, 0)
        } else {
            ret.resume(Unit)
        }
    }

    override suspend fun startService() {
        val startIntent = Intent(context, ScatterRoutingService::class.java)
        context.startForegroundService(startIntent)
        bindService()
    }

    override suspend fun bindService() {
        withTimeout(5000L) {
            bindServiceWithoutTimeout()
        }
    }
    
    override suspend fun unbindService(): Boolean = suspendCoroutine { ret ->
        val c: (Boolean?) -> Unit = { r ->
            if (r == null) throw IllegalStateException("failed to bind service")
            ret.resume(r)
            
        }
        try {
            registerCallback(c)
            if (binder != null) {
                context.unbindService(callback)
            }
        } catch (e: IllegalArgumentException) {
            ret.resume(true) //service already unbound
        } finally {
            unregisterCallback(c)
        }
    }

    override suspend fun getIdentities(): List<Identity> {
        bindService()
        return binder!!.identities
    }

    override suspend fun stopService() {
        val stopIntent = Intent(context, ScatterRoutingService::class.java)
        context.stopService(stopIntent)
    }

    @ExperimentalCoroutinesApi
    override suspend fun observeIdentities(): Flow<List<Identity>>  = callbackFlow {
        bindService()
        offer(getIdentities())
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
        bindService()
        return binder!!.getByApplication(application)
    }

    @ExperimentalCoroutinesApi
    override suspend fun observeMessages(application: String): Flow<List<ScatterMessage>> = callbackFlow  {
        bindService()
        offer(getScatterMessages(application))
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

    override suspend fun generateIdentity(name: String): String? {
        bindService()
        return try {
            binder!!.generateIdentity(name)
            null
        } catch (re: RemoteException) {
            Log.e(TAG, "remoteException")
            re.printStackTrace()
            re.localizedMessage
        }
    }

    override suspend fun authorizeIdentity(identity: Identity, packageName: String) {
        bindService()
        binder!!.authorizeApp(identity.fingerprint, packageName)
    }

    override suspend fun deauthorizeIdentity(identity: Identity, packageName: String) {
        bindService()
        Log.v(TAG, "deauthorizing $packageName")
        binder!!.deauthorizeApp(identity.fingerprint, packageName)
    }

    override suspend fun getPermissions(identity: Identity): Flow<List<NamePackage>> = flow {
        bindService()
        val identities = binder!!.getAppPermissions(identity.fingerprint)
        val result = mutableListOf<NamePackage>()
        val pm = context.packageManager
        for (id in identities) {
            yield()
            val r = pm.getApplicationInfo(id, PackageManager.GET_META_DATA)
            result.add(NamePackage(pm.getApplicationLabel(r).toString(), r))
        }
        emit(result)
    }
    
    init {
        Log.v(TAG, "init called")
    }
}