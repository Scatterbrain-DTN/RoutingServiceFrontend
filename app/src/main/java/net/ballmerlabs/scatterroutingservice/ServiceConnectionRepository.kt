package net.ballmerlabs.scatterroutingservice

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.flow.Flow
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage

interface ServiceConnectionRepository {
    val serviceConnections: Flow<Boolean>
    suspend fun startService()
    suspend fun stopService()
    suspend fun bindService()
    suspend fun unbindService(): Boolean
    suspend fun getIdentities(): List<Identity>
    suspend fun getScatterMessages(application: String): List<ScatterMessage>
    suspend fun observeIdentities(): Flow<List<Identity>>
    suspend fun observeMessages(application: String): Flow<List<ScatterMessage>>
    suspend fun generateIdentity(name: String): String?
    suspend fun getPermissions(identity: Identity): Flow<List<NamePackage>>
    suspend fun authorizeIdentity(identity: Identity, packageName: String)
    suspend fun deauthorizeIdentity(identity: Identity, packageName: String)
    suspend fun removeIdentity(identity: Identity): Boolean
    suspend fun startDiscover()
    suspend fun stopDiscover()
    suspend fun startPassive()
    suspend fun stopPassive()
    fun isConnected(): Boolean
    companion object {
        val TAG = "ServiceConnectionRepository"
    }
}


class NamePackage(
        val name: String,
        val info: ApplicationInfo
) {
    override fun toString(): String {
        return name
    }
}