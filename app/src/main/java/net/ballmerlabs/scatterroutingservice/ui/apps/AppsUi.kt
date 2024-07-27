package net.ballmerlabs.scatterroutingservice.ui.apps

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cash.z.ecc.android.bip39.Mnemonics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ui.SbCard
import net.ballmerlabs.uscatterbrain.dataStore
import net.ballmerlabs.uscatterbrain.network.LibsodiumInterface
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopAddrs
import net.ballmerlabs.uscatterbrain.network.desktop.DesktopPower
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.random.Random

fun sbName(): String {
    val i = Random(Date().time).nextInt().absoluteValue % 1024
    return "sb$i"
}

@Composable
fun PairingRequestDialog(navController: NavController) {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    val appName = navController.currentBackStackEntry?.arguments?.getString("name")
    val fingeprint = navController.currentBackStackEntry?.arguments?.getString("fingerprint")
    val id = if (fingeprint != null) LibsodiumInterface.base64decUrl(fingeprint) else null
    val coin =
        if (id != null) Mnemonics.MnemonicCode(id).words.joinToString { v -> String(v) } else null
    Surface(
        modifier = Modifier
            .background(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.background
            )
            .padding(4.dp)
    ) {

        Column {
            Text(text = "Pairing request from $appName\n\n$coin")
            Row {
                Button(onClick = {
                    navController.popBackStack()
                    scope.launch {
                        if (id != null)
                            model.repository.authorizeDesktop(id, true)
                    }
                }) {
                    Text(text = "Accept")
                }
                Button(onClick = {
                    navController.popBackStack()
                    scope.launch {
                        if (id != null)
                            model.repository.authorizeDesktop(id, false)
                    }
                }) {
                    Text(text = "Deny")
                }
            }

        }
    }
}


@Composable
fun AppCard(name: String, ident: String?, desktop: Boolean, modifier: Modifier = Modifier) {
    val id = (ident ?: "").trim()
    val color =
        if (desktop) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.secondaryContainer
    SbCard(modifier = modifier, padding = 8.dp, color = color) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                if (desktop) {
                    Text(text = "Key fingerprint:\n$id")
                } else {
                    Text(text = "Package name:\n$id")
                }
            }

            if (desktop)
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(text = "Desktop")
                }
            else
                Badge(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(text = "Mobile")
                }
        }

    }
}

@Composable
fun AppsList(modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    val model: RoutingServiceViewModel = hiltViewModel()
    val apps by model.desktopObserver.appsList.observeAsState(persistentListOf())
    val ctx = LocalContext.current

    Column {
        Text(text = "Connected apps")
        LazyColumn(
            modifier = modifier,
            state = state,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (app in apps.filter { v -> v.id != ctx.packageName }) {
                Log.v("debug", "recompose app ${app.id}")
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        name = app.name,
                        ident = app.id,
                        desktop = app.desktop
                    )
                }
            }
        }
    }
}

@Composable
fun ToggleView(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.dataStore }
    val switchState by model.desktopObserver.desktopPower.observeAsState()
    val connectivity by model.desktopObserver.connectivityState.observeAsState(
        DesktopAddrs(
            persistentListOf()
        )
    )
    var name by remember {
        mutableStateOf(sbName())
    }

    Column(modifier = modifier) {
        Text(text = "Connection settings")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                modifier = Modifier.weight(1F),
                value = name,
                onValueChange = { v ->
                    name = v
                }
            )
            Switch(checked = switchState == DesktopPower.ENABLED, onCheckedChange = { checked ->
                scope.launch {
                    prefs.edit { prefs ->
                        prefs[stringPreferencesKey(
                            context.getString(R.string.pref_desktop_name)
                        )] = name

                        prefs[booleanPreferencesKey(
                            context.getString(R.string.pref_desktop)
                        )] = checked

                    }
                    if (checked)
                        model.repository.startDesktopApi(name)
                    else
                        model.repository.stopDesktopApi()
                }
            })
        }

        if (connectivity.addrs.isNotEmpty()) {
            val hostnames = connectivity.addrs.map { v ->
                val addr = if (v.ipv6)
                    Inet6Address.getByAddress(v.addr)
                else
                    Inet4Address.getByAddress(v.addr)

                "${addr.hostAddress}:${v.port}"
            }.joinToString(separator = "\n")

            Text(text = "Current hostnames:\n$hostnames")
        }
    }
}

@Composable
fun AppsView(modifier: Modifier = Modifier) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "WARNING: Desktop API is in alpha", style = MaterialTheme.typography.titleMedium)
        ToggleView(modifier = Modifier.fillMaxWidth())
        AppsList(modifier = Modifier.fillMaxWidth())
    }
}