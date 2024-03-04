package net.ballmerlabs.scatterroutingservice.ui.power

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
import java.util.Date

@OptIn(ExperimentalPermissionsApi::class)

@Composable
fun ToggleBox(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by model.repository.observeRouterState().observeAsState()
    val bleState by model.observeAdapterState().observeAsState()
    Column(
        modifier
            .fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Top) {
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
                            Toast.makeText(
                                context,
                                "Failed to start discovery $exc",
                                Toast.LENGTH_LONG
                            )
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
                val permission =
                    rememberMultiplePermissionsState(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                var checked by remember { mutableStateOf(false) }
                Column {
                    if (!permission.allPermissionsGranted) {
                        Switch(
                            checked = checked,
                            onCheckedChange = { c -> checked = c }
                        )
                    }
                    if (checked || permission.allPermissionsGranted) {
                        ScopePermissions(
                            modifier = if (!permission.allPermissionsGranted)
                                Modifier.padding(horizontal = 16.dp)
                            else
                                Modifier,
                            permissions = permission,
                            text = {
                                "The ACCESS_BACKGROUND_LOCATION permission is required to start the Scatterbrain " +
                                        "service on boot and continue operation even when the app is closed. " +
                                        "this is an optional requirement, without granting this permission Scatterbrain will still run after " +
                                        "you open the app manually. " +
                                        "The location data accessed by this permission is only collected to use bluetooth and wifi " +
                                        "in the background. It is not shared and it does not leave your device. " +
                                        "This is an android restriction on android 14 and newer. " +
                                        "press this message and select \"Allow all the time\" to enable autostart on boot, or leave this permission " +
                                        "ungranted to use Scatterbrain after manually starting it."
                            },
                            failText = {
                                "ACCESS_BACKGROUND_LOCATION permission is not granted. Scatterbrain wont start on boot, " +
                                        "but will work just fine after the app is opened manually"
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
fun LuidView(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val state by model.repository.observeLuid().observeAsState()
    Column(modifier = modifier) {
        Text(text = "Current router id:")
        Text(text = "${state?.uuid}")
        Button(onClick = {
            scope.launch {
                model.repository.randomizeLuid()
            }
        }) {
            Text(text = "Randomize Luid")
        }
    }
}

@Composable
fun MetricsView(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val metrics by model.repository.observeMetrics().observeAsState()
    val m = metrics?.metrics
    Column(modifier = modifier) {
        if (m != null) {
            for (stat in m) {
                val signed = stat.signed
                val unsigned = stat.messages - stat.signed
                val lastSeen = Date(stat.lastSeen)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = stat.application, style = MaterialTheme.typography.titleMedium)
                        Text(text ="signed: $signed\n" +
                                "unsigned: $unsigned\n" +
                                "last seen: $lastSeen")
                    }
                }
            }
        } else {
            Text(text = "No metrics yet")
        }
    }
}

@Composable
fun PowerToggle() {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .scrollable(scrollState, Orientation.Vertical),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        val titleStyle = MaterialTheme.typography.titleMedium
        val titleModifier = Modifier
        Text(modifier = titleModifier, text = "Router state", style = titleStyle)
        ToggleBox()
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(modifier = titleModifier, text = "Identity", style = titleStyle)
        LuidView()
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(modifier = titleModifier, text = "Recently seen applications:", style = titleStyle)
        MetricsView()
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