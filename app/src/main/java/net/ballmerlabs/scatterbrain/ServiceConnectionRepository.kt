package net.ballmerlabs.scatterbrain

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage

interface ServiceConnectionRepository {

    suspend fun bindService(): Boolean
    suspend fun unbindService(): Boolean
    suspend fun getIdentities(): List<Identity>
    suspend fun getScatterMessages(application: String): List<ScatterMessage>
    suspend fun observeIdentities(): Flow<List<Identity>>
    suspend fun observeMessages(application: String): Flow<List<ScatterMessage>>
    suspend fun generateIdentity(name: String)

    companion object {
        val TAG = "ServiceConnectionRepository"
    }
}