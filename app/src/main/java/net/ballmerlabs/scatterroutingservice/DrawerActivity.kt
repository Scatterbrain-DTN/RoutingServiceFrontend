package net.ballmerlabs.scatterroutingservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.scatterroutingservice.ui.power.PowerToggle
import net.ballmerlabs.scatterroutingservice.ui.theme.ScatterbrainTheme
import net.ballmerlabs.uscatterbrain.RouterPreferences
import net.ballmerlabs.uscatterbrain.isActive
import net.ballmerlabs.uscatterbrain.isPassive
import javax.inject.Inject

val Context.dataStore by preferencesDataStore(name = RouterPreferences.PREF_NAME)

@AndroidEntryPoint
@InternalCoroutinesApi
@OptIn(ExperimentalMaterial3Api::class)
class DrawerActivity : AppCompatActivity() {
    @Inject
    lateinit var repository: BinderWrapper

    @Inject
    lateinit var broadcastReceiver: ScatterbrainBroadcastReceiver

    @Inject
    lateinit var uiBroadcastReceiver: UiBroadcastReceiver

    @InternalCoroutinesApi
    val model: RoutingServiceViewModel by viewModels()

    private val requestCodeBattery = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeBattery) {
            if (resultCode == RESULT_OK) {
            } else {
                //TODO: chastise user
            }
        }
    }

    @Composable
    @ExperimentalPermissionsApi
    fun ScopePermissions(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val permissions = mutableListOf(
            rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (x in listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            ) {
                val p = rememberPermissionState(permission = x)
                permissions.add(p)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val p = rememberPermissionState(permission = Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(p)
        }

        val granted = permissions.all { s ->
            s.status == com.google.accompanist.permissions.PermissionStatus.Granted
        }
        if (granted) {
            Box(modifier = modifier) {
                content()
            }
        } else {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                val p =
                    permissions.first { p -> p.status != com.google.accompanist.permissions.PermissionStatus.Granted }
                Button(
                    onClick = { p.launchPermissionRequest() }
                ) {
                    Text(text = "Permission ${p.permission} not granted")
                }
            }
        }
    }

    @Composable
    fun TopBar(navController: NavController) {
        Column {
            TopAppBar(
                title = { Text(text = getString(R.string.app_name)) },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
            TabSwitcher(navController)
        }
    }

    @Composable
    fun TabSwitcher(navController: NavController) {
        val indices = arrayOf(
            Pair(NAV_POWER, R.drawable.ic_baseline_power_settings_new_24),
            Pair(NAV_IDENTITY, R.drawable.ic_baseline_perm_identity_24),
            Pair(NAV_ABOUT, R.drawable.ic_baseline_info_24)
        )
        val active = remember {
            mutableStateOf(0)
        }
        TabRow(
            selectedTabIndex = active.value,
            indicator = { p ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(-1F)
                        .tabIndicatorOffset(p[active.value])
                )
            }
        ) {
            indices.mapIndexed { i, item ->
                Tab(selected = active.value == i, onClick = {
                    navController.navigate(item.first)
                }) {
                    Icon(
                        painter = painterResource(id = item.second),
                        contentDescription = stringResource(id = R.string.enable_disable),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
        }
    }

    private suspend fun tryStart() {
        try {
            Log.v(TAG, "tryStart")
            model.repository.startService()
            model.repository.bindService(timeout = 6000L)
            Log.v(TAG, "bound service")
            if (isActive(applicationContext)) {
                if(!model.repository.isDiscovering()) model.repository.startDiscover()
            } else if (isPassive(applicationContext)) {
                model.repository.stopDiscover()
                model.repository.startPassive()
            } else {
                model.repository.stopDiscover()
            }
            Log.v(TAG, "discovery ui")
        } catch (exc: RemoteException) {
            Log.e(TAG, "failed to start/bind $exc")
            Toast.makeText(this, "Failed to start background service: $exc", Toast.LENGTH_LONG)
                .show()
        }
    }


    private suspend fun tryPause() {
        try {
            model.repository.unbindService()
        } catch (exc: RemoteException) {
            Toast.makeText(this, "Failed to unbind background service: $exc", Toast.LENGTH_LONG)
                .show()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val controller = rememberNavController()
            ScatterbrainTheme {
                Scaffold(
                    content = { pad ->
                        NavHost(navController = controller, startDestination = NAV_POWER) {
                            composable(NAV_POWER) {
                                ScopePermissions(modifier = Modifier.fillMaxSize()) {
                                    PowerToggle(pad)
                                }
                            }
                            composable(NAV_IDENTITY) { Text(text = "todo") }
                            composable(NAV_ABOUT) { About(pad) }
                        }
                    },
                    topBar = { TopBar(controller) }

                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
        uiBroadcastReceiver.unregister()
        lifecycleScope.launch { tryPause() }
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
        uiBroadcastReceiver.register()
        lifecycleScope.launch { tryStart() }
    }

    companion object {
        const val TAG = "DrawerActivity"
        const val NAV_IDENTITY = "identity"
        const val NAV_POWER = "power"
        const val NAV_ABOUT = "about"
    }
}