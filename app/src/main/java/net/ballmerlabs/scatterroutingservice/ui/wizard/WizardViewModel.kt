package net.ballmerlabs.scatterroutingservice.ui.wizard

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

@Composable
fun Int.pxToDp() = with(LocalDensity.current) {
    this@pxToDp.toDp()
}


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
    val body: @Composable () -> Unit,
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

    var onBattery: () -> Unit = {}

    val batteryState = mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName))

    val states = mutableStateListOf(
        WizardState(
            title = "Welcome to Scatterbrain!",
            body = {
                val trans = rememberInfiniteTransition("welcome")
                var end by remember { mutableStateOf(IntSize.Zero) }
                var start by remember { mutableStateOf(IntSize.Zero) }
                var person by remember { mutableStateOf(IntSize.Zero) }
                var showfile by remember { mutableStateOf(false) }
                val endWalk = end.width.pxToDp().value- start.width.pxToDp().value - person.width.pxToDp().value
                val walk by trans.animateFloat(
                    initialValue = 0.dp.value,
                    targetValue = endWalk,
                    animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse),
                    label = "walk"
                )

                if (walk in 0.toFloat()..0.5.toFloat())
                    showfile = true
                if (walk in (endWalk-0.5).toFloat()..endWalk)
                    showfile = false

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1F)
                            .onGloballyPositioned { v -> end = v.size },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(60.dp).onGloballyPositioned { v ->
                                start = v.size
                            },
                            painter = painterResource(R.drawable.baseline_groups_24),
                            contentDescription = "Group of people"
                        )
                        Row(
                            modifier = Modifier.wrapContentWidth()
                                .offset(x = walk.dp)
                        ) {
                            if (showfile) {
                                Icon(
                                    modifier = Modifier.size(15.dp),
                                    painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                                    contentDescription = "file"
                                )
                            }
                            Icon(
                                modifier = Modifier.size(30.dp).onGloballyPositioned { v ->
                                    person = v.size
                                },
                                painter = painterResource(R.drawable.baseline_person_outline_24),
                                contentDescription = "person"
                            )

                        }
                    }
                    Icon(
                        modifier = Modifier.size(60.dp),
                        painter = painterResource(R.drawable.baseline_groups_24),
                        contentDescription = "Group of people"
                    )
                }


                Text(
                    text = stringResource(R.string.big_description)
                )
            }
        ),
        WizardState(
            title = "Grant location permission",
            body = { Text(stringResource(R.string.location_description)) },
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
                        body = { Text(stringResource(R.string.wifi_description)) },
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
                        title = ctx.getString(R.string.bluetooth_title),
                        body = { Text(stringResource(R.string.bluetooth_scan_description)) },
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_SCAN,
                            text = "Grant the BLUETOOTH_SCAN permission to discover nearby peers"
                        )
                    )
                )
                add(
                    WizardState(
                        title = ctx.getString(R.string.advertise_title),
                        body = { Text(stringResource(R.string.advertise_description)) },
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_ADVERTISE,
                            text = "Grant the BLUETOOTH_ADVERTISE permission to broadcast device id"
                        )
                    )
                )
                add(
                    WizardState(
                        title = ctx.getString(R.string.connect_title),
                        body = { Text(stringResource(R.string.bluetooth_connect_description)) },
                        permissionState = WizardPermission(
                            permissionState = Manifest.permission.BLUETOOTH_CONNECT,
                            text = "Grant the BLUETOOTH_CONNECT permission to connect to peers in the background"
                        )
                    )
                )
                add(
                    WizardState(
                        title = ctx.getString(R.string.battery_title),
                        body = { Text(stringResource(R.string.battery_description)) },
                        battery = true
                    )
                )

            }
        }
    }
}