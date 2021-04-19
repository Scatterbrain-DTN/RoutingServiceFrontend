package net.ballmerlabs.scatterroutingservice.ui.identity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.*
import net.ballmerlabs.scatterroutingservice.databinding.FragmentEditIdentityDialogListDialogBinding
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
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
    private lateinit var adapter: AppPackageArrayAdapter<ApplicationInfo>
    private val model: RoutingServiceViewModel by viewModels()

    @Inject lateinit var repository: BinderWrapper
    
    private fun createPermissionChip(info: NamePackage) {
        val chip = Chip(requireContext())
        chip.text = info.name
        chip.isCloseIconVisible = true
        chip.isCheckable = false
        chip.isClickable = false
        chip.isCheckable = false
        binding.flexbox.addView(chip as View, binding.flexbox.childCount - 1)
        chip.setOnCloseIconClickListener {
            lifecycleScope.softCancelLaunch { repository.deauthorizeIdentity(identity, info.info.packageName) }
            binding.flexbox.removeView(chip as View)
        }
    }

    @SuppressLint("QueryPermissionsNeeded") //we declare the queries element
    private suspend fun composeInfoList() : List<NamePackage> {
        val apps = requireContext().packageManager.getInstalledApplications(0)
        val infoList = ArrayList<NamePackage>()
        for (info in apps) {
            yield()
            if (info.name != null) {
                Log.v(TAG, "loading package: ${info.name}")
                infoList.add(
                        NamePackage(
                                requireContext().packageManager.getApplicationLabel(info).toString(),
                                info
                        )
                )
            }
        }
        return infoList
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentEditIdentityDialogListDialogBinding.inflate(inflater)
        identity = requireArguments().getParcelable(ARG_IDENTITY)!!
        binding.editname.text = identity.givenname
        binding.autocompleteAppSelector.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        binding.autocompleteAppSelector.threshold = 1
        binding.autocompleteAppSelector.onItemClickListener = AdapterView.OnItemClickListener()
        { adapterView: AdapterView<*>, _: View, i: Int, _: Long ->
            val info = adapterView.getItemAtPosition(i) as NamePackage?
            if (info != null) {
                lifecycleScope.softCancelLaunch {
                    repository.authorizeIdentity(identity, info.info.packageName)
                }
                createPermissionChip(info)
            } else {
                Log.w(TAG, "attempted to create chip for null string at $i")
            }
        }
        
        lifecycleScope.softCancelLaunch {
            val infoList = withContext(Dispatchers.IO) { composeInfoList() }

            withContext(Dispatchers.Main) {
                adapter = AppPackageArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, infoList)
                adapter.notifyDataSetChanged()
                model.getApplicationInfo(identity)
                        .observe(viewLifecycleOwner, { list ->
                            list.forEach {
                                createPermissionChip(it)
                            }
                            binding.autocompleteAppSelector.setAdapter(adapter)
                        })
            }

        }
        return binding.root
    }

    private inner class AppPackageArrayAdapter<T>(
            context: Context,
            resource: Int,
            private val allVals: List<NamePackage>
    ) : ArrayAdapter<NamePackage>(context, resource, allVals) {
        private var items = allVals

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): NamePackage? {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return items[position].hashCode().toLong()
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(p0: CharSequence?): FilterResults {
                    Log.v(TAG, "filtering $p0")
                    val f = p0?.toString()?.toLowerCase(Locale.ROOT)
                    val results = FilterResults()
                    results.values = if (f == null || f.isEmpty())
                        allVals
                    else
                        allVals.filter {
                            it.name.toLowerCase(Locale.ROOT).contains(f)
                        }

                    return results

                }

                override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                    items = (p1!!.values as List<NamePackage>)
                    notifyDataSetChanged()
                }
            }   
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val textView: TextView
            return if (convertView == null) {
                val inflater = LayoutInflater.from(requireContext())
                textView  = inflater.inflate(R.layout.permission_autocomplete_item, parent, false) as TextView
                textView.text = item!!.name
                textView
            } else {
                convertView
            }
        }
    }

    companion object {
        const val TAG = "EditIdentityDialogFragment"
        fun newInstance(identity: Identity): EditIdentityDialogFragment =
                EditIdentityDialogFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_IDENTITY, identity)
                    }
                }
    }
}