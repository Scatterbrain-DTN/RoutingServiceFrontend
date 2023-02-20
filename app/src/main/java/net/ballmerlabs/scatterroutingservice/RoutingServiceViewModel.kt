package net.ballmerlabs.scatterroutingservice

import android.os.FileObserver
import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterroutingservice.ui.debug.LogObserver
import net.ballmerlabs.uscatterbrain.util.LoggerImpl.Companion.LOGS_DIR
import net.ballmerlabs.uscatterbrain.util.initDiskLogging
import net.ballmerlabs.uscatterbrain.util.logsDir
import net.ballmerlabs.uscatterbrain.util.scatterLog
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
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
