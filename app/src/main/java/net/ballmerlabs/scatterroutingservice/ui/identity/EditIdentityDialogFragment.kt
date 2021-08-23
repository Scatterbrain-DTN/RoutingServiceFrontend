package net.ballmerlabs.scatterroutingservice.ui.identity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentEditIdentityDialogListDialogBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

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
class EditIdentityDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditIdentityDialogListDialogBinding
    private lateinit var identity: Identity
    private lateinit var adapter: AppListAdapter
    @InternalCoroutinesApi
    private val model: RoutingServiceViewModel by viewModels()

    @Inject lateinit var repository: BinderWrapper

    @SuppressLint("QueryPermissionsNeeded") //we declare the queries element
    private fun composeInfoList() : Flow<NamePackage> = flow {
        val intent = Intent(ScatterbrainApi.BROADCAST_EVENT)

        val pm = requireContext().packageManager
        val resolve = pm.queryBroadcastReceivers(intent, 0)
        resolve.forEach { r ->
            val info = pm.getApplicationInfo(r.activityInfo.packageName, 0)
            Log.v(TAG, "loading package: ${info.name}")
            val p = NamePackage(
                    requireContext().packageManager.getApplicationLabel(info).toString(),
                    info,
                    pm
            )
            emit(p)
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    @InternalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        binding = FragmentEditIdentityDialogListDialogBinding.inflate(inflater)
        identity = requireArguments().getParcelable(ARG_IDENTITY)!!
        WindowCompat.setDecorFitsSystemWindows(dialog!!.window!!, false)
        binding.editname.text = identity.name
        adapter = AppListAdapter(requireContext())
        lifecycleScope.softCancelLaunch {
            withContext(Dispatchers.IO) {
                val infoList = composeInfoList().fold(ArrayList<NamePackage>()) { accumulator, value ->
                    value.loadIcon()
                    accumulator.add(value)
                    accumulator
                }

                withContext(Dispatchers.Main) {
                    adapter.items.addAll(infoList)
                    binding.appListRecyclerview.adapter = adapter
                }

                Log.e(TAG, "setting adapter ${infoList.size}")
                withContext(Dispatchers.Main) {
                    model.getPackages()
                            .observe(viewLifecycleOwner, { list ->
                                Log.e(TAG, "restored identity list ${list.size}")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    list.forEach { i -> i.loadIcon() }
                                    withContext(Dispatchers.Main) {
                                        adapter.items.addAll(list)
                                    }
                                }
                            })
                }
            }
        }
        return binding.root
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