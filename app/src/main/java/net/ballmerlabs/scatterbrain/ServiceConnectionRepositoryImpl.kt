package net.ballmerlabs.scatterbrain

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository.Companion.TAG
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceConnectionRepositoryImpl @Inject constructor(
        @ApplicationContext context: Context
) : ServiceConnectionRepository {
    init {
        Log.v(TAG, "init called")
    }
}