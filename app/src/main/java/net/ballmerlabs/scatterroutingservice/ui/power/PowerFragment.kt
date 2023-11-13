package net.ballmerlabs.scatterroutingservice.ui.power

import android.content.res.Configuration
import android.net.Uri
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.uscatterbrain.setActive
import net.ballmerlabs.uscatterbrain.setPassive

@Composable
fun PowerFragment(paddingValues: PaddingValues) {
    Button(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxWidth()
            .fillMaxHeight(),
        onClick = { /*TODO*/ }
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            painter = painterResource(id = R.drawable.ic_baseline_router_disabled),
            contentDescription = stringResource(id = R.string.enable_disable)
        )
    }
}

@Composable
fun DatastoreBackup(padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        scope.launch(Dispatchers.IO) {
            model.repository.dumpDatastore(uri)
        }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = {
                launcher.launch("output.sqlite")
            }) {
                Text(text = "Export database")
            }
        }
    }
}

@Composable
fun PowerToggle(paddingValues: PaddingValues) {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    val context = LocalContext.current
    val state = model.repository.observeRouterState().observeAsState()
    Column(Modifier.fillMaxSize().padding(paddingValues), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Top) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(checked = state.value == RouterState.DISCOVERING, onCheckedChange = { s ->
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
            val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
            var enabled by remember {
                mutableStateOf(
                    prefs.getBoolean(
                        context.getString(R.string.pref_enabled),
                        true
                    )
                )
            }
            Switch(checked = enabled, onCheckedChange = { s ->
                scope.launch {
                    prefs.edit().apply {
                        putBoolean(context.getString(R.string.pref_enabled), s)
                        apply()
                    }
                    enabled = s
                }
            })
            Text(text = "Start on boot")
        }
        DatastoreBackup(padding = paddingValues)
    }
}