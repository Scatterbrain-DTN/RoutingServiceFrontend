package net.ballmerlabs.scatterroutingservice.ui

import androidx.lifecycle.MutableLiveData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.internal.SbApp
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopAddrs
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopPower
import net.ballmerlabs.uscatterbrain.network.desktop.IdentityImportState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesktopObserver @Inject constructor(
    val api: BinderWrapper
) {
    val currentImport = MutableLiveData<IdentityImportState?>(null)
    val desktopPower = MutableLiveData(DesktopPower.DISABLED)
    val appsList = MutableLiveData<ImmutableList<SbApp>>(persistentListOf())
    val connectivityState = MutableLiveData(DesktopAddrs(persistentListOf()))

    init {
        api.coroutineScope.launch {
            val apps = api.getApps().toImmutableList()
            appsList.postValue(apps)
        }
    }

    fun updateApps() {
        api.coroutineScope.launch(Dispatchers.IO) {
            val apps = api.getApps().toImmutableList()
            appsList.postValue(apps)
        }
    }
}