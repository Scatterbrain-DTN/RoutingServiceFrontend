package net.ballmerlabs.scatterbrain

import android.content.pm.ApplicationInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import javax.inject.Inject

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        val repository: ServiceConnectionRepository
) : ViewModel() {
    val serviceConnections = repository.serviceConnections.asLiveData()
    
    suspend fun observeMessages(application: String): LiveData<List<ScatterMessage>> {
        return repository.observeMessages(application).asLiveData()
    }

    suspend fun observeIdentities() : LiveData<List<Identity>> {
        return repository.observeIdentities().asLiveData()
    }

    suspend fun getApplicationInfo(identity: Identity): LiveData<List<ApplicationInfo>> {
        return repository.getPermissions(identity).asLiveData()
    }
}

fun CoroutineScope.softCancelLaunch(f: suspend CoroutineScope.() -> Unit): Job {
    return this.launch {
        try {
            f()
        } catch (e: CancellationException) {
            //ignored
        }
    }
}
