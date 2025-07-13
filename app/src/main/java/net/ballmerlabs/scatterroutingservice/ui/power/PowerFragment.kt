package net.ballmerlabs.scatterroutingservice.ui.power

import android.Manifest
import android.os.Build
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterroutingservice.BluetoothState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ui.SbCard
import net.ballmerlabs.scatterroutingservice.ui.SbSettingsList
import net.ballmerlabs.scatterroutingservice.ui.ScopePermissions
import net.ballmerlabs.scatterroutingservice.ui.isAppInstalled
import net.ballmerlabs.scatterroutingservice.ui.observeAsState
import net.ballmerlabs.scatterroutingservice.ui.wizard.pxToDp
import net.ballmerlabs.uscatterbrain.dataStore
import net.ballmerlabs.uscatterbrain.network.meshtastic.prefix
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
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                modifier = Modifier.padding(end = 16.dp),
                checked = state == RouterState.DISCOVERING,
                enabled = bleState == BluetoothState.STATE_ON,
                onCheckedChange = { s ->
                    scope.launch(Dispatchers.Default) {
                        try {
                            if (s) {
                                setActive(context)
                                model.repository.startDiscover()
                            } else {
                                setPassive(context)
                                model.repository.stopDiscover()
                            }
                        } catch (exc: RemoteException) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context, "Failed to start discovery $exc", Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                })
            Text(text = "Toggle discovery")

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val permission =
                    rememberMultiplePermissionsState(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                var checked by remember { mutableStateOf(false) }
                Column {
                    if (!permission.allPermissionsGranted) {
                        Switch(checked = checked, onCheckedChange = { c -> checked = c })
                    }
                    if (checked || permission.allPermissionsGranted) {
                        ScopePermissions(
                            modifier = if (!permission.allPermissionsGranted) Modifier.padding(
                            horizontal = 16.dp
                        )
                        else Modifier, permissions = permission, text = {
                            "The ACCESS_BACKGROUND_LOCATION permission is required to start the Scatterbrain " + "service on boot and continue operation even when the app is closed. " + "this is an optional requirement, without granting this permission Scatterbrain will still run after " + "you open the app manually. " + "The location data accessed by this permission is only collected to use bluetooth and wifi " + "in the background. It is not shared and it does not leave your device. " + "This is an android restriction on android 14 and newer. " + "press this message and select \"Allow all the time\" to enable autostart on boot, or leave this permission " + "ungranted to use Scatterbrain after manually starting it."
                        }, failText = {
                            "ACCESS_BACKGROUND_LOCATION permission is not granted. Scatterbrain wont start on boot, " + "but will work just fine after the app is opened manually"
                        }) {
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
            scope.launch(Dispatchers.Default) {
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
    val state = rememberLazyListState()
    val metrics by model.repository.observeMetrics().observeAsState()
    LaunchedEffect(key1 = model) {
        model.repository.getMetrics()
    }
    val m = metrics?.metrics
    if (m != null) {
        LazyColumn(
            state = state, modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (stat in m) {
                val signed = stat.signed
                val unsigned = stat.messages - stat.signed
                val lastSeen = Date(stat.lastSeen)
                item {
                    SbCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = stat.application,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "signed: $signed\nunsigned: $unsigned\nlast seen: $lastSeen"
                            )
                        }
                    }
                }
            }
        }
    } else {
        Text(text = "No metrics yet")
    }
}

@Composable
fun EnabledTransports(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: RoutingServiceViewModel = hiltViewModel()
    val prefSettings  = stringResource(R.string.pref_enabled_transports)
    val prefs = context.dataStore
    val scope = rememberCoroutineScope()
    val appInstalled = context.isAppInstalled(prefix)
    val default = remember {
        if (appInstalled)
            setOf("wifi", "bluetooth", "meshtastic")
        else
            setOf("wifi", "bluetooth")
    }


    val transports by remember {  prefs.data.map { pref ->
        pref[stringSetPreferencesKey(prefSettings)]?:default
    } }.collectAsState(default)
    Column(modifier = modifier) {
        Text("Enabled transports", style = MaterialTheme.typography.titleMedium)
        for (name in default)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = transports.contains(name), onCheckedChange = { c ->
                    scope.launch {
                        prefs.edit { p ->
                            val set = (if (c)
                                 transports + setOf(name)
                            else
                                transports - setOf(name)).toMutableSet()

                            if (set.contains("wifi") && !set.contains("bluetooth")) {
                                set.remove("wifi")
                            }


                            try {
                                if (!set.contains("bluetooth")) {
                                    viewModel.repository.stopDiscover()
                                } else {
                                    viewModel.repository.startDiscover()
                                }
                            } catch (exc: Exception) {
                                Log.e("debug", "failed to start/stop discover: $exc")
                            }

                            try {
                                if (set.contains("meshtastic") && appInstalled) {
                                    viewModel.repository.startMeshtastic()
                                }
                            } catch (exc: Exception) {
                                Log.e("debug", "failed to start/stop meshtastic: $exc")
                            }

                            if (!appInstalled)
                                set.remove("meshtastic")

                            Log.v("debug", "settings prefs $set")

                            p[stringSetPreferencesKey(prefSettings)] = set
                        }
                    }
                })
                Text(name)
            }
    }
}

@Composable
fun PowerToggle() {
    var containerHeight by remember { mutableIntStateOf(0) }
    val titleStyle = MaterialTheme.typography.titleMedium
    val titleModifier = Modifier
    var blockHeight by remember { mutableIntStateOf(0) }
    var identityHeight by remember { mutableIntStateOf(0) }
    var listHeight =  (containerHeight.pxToDp() - (blockHeight.pxToDp() + identityHeight.pxToDp()))
    if (listHeight <= 0.dp) {
        listHeight = 200.dp
    }

    SbSettingsList()
        .item(modifier = Modifier.onGloballyPositioned { coords ->
            blockHeight = coords.size.height
        }) {
            Text(modifier = titleModifier, text = "Router state", style = titleStyle)
            ToggleBox()
            EnabledTransports(modifier = Modifier.fillMaxWidth())
        }
        .item(modifier = Modifier.onGloballyPositioned { coords ->
            identityHeight = coords.size.height
        }) {
            Text(modifier = titleModifier, text = "Identity", style = titleStyle)
            LuidView(modifier = Modifier)
        }
        .item(modifier = Modifier.sizeIn(minHeight = listHeight)) {
            Text(
                modifier = titleModifier,
                text = "Recently seen applications:",
                style = titleStyle
            )
            MetricsView(modifier = Modifier
                .fillMaxHeight(1F)
                .weight(1F))
        }.Display(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned { coords -> containerHeight = coords.size.height },
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        )
}

@Composable
fun BootSwitch(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val enabled by prefs.observeAsState(context.getString(R.string.pref_enabled), false)
    val scope = rememberCoroutineScope()
    Switch(modifier = modifier.padding(end = 16.dp), checked = enabled, onCheckedChange = { s ->
        scope.launch {
            prefs.edit().apply {
                putBoolean(context.getString(R.string.pref_enabled), s)
                apply()
            }
        }
    })
}