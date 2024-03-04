package net.ballmerlabs.scatterroutingservice.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

@Composable
@ExperimentalPermissionsApi
fun ScopePermissions(
    permissions: List<PermissionState>,
    modifier: Modifier = Modifier,
    func: CoroutineScope.() -> Unit = {},
    text: (String) -> String = { v -> "Permission $v not granted" },
    title: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val granted = permissions.all { s ->
        s.status == PermissionStatus.Granted
    }
    LaunchedEffect(granted, func)
    if (granted) {
        Box {
            content()
        }
    } else {
        Box(
            modifier = modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    title()
                    val p =
                        permissions.first { p -> p.status != PermissionStatus.Granted }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { p.launchPermissionRequest() },
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = text(p.permission)
                        )
                    }
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