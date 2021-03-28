package net.ballmerlabs.scatterroutingservice

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        val repository: ServiceConnectionRepository
) : ViewModel() {
    private val identityLiveData = MediatorLiveData<List<Identity>>()

    init {
        viewModelScope.softCancelLaunch {
            repository.observeIdentities().collect {
                yield()
                identityLiveData.postValue(it)
            }
        }
    }
    
    fun observeMessages(application: String): LiveData<List<ScatterMessage>> = liveData {
        repository.observeMessages(application).collect {
            emit(it)
        }
    }

    fun observeIdentities() : LiveData<List<Identity>> {
        return identityLiveData
    }

    fun refreshIdentities() {
        viewModelScope.launch {
            identityLiveData.postValue(repository.getIdentities())
        }
    }

    fun getApplicationInfo(identity: Identity): LiveData<List<NamePackage>> = liveData {
        repository.getPermissions(identity).collect {
            emit(it)
        }
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
