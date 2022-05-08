package net.ballmerlabs.scatterroutingservice.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

class Utils {
    companion object {
        suspend fun checkPermission(permission: String, context: Context): Boolean = suspendCancellableCoroutine { c ->
            if (ContextCompat.checkSelfPermission(
                            context,
                            permission
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                c.resumeWith(Result.success(true))
            } else {

                c.resumeWith(Result.success(false))
            }

        }

        suspend fun checkPermission(context: Context): Optional<String> {
            var res: Optional<String> = Optional.empty()
            if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)) {
                res = Optional.of(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    for (perm in arrayOf(
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN)
                    ) {
                        if (!checkPermission(perm, context)) {
                            res = Optional.of(perm)
                            break
                        }
                    }
                }
            }
            return res
        }

    }
}