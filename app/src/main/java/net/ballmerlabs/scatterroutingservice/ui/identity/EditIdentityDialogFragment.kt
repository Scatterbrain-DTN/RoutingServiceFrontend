package net.ballmerlabs.scatterroutingservice.ui.identity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import javax.inject.Inject

// TODO: Customize parameter argument names
const val ARG_IDENTITY = "identity"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    EditIdentityDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
@AndroidEntryPoint
@InternalCoroutinesApi
class EditIdentityDialogFragment : BottomSheetDialogFragment() {

    private lateinit var identity: Identity

    @InternalCoroutinesApi
    private val model: RoutingServiceViewModel by viewModels()

    @Inject
    lateinit var repository: BinderWrapper

    @SuppressLint("QueryPermissionsNeeded") //we declare the queries element
    private fun composeInfoList(): Flow<NamePackage> = flow {
        val intent = Intent(ScatterbrainApi.BROADCAST_EVENT)

        val pm = requireContext().packageManager
        val resolve = pm.queryBroadcastReceivers(intent, 0)
        resolve.forEach { r ->
            val info = pm.getApplicationInfo(r.activityInfo.packageName, 0)
            Log.v(TAG, "loading package: ${info.name}")
            if (info.packageName != BinderWrapper.BIND_PACKAGE) {
                val p = NamePackage(
                    requireContext().packageManager.getApplicationLabel(info).toString(),
                    info,
                    pm
                )
                emit(p)
            }
        }
    }


    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent { }
        }
    }

    companion object {
        const val TAG = "EditIdentity"
        fun newInstance(identity: Identity): EditIdentityDialogFragment =
            EditIdentityDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_IDENTITY, identity)
                }
            }
    }
}