package net.ballmerlabs.scatterroutingservice

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    val logObserver: LogObserver
) : AndroidViewModel(application) {

    val log by scatterLog()

    @Inject
    lateinit var repository: BinderWrapper

    @Inject
    lateinit var uiBroadcastReceiver: UiBroadcastReceiver

    private val identityLiveData = MediatorLiveData<List<Identity>>()

    val permissionGranted = MutableLiveData(false)

    fun observeAdapterState(): LiveData<BluetoothState> {
        return uiBroadcastReceiver.liveData
    }

    val adapterState: BluetoothState
        get() = uiBroadcastReceiver.state

    fun observeIdentities() : LiveData<List<Identity>> {
        return identityLiveData
    }

    fun refreshIdentities() {
        viewModelScope.launch {
            identityLiveData.postValue(repository.getIdentities())
        }
    }

    fun getPackages(): LiveData<List<NamePackage>> = liveData {
        viewModelScope.softCancelLaunch {
            val packages = repository.getPackages()
            emit(packages)
        }
    }

    init {
        Log.e("debug", "init viewmodel")
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
