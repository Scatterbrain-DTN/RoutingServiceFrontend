package net.ballmerlabs.scatterroutingservice.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import java.util.Optional


@Composable
fun SbCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    padding: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardColors(
            containerColor = color,
            disabledContainerColor = color,
            contentColor = MaterialTheme.colorScheme.contentColorFor(color),
            disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                color
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = contentAlignment
        ) {
            content()
        }

    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScopeScatterbrainPermissions(
    modifier: Modifier = Modifier,
    onGrant: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val model: RoutingServiceViewModel = hiltViewModel()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        for (x in listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )) {
            permissions.add(x)
        }
    } else {
        for (x in listOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
        )) {
            permissions.add(x)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
    val p = rememberMultiplePermissionsState(permissions = permissions.toList())
    ScopePermissions(
        p,
        modifier = modifier,
        func = {
            onGrant()
        },
        title = {
            Text(
                text =
                "The following permissions need to be granted for Scatterbrain to operate. " +
                        "push the below button to grant the permission:",
                style = MaterialTheme.typography.labelLarge
            )
        },
        text = { p ->
            when (p) {
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> "The $p permission is required in order to " +
                        "enable background bluetooth connections while the app is closed. This collects location data for the sole purpose " +
                        "of bluetooth network. This data is not shared and does not leave your device. " +
                        "Android requires this permission to access wifi and bluetooth. This is a requirement set " +
                        "by google to preserve privacy when collecting location data from external device IDs "

                Manifest.permission.BLUETOOTH_SCAN -> "The BLUETOOTH_SCAN permission is required on newer devices to discover" +
                        " Scatterbrain peers in the background via bluetooth"

                Manifest.permission.NEARBY_WIFI_DEVICES -> "The NEARBY_WIFI_DEVICES permission is required to perform" +
                        " wifi direct operations in the background on newer devices"

                Manifest.permission.BLUETOOTH_CONNECT -> "The BLUETOOTH_CONNECT permission is used to connect to Scatterbrain peers in the background using BLE"
                Manifest.permission.BLUETOOTH_ADVERTISE -> "The BLUETOOTH_ADVERTISE permission is used to broadcast an anonymous id that nearby devices can connect to"
                else -> p
            }
        },
        failText = { p -> "Permission $p not granted, Scatterbrain cannot start. Push this button to try again." }
    ) {
        SideEffect {
            model.onPermissionsGranted()
        }
        content()
    }
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun PermissionSingleDialog(
    permission: PermissionState,
    text: String,
    dialog: MutableState<Boolean>,
) {

    var openMainDialog by dialog

    if (openMainDialog && permission.status != PermissionStatus.Granted) {
        AlertDialog(
            title = { Text(text = "Permission required") },
            text = { Text(text = text, color = MaterialTheme.colorScheme.onBackground) },
            onDismissRequest = { openMainDialog = false },
            confirmButton = {
                Button(
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        disabledContentColor = MaterialTheme.colorScheme.surfaceDim,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    onClick = {
                        permission.launchPermissionRequest()
                        openMainDialog = false
                    }
                ) {
                    Text(
                        color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondary),
                        text = "Grant"
                    )
                }
            },
            dismissButton = {
                Button(onClick = { openMainDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
@ExperimentalPermissionsApi
fun ScopePermissions(
    permissions: MultiplePermissionsState,
    modifier: Modifier = Modifier,
    func: CoroutineScope.() -> Unit = {},
    text: (String) -> String = { v -> "Permission $v not granted" },
    failText: (String) -> String = { v -> text(v) },
    title: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val granted = permissions.allPermissionsGranted
    val openMainDialog = remember {
        mutableStateOf(true)
    }
    LaunchedEffect(granted, func)
    if (granted) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        Box(
            modifier = modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalArrangement = Arrangement.Top
            ) {
                title()
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openMainDialog.value = true },
                ) {
                    val p =
                        permissions.permissions.first { v -> v.status != PermissionStatus.Granted }
                    PermissionSingleDialog(
                        permission = p,
                        text = text(p.permission),
                        dialog = openMainDialog
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = failText(p.permission),
                    )
                }
            }
        }
    }
}

suspend fun checkPermission(permission: String, context: Context): Boolean =
    suspendCancellableCoroutine { c ->
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            c.resumeWith(Result.success(true))
        } else {

            c.resumeWith(Result.success(false))
        }

    }

suspend fun checkPermission(context: Context): Optional<String> {
    var res: Optional<String> = Optional.empty()
    if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)) {
        res = Optional.of(Manifest.permission.ACCESS_FINE_LOCATION)
    } else if (!checkPermission(
            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
            context
        ) && Build.VERSION.SDK_INT >= 34
    ) {
        res = Optional.of(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (perm in arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            ) {
                if (!checkPermission(perm, context)) {
                    res = Optional.of(perm)
                    break
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.NEARBY_WIFI_DEVICES
            val check = checkPermission(
                perm,
                context
            )

            if (!check) {
                res = Optional.of(perm)
            }
        }
    }
    return res
}