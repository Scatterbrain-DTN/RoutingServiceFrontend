package net.ballmerlabs.scatterroutingservice

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@InternalCoroutinesApi
@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        private val repository: BinderWrapper,
        private val bluetoothBroadcastReceiver: BluetoothBroadcastReceiver
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

    fun observeAdapterState(): LiveData<BluetoothState> {
        return bluetoothBroadcastReceiver.liveData
    }

    val adapterState: BluetoothState
        get() = bluetoothBroadcastReceiver.state

    fun observeIdentities() : LiveData<List<Identity>> {
        return identityLiveData
    }

    fun refreshIdentities() {
        viewModelScope.launch {
            identityLiveData.postValue(repository.getIdentities())
        }
    }

    fun getPackages(): LiveData<List<String>> = liveData {
        viewModelScope.softCancelLaunch {
            val packages = repository.getPackages()
            emit(packages)
        }
    }

    fun getApplicationInfo(identity: Identity): LiveData<List<NamePackage>> = liveData {
        emit(repository.getPermissions(identity))
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
