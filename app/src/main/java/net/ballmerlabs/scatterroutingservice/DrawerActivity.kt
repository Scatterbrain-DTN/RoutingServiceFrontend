package net.ballmerlabs.scatterroutingservice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.databinding.ActivityDrawerBinding
import javax.inject.Inject

@AndroidEntryPoint
class DrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawerBinding

    @Inject lateinit var repository: ServiceConnectionRepository
    
    @Inject lateinit var broadcastReceiver: ScatterbrainBroadcastReceiver

    val model: RoutingServiceViewModel by viewModels()


    private var mAppBarConfiguration: AppBarConfiguration? = null
    private val requestPermissionLauncher = (this as ComponentActivity).registerForActivityResult (RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            binding.appbar.maincontent.grantlocationbanner.dismiss()
        } else {
            binding.appbar.maincontent.grantlocationbanner.setMessage(R.string.failed_location_text)
        }
    }

    private fun requestForegroundLocation() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fab = binding.appbar.fab
        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            binding.appbar.maincontent.grantlocationbanner.show()
        }
        binding.appbar.maincontent.grantlocationbanner.setRightButtonListener {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        binding.appbar.maincontent.grantlocationbanner.setLeftButtonListener {
            binding.appbar.maincontent.grantlocationbanner.setMessage(R.string.failed_location_text)
        }
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
                                    
                                    if (respose == null) {
                                        dialog.dismiss()
                                    } else {
                                        dialog.setTitle("Failed to generate identity: $respose")
                                    }

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
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
    }
    
    companion object {
        val TAG = "DrawerActivity"
    }
}