package net.ballmerlabs.scatterbrain

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import net.ballmerlabs.scatterbrain.databinding.ActivityDrawerBinding
import net.ballmerlabs.scatterbrain.databinding.AppBarMainBinding
import net.ballmerlabs.scatterbrain.databinding.ContentMainBinding

class DrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawerBinding
    private lateinit var contentBinding: ContentMainBinding
    private lateinit var appBarMainBinding: AppBarMainBinding

    private var mAppBarConfiguration: AppBarConfiguration? = null
    private val requestPermissionLauncher = (this as ComponentActivity).registerForActivityResult (RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            contentBinding.grantlocationbanner.dismiss()
        } else {
            contentBinding.grantlocationbanner.setMessage(R.string.failed_location_text)
        }
    }

    private fun requestForegroundLocation() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        contentBinding = ContentMainBinding.inflate(layoutInflater)
        appBarMainBinding = AppBarMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_drawer)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            contentBinding.grantlocationbanner.show()
        }
        contentBinding.grantlocationbanner.setRightButtonListener {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        contentBinding.grantlocationbanner.setLeftButtonListener {
            contentBinding.grantlocationbanner.setMessage(R.string.failed_location_text)
        }
        val fabParams = fab.layoutParams as CoordinatorLayout.LayoutParams
        setSupportActionBar(appBarMainBinding.toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _: NavController?, destination: NavDestination, _: Bundle? ->
            if (destination.id == R.id.navigation_identity) {
                fabParams.anchorId = R.id.appbar_layout
            } else {
                fabParams.anchorId = View.NO_ID
            }
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
}