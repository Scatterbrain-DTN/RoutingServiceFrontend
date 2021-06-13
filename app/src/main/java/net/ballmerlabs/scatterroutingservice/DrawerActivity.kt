package net.ballmerlabs.scatterroutingservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.scatterroutingservice.databinding.ActivityDrawerBinding
import javax.inject.Inject


@AndroidEntryPoint
class DrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawerBinding

    @Inject lateinit var repository: BinderWrapper
    
    @Inject lateinit var broadcastReceiver: ScatterbrainBroadcastReceiver

    @Inject lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver

    @InternalCoroutinesApi
    val model: RoutingServiceViewModel by viewModels()

    private val requestCodeBattery = 1


    private var mAppBarConfiguration: AppBarConfiguration? = null
    private val requestPermissionLauncher = (this as ComponentActivity).registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            binding.appbar.maincontent.grantlocationbanner.dismiss()
            checkBatteryOptimization()
        } else {
            binding.appbar.maincontent.grantlocationbanner.setMessage(R.string.failed_location_text)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeBattery) {
            if (resultCode == RESULT_OK) {
                binding.appbar.maincontent.batteryOptimizationBanner.dismiss()
            } else {
                binding.appbar.maincontent.batteryOptimizationBanner.dismiss()
                //TODO: chastise user
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        binding.appbar.maincontent.grantlocationbanner.setRightButtonListener {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        binding.appbar.maincontent.grantlocationbanner.setLeftButtonListener {
            binding.appbar.maincontent.grantlocationbanner.setMessage(R.string.failed_location_text)
        }

        return if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            binding.appbar.maincontent.grantlocationbanner.show()
            false
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife") //am really sowwy google. Pls fowgive me ;(
    private fun checkBatteryOptimization() : Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            binding.appbar.maincontent.batteryOptimizationBanner.setRightButtonListener {
                val intent = Intent()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, requestCodeBattery)
            }

            binding.appbar.maincontent.batteryOptimizationBanner.setLeftButtonListener {
                binding.appbar.maincontent.batteryOptimizationBanner.dismiss()
            }
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                binding.appbar.maincontent.batteryOptimizationBanner.show()
                false
            } else {
                true
            }
        } else {
            true //older devices don't have doze mode
        }
    }

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fab = binding.appbar.fab

        binding.appbar.maincontent.enableBluetoothBanner.setLeftButtonListener {
            binding.appbar.maincontent.enableBluetoothBanner.dismiss()
        }

        model.observeAdapterState().observe(this, { state ->
            if (state != BluetoothState.STATE_ON) {
                binding.appbar.maincontent.enableBluetoothBanner.show()
            } else {
                binding.appbar.maincontent.enableBluetoothBanner.dismiss()
            }
        })

        if (checkLocationPermission()) {
            checkBatteryOptimization()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window!!.setDecorFitsSystemWindows(true)
        }
        val versionView = binding.navView.getHeaderView(0).findViewById<TextView>(R.id.textView) //Viewbinding doesn't work with the nav header
        versionView.append(BuildConfig.VERSION_NAME)
        val fabParams = fab.layoutParams as CoordinatorLayout.LayoutParams
        setSupportActionBar(binding.appbar.toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _: NavController?, destination: NavDestination, _: Bundle? ->
            if (destination.id == R.id.navigation_identity) {
                fabParams.anchorId = R.id.appbar_layout
                fab.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                            .setView(R.layout.create_identity_dialog_view)
                            .setTitle(R.string.create_dialog_title)
                            .setNeutralButton(R.string.create_cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.create_create) { dialog, _ ->
                                val dialogLayout = dialog as AlertDialog
                                val editText = dialogLayout.findViewById<TextInputEditText>(R.id.identity_name_text)
                                Log.v(TAG, "got text val ${editText!!.text}")
                                lifecycleScope.softCancelLaunch {
                                    val respose = repository.generateIdentity(editText.text.toString())

                                    dialog.setTitle("Failed to generate identity: $respose")

                                }
                            }
                            .show()
                }
                fab.show()
            } else {
                fabParams.anchorId = View.NO_ID
                fab.hide()
            }
            fab.layoutParams = fabParams
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = AppBarConfiguration.Builder(navController.graph)
                .setDrawerLayout(drawer)
                .build()
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration!!)
        NavigationUI.setupWithNavController(navigationView, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return (NavigationUI.navigateUp(navController, mAppBarConfiguration!!)
                || super.onSupportNavigateUp())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return (NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item))
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
        bluetoothBroadcastReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
        bluetoothBroadcastReceiver.register()
    }
    
    companion object {
        val TAG = "DrawerActivity"
    }
}