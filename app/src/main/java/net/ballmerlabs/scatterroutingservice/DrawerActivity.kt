package net.ballmerlabs.scatterroutingservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import javax.inject.Inject


@AndroidEntryPoint
@InternalCoroutinesApi
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

    private val requestPermissionLauncher =
        (this as ComponentActivity).registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                //   binding.appbar.maincontent.grantlocationbanner.dismiss()
                lifecycleScope.launch {
                    if (checkLocationPermission()) {
                        repository.startService()
                        repository.bindService(timeout = 5000000L)

                    }
                }

            } else {
                // binding.appbar.maincontent.grantlocationbanner.setMessage(R.string.failed_location_text)
            }
        }

    private var mAppBarConfiguration: AppBarConfiguration? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeBattery) {
            if (resultCode == RESULT_OK) {
            } else {
                //TODO: chastise user
            }
        }
    }

    private suspend fun requestPermission(permission: String, request: Int, fail: Int): Boolean =
        suspendCancellableCoroutine { c ->
            //TODO
        }

    private suspend fun checkLocationPermission(): Boolean {
        var works = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (perm in arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
            ) {
                val check = requestPermission(
                    perm,
                    R.string.strongly_assert,
                    R.string.failed_strongly_assert
                )
                if (!check) {
                    works = false
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val check = requestPermission(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                R.string.grant_wifi_text,
                R.string.failed_strongly_assert
            )

            if (!check) {
                works = false
            }
        }
        val check = requestPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.string.grant_location_text,
            R.string.failed_location_text
        )
        if (!check) {
            works = false
        }
        if (!checkBatteryOptimization()) {
            works = false
        }
        if (works) {
            model.permissionGranted.value = works
        }
        return works
    }

    @SuppressLint("BatteryLife") //am really sowwy google. Pls fowgive me ;(
    private fun checkBatteryOptimization(): Boolean {
        //TODO
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //TODO theme
        }
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
        uiBroadcastReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
        uiBroadcastReceiver.register()
    }

    companion object {
        const val TAG = "DrawerActivity"
    }
}