package net.ballmerlabs.scatterbrain

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import net.ballmerlabs.scatterbrainsdk.Identity

interface ServiceConnectionRepository {

    suspend fun bindService(): Boolean
    suspend fun unbindService(): Boolean
    suspend fun getIdentities(): List<Identity>
    suspend fun observeIdentities(): Flow<List<Identity>>

    companion object {
        val TAG = "ServiceConnectionRepository"
    }
}