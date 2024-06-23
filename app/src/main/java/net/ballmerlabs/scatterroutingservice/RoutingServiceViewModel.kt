package net.ballmerlabs.scatterroutingservice

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterroutingservice.ui.debug.LogObserver
import net.ballmerlabs.uscatterbrain.util.scatterLog
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
    application: ScatterbrainApp,
    val logObserver: LogObserver,
    val repository: BinderWrapper,
    val uiBroadcastReceiver: UiBroadcastReceiver
) : AndroidViewModel(application) {

    val log by scatterLog()

    val permissionGranted = MutableLiveData(false)

    val identities: MutableLiveData<List<Identity>> =  MutableLiveData<List<Identity>>(listOf())
    fun observeAdapterState(): LiveData<BluetoothState> {
        return uiBroadcastReceiver.liveData
    }

    val adapterState: BluetoothState
        get() = uiBroadcastReceiver.state


    fun getPermissions(identity: Identity): LiveData<List<NamePackage>> {
        val ld = MutableLiveData<List<NamePackage>>()
        viewModelScope.softCancelLaunch {
            ld.postValue(repository.getPermissions(identity))
        }
        return ld
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onPermissionsGranted() {
        repository.coroutineScope.launch(Dispatchers.Main) {
            try {
                val ids = repository.getIdentities()
                identities.postValue(ids)
                repository.observeIdentities().collect { id ->
                    identities.postValue(id)
                }
            } catch (exc: Exception) {
                log.e("failed to initial fetch identities")
            }
        }
    }

    fun getPackages(): LiveData<List<NamePackage>> {
        val ld = MutableLiveData<List<NamePackage>>()
        viewModelScope.softCancelLaunch {
            ld.postValue(repository.getPackages())
        }
        return ld
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
