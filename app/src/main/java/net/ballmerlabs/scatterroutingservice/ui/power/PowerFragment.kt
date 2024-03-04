package net.ballmerlabs.scatterroutingservice.ui.power

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ui.ScopePermissions
import net.ballmerlabs.uscatterbrain.setActive
import net.ballmerlabs.uscatterbrain.setPassive

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PowerToggle(paddingValues: PaddingValues) {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    val context = LocalContext.current
    val state by model.repository.observeRouterState().observeAsState()
    val bleState by model.observeAdapterState().observeAsState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Top) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(
                checked = state == RouterState.DISCOVERING,
                enabled = bleState == BluetoothState.STATE_ON,
                onCheckedChange = { s ->
                scope.launch {
                    try {
                        if (s) {
                            setActive(context)
                            model.repository.startDiscover()
                        } else {
                            setPassive(context)
                            model.repository.stopDiscover()
                        }
                    } catch (exc: RemoteException) {
                        Toast.makeText(context, "Failed to start discovery $exc", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            })
            Text(text = "Toggle discovery")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
               val permission = rememberPermissionState(permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                var checked by remember { mutableStateOf(false) }
                Column {
                    if (permission.status != PermissionStatus.Granted) {
                        Switch(
                            checked = checked,
                            onCheckedChange = { c -> checked = c }
                        )
                    }
                    if (checked || permission.status == PermissionStatus.Granted) {
                        ScopePermissions(
                            modifier = if (permission.status != PermissionStatus.Granted)
                                Modifier.padding(horizontal = 16.dp)
                            else
                                Modifier,
                            permissions = listOf(permission),
                            text = {
                                "The ACCESS_BACKGROUND_LOCATION permission is required to start the Scatterbrain " +
                                        "service on boot. This is an android restriction on android 14 and newer. " +
                                        "Location data is not collected or shared, this is just a requirement to use wifi. " +
                                        "press this message and select \"Allow all the time\" to enable autostart on boot"
                            }
                        ) {
                            BootSwitch()

                        }
                    }
                }

            } else {
                BootSwitch()
            }
            Text(text = "Start on boot")

        }
    }
}

@Composable
fun BootSwitch(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var enabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                context.getString(R.string.pref_enabled),
                false
            )
        )
    }
    val scope = rememberCoroutineScope()
    Switch(modifier = modifier, checked = enabled, onCheckedChange = { s ->
        scope.launch {
            prefs.edit().apply {
                putBoolean(context.getString(R.string.pref_enabled), s)
                apply()
            }
            enabled = s
        }
    })
}