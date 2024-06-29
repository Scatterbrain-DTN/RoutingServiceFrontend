package net.ballmerlabs.scatterroutingservice

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.scatterroutingservice.ui.ScopeScatterbrainPermissions
import net.ballmerlabs.scatterroutingservice.ui.chat.ChatView
import net.ballmerlabs.scatterroutingservice.ui.debug.DebugView
import net.ballmerlabs.scatterroutingservice.ui.power.PowerToggle
import net.ballmerlabs.scatterroutingservice.ui.theme.ScatterbrainTheme
import net.ballmerlabs.uscatterbrain.isActive
import net.ballmerlabs.uscatterbrain.isPassive
import net.ballmerlabs.uscatterbrain.util.initDiskLogging
import net.ballmerlabs.uscatterbrain.util.logsDir
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

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

    @SuppressLint("BatteryLife")
    private fun ignoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if(!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }



    @Composable
    fun TopBar(navController: NavController) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        Column {
            TopAppBar(
                title = { Text(text = navBackStackEntry?.destination?.route?:getString(R.string.app_name)) },
                colors = TopAppBarColors(
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
            TabSwitcher(navController)
        }
    }

    @Composable
    fun TabSwitcher(navController: NavController) {
        val indices = arrayOf(
            Pair(NAV_CHAT, R.drawable.baseline_chat_24),
            Pair(NAV_POWER, R.drawable.ic_baseline_power_settings_new_24),
            Pair(NAV_IDENTITY, R.drawable.ic_baseline_perm_identity_24),
            Pair(NAV_DEBUG, R.drawable.baseline_settings_applications_24),
            Pair(NAV_ABOUT, R.drawable.ic_baseline_info_24)

        )
        val active = remember {
            mutableIntStateOf(0)
        }
        TabRow(
            selectedTabIndex = active.intValue,
            indicator = { p ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(-1F)
                        .tabIndicatorOffset(p[active.intValue])
                )
            }
        ) {
            indices.mapIndexed { i, item ->
                Tab(selected = active.intValue == i, onClick = {
                    navController.navigate(item.first)
                }) {
                    Icon(
                        painter = painterResource(id = item.second),
                        contentDescription = stringResource(id = R.string.enable_disable),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
        }
    }

    private suspend fun tryStart() {
        try {
            if(checkSelfPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
                Log.v(TAG, "tryStart")
                model.repository.startService()
                model.repository.bindService(timeout = 6000L)
                Log.v(TAG, "bound service")
                if (isActive(applicationContext)) {
                    model.repository.startDiscover()
                } else if (isPassive(applicationContext)) {
                    model.repository.stopDiscover()
                    model.repository.startPassive()
                } else {
                    model.repository.stopDiscover()
                }
                Log.v(TAG, "discovery ui")
            }
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

    @Composable
    fun CreateIdentityDialog(navController: NavController) {
        val model: RoutingServiceViewModel = hiltViewModel()
        val scope = rememberCoroutineScope()
        val ctx = LocalContext.current
        Surface(
            modifier = Modifier
                .background(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.background
                )
                .padding(4.dp)
        ) {
            var name by remember {
                mutableStateOf("")
            }
            Column {
                Text(text = "Create Identity")
                TextField(
                    value = name,
                    onValueChange = { v -> name = v},
                    placeholder = { Text(text = "Identity name") }
                )
                Row {
                    Button(onClick = { scope.launch {
                        try {
                            val id = model.repository.generateIdentity(name)
                            Toast.makeText(ctx, "Created identity ${id.fingerprint}", Toast.LENGTH_LONG).show()
                        } catch (exc: RemoteException) {
                            Toast.makeText(ctx, "Failed to create identity: $exc", Toast.LENGTH_LONG).show()
                        }
                        navController.popBackStack()
                    } }) {
                        Text(text = "Create")
                    }
                    Button(onClick = { navController.popBackStack() }) {
                        Text(text = "Cancel")
                    }
                }
            }

        }
    }

    @Composable
    fun Fab(navController: NavController) {
        var hidefab by remember {
            mutableStateOf(false)
        }
        var dest by remember {
            mutableStateOf<String?>(null)
        }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/zip"
            )
        ) { uri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val dir = logsDir!!

                    ZipOutputStream(BufferedOutputStream(context.contentResolver.openOutputStream(uri!!))).use { zip ->
                        dir.walkTopDown().forEach { f ->
                            val name = f.absolutePath.removePrefix(dir.absolutePath).removePrefix("/")
                            val entry = ZipEntry("$name${( if(f.isDirectory) "/" else "" )}")
                            zip.putNextEntry(entry)
                            if(f.isFile) {
                                f.inputStream().use { stream ->
                                    stream.copyTo(zip)
                                }
                            }
                        }
                    }
                } catch (exc: Exception) {
                    exc.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "failed to export logs: $exc", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
        navController.addOnDestinationChangedListener { controller, destination, args ->
            val s = when(destination.route) {
                NAV_IDENTITY -> true
                NAV_DEBUG -> true
                else -> false
            }
            dest = destination.route
            Log.v(TAG, "navigating to ${destination.route} $s")
            hidefab = s
        }

        AnimatedVisibility(visible = hidefab, enter = scaleIn(), exit = scaleOut()) {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                when(dest) {
                    NAV_IDENTITY -> navController.navigate(NAV_CREATE_IDENTITY)
                    NAV_DEBUG -> launcher.launch(
                        "logs-${SimpleDateFormat("mm-dd-yyyy", Locale.ROOT).format(Date())}.zip"
                    )
                }
            }) {
                when(dest) {
                    NAV_IDENTITY -> Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_identity_add_24),
                        contentDescription = "Add identity"
                    )
                    NAV_DEBUG -> Icon(
                        painter = painterResource(id = R.drawable.baseline_share_24),
                        contentDescription = "Share logs"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext.initDiskLogging()
        model.logObserver.enableObserver()

        setContent {
            val controller = rememberNavController()


            val scope = rememberCoroutineScope()
            ScatterbrainTheme {
                Scaffold(
                    content = { pad ->
                        NavHost(
                            modifier = Modifier
                                .padding(pad)
                                .fillMaxSize(),
                            navController = controller,
                            startDestination = NAV_CHAT
                        ) {
                            composable(NAV_CHAT) {
                                ScopeScatterbrainPermissions(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    onGrant = { scope.launch { tryStart() } },
                                ) {
                                    ChatView(modifier = Modifier.fillMaxSize())
                                }
                            }
                            composable(NAV_POWER) {
                               ScopeScatterbrainPermissions(
                                   modifier = Modifier
                                       .fillMaxSize(),
                                   onGrant = { scope.launch { tryStart() } },
                               ) {
                                    SideEffect {
                                        model.onPermissionsGranted()
                                    }
                                    PowerToggle()
                                }
                            }
                            composable(NAV_IDENTITY) { IdentityManagement() }
                            composable(NAV_DEBUG) { DebugView() }
                            composable(NAV_ABOUT) { About() }
                            dialog(NAV_CREATE_IDENTITY) { CreateIdentityDialog(controller) }
                        }
                    },
                    topBar = { TopBar(controller) },
                    floatingActionButton = { Fab(controller) }

                )
            }
        }
        ignoreBatteryOptimizations()
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
        const val NAV_IDENTITY = "Identities"
        const val NAV_CREATE_IDENTITY = "create_identity"
        const val NAV_POWER = "Control Panel"
        const val NAV_ABOUT = "Legal Info"
        const val NAV_DEBUG = "Debugging Information"
        const val NAV_CHAT = "Public Chat"
    }
}