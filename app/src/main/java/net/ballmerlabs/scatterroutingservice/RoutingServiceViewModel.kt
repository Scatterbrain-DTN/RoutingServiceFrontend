package net.ballmerlabs.scatterroutingservice

import android.content.Context
import android.net.wifi.WifiManager
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterroutingservice.db.UiDatastore
import net.ballmerlabs.scatterroutingservice.ui.DesktopObserver
import net.ballmerlabs.scatterroutingservice.ui.debug.LogObserver
import net.ballmerlabs.uscatterbrain.util.scatterLog
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
    application: ScatterbrainApp,
    val logObserver: LogObserver,
    val repository: BinderWrapper,
    val uiBroadcastReceiver: UiBroadcastReceiver,
    val datastore: UiDatastore,
    val desktopObserver: DesktopObserver,
    val wifiManager: WifiManager,
) : AndroidViewModel(application) {

    val log by scatterLog()
    val devices = HashMap<String, RxBleDevice>()
    var currentScan: Flowable<RxBleDevice>? = null

    val identities: MutableLiveData<List<Identity>> = MutableLiveData<List<Identity>>(listOf())

    fun observeAdapterState(): LiveData<BluetoothState> {
        return uiBroadcastReceiver.liveData
    }

    fun getPermissions(identity: UUID): LiveData<List<NamePackage>> {
        val ld = MutableLiveData<List<NamePackage>>()
        viewModelScope.softCancelLaunch {
            ld.postValue(repository.getPermissions(identity))
        }
        return ld
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onPermissionsGranted() {
        repository.coroutineScope.launch(Dispatchers.IO) {
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

    fun bleScan(context: Context): LiveData<List<RxBleDevice>> {
        val client = RxBleClient.create(context)
        val current = currentScan
        val scan = if (current == null) {
            val scan = client.scanBleDevices(
                ScanSettings.Builder()
                    .build(),
                ScanFilter.Builder()
                    //          .setServiceUuid(ParcelUuid(UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd")))
                    .build()
            ).doFinally { currentScan = null }
                .toFlowable(BackpressureStrategy.BUFFER)
                .map { v -> v.bleDevice }
                .cache()
            currentScan = scan
            scan
        } else {
            current
        }
        return scan
            .map { res ->
                devices[res.macAddress] = res
                devices.values.toList()
            }
            .toLiveData()
    }
}

const val TAG = "RoutingServiceViewModel"
fun CoroutineScope.softCancelLaunch(f: suspend CoroutineScope.() -> Unit): Job {
    return this.launch {
        try {
            f()
        } catch (e: CancellationException) {
            //ignored
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION in coroutine: $e")
            e.printStackTrace()
        }
    }
}
