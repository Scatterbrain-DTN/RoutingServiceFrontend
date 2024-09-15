package net.ballmerlabs.scatterroutingservice.ui.wizard

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.ScatterbrainApp
import net.ballmerlabs.scatterroutingservice.ui.PermissionSingleDialog
import javax.inject.Inject

data class WizardPermission constructor(
    val permissionState: String,
    val text: String,
) {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun Show(
        state: MutableState<Boolean>,
        permission: PermissionState = rememberPermissionState(permissionState),
    ) {
        PermissionSingleDialog(
            permission,
            text,
            state
        )
    }
}

data class WizardState(
    val title: String,
    val text: String,
    val button: String = "Next",
    val battery: Boolean = false,
    val permissionState: WizardPermission? = null,
) {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun Button(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
        val state = remember { mutableStateOf(false) }
        val permission: PermissionState? = if (permissionState != null)
            rememberPermissionState(permissionState.permissionState)
        else
            null
        if (permission == null || permission.status == PermissionStatus.Granted) {
            OutlinedButton(modifier = modifier,onClick = onClick) {
                Text(button)
            }
        } else {
            permissionState?.Show(state)
            FilledTonalButton(modifier = modifier,onClick = { state.value = true }) {
                Text("Grant permission")
            }
        }
    }
}

@HiltViewModel
class WizardViewModel @Inject constructor(
    application: ScatterbrainApp,
    @ApplicationContext val ctx: Context,
    pm: PowerManager
) : AndroidViewModel(application) {
    private val done = mutableListOf<WizardState>()

    var onBattery: () -> Unit = {}

    val batteryState = mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName))

    val states = mutableStateListOf(
        WizardState(
            title = "Welcome to Scatterbrain!",
            text = ctx.getString(R.string.big_description)
        ),
        WizardState(
            title = "Grant location permission",
            text = "The ACCESS_FINE_LOCATION permission is required in order to " +
                    "enable background bluetooth connections while the app is closed. This collects location data for the sole purpose " +
                    "of bluetooth network. This data is not shared and does not leave your device. " +
                    "Android requires this permission to access wifi and bluetooth. This is a requirement set " +
                    "by google to preserve privacy when collecting location data from external device IDs " +
                    "\n" +
                    "This location permission does not actually use the GPS location, it only allows Scatterbrain to " +
                    "access the bluetooth adapter. The reason your device requires ACCESS_FINE_LOCATION for bluetooth " +
                    "is because some apps use 3rd party bluetooth devices to find your devices approximate location by " +
                    "looking up their device ids. Scatterbrain does not do this.",
            permissionState = WizardPermission(
                permissionState = Manifest.permission.ACCESS_FINE_LOCATION,
                text = "Grant the ACCESS_FINE_LOCATION to allow wifi and bluetooth connections when the app" +
                        "is closed?"
            )
        )
    )

    init {
        states.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    WizardState(
                        title = "Grant wifi permission",
                        text = "The NEARBY_WIFI_DEVICES permission is required to perform" +
                                " wifi direct operations in the background on newer devices" +
                                " this is used in tandem with bluetooth to speed to transfers to nearby" +
                                " devices that are capable of wifi direct",
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.NEARBY_WIFI_DEVICES,
                            text = "Grant NEARBY_WIFI_DEVICES permission to connect to wifi in the background"
                        )
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    WizardState(
                        title = "Grant bluetooth scan permissions",
                        text = "The BLUETOOTH_SCAN permission is required on newer devices to discover" +
                                " Scatterbrain peers in the background via bluetooth",
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_SCAN,
                            text = "Grant the BLUETOOTH_SCAN permission to discover nearby peers"
                        )
                    )
                )
                add(
                    WizardState(
                        title = "Grant bluetooth advertise permission",
                        text = "The BLUETOOTH_ADVERTISE permission is used to broadcast an anonymous id that nearby devices can connect to." +
                                "The id is randomized frequently to prevent tracking.",
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_ADVERTISE,
                            text = "Grant the BLUETOOTH_ADVERTISE permission to broadcast device id"
                        )
                    )
                )
                add(
                    WizardState(
                        title = "Grant bluetooth connect permission",
                        text = "The BLUETOOTH_CONNECT permission is used to connect to Scatterbrain peers in the background using BLE",
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_CONNECT,
                            text = "Grant the BLUETOOTH_CONNECT permission to connect to peers in the background"
                        )
                    )
                )
                add(
                    WizardState(
                        title = "Optionally grant permission to ignore battery optimizations",
                        text = ctx.getString(R.string.battery_description),
                        battery = true
                    )
                )

            }
        }
    }

    fun popState() {
        if (states.isNotEmpty())
            done.add(states.removeLast())
    }

    fun pushState() {
        if (done.isNotEmpty())
            states.add(done.removeLast())
    }

    fun isStart(): Boolean {
        return done.isEmpty()
    }
}