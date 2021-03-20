package net.ballmerlabs.scatterbrain

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        val repository: ServiceConnectionRepository
) : ViewModel() {
    val serviceConnections = repository.serviceConnections.asLiveData()
    
    fun observeMessages(application: String): LiveData<List<ScatterMessage>> = runBlocking {
        repository.observeMessages(application).asLiveData()
    }

    fun observeIdentities() : LiveData<List<Identity>> = runBlocking {
        repository.observeIdentities().asLiveData()
    }

    fun getApplicationInfo(identity: Identity): LiveData<List<ApplicationInfo>> = runBlocking {
        repository.getPermissions(identity).asLiveData()
    }
}
const val TAG = "RoutingServiceViewModel"
fun CoroutineScope.softCancelLaunch(f: suspend CoroutineScope.() -> Unit): Job {
    return this.launch {
        try {
            f()
        } catch (e: CancellationException) {
            //ignored
        } catch (e : Exception) {
            Log.e(TAG, "EXCEPTION in coroutine: $e")
            e.printStackTrace()
        }
    }
}
