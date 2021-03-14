package net.ballmerlabs.scatterbrain

interface ServiceConnectionRepository {

    suspend fun bindService(): Boolean?
    suspend fun unbindService(): Boolean?

    companion object {
        val TAG = "ServiceConnectionRepository"
    }
}