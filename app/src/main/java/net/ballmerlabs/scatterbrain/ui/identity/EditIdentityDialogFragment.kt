package net.ballmerlabs.scatterbrain.ui.identity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.chip.ChipDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.yield
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrain.RoutingServiceViewModel
import net.ballmerlabs.scatterbrain.ServiceConnectionRepository
import net.ballmerlabs.scatterbrain.databinding.FragmentEditIdentityDialogListDialogBinding
import net.ballmerlabs.scatterbrain.softCancelLaunch
import net.ballmerlabs.scatterbrainsdk.Identity
import java.lang.UnsupportedOperationException
import java.util.*
import java.util.stream.IntStream
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
    private lateinit var pm: PackageManager

    @Inject lateinit var repository: ServiceConnectionRepository
    
    private fun createPermissionChip(str: String, offset: Int) {
        val chip = ChipDrawable.createFromResource(requireContext(), R.xml.permission_chip)
        val editText = binding.autocompleteAppSelector.text
        val span = ImageSpan(chip)
        chip.text = str
        chip.setBounds(0, 0, chip.intrinsicWidth, chip.intrinsicHeight)
        editText.setSpan(span, 0, offset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    
    @SuppressLint("QueryPermissionsNeeded") //we declare the queries element
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        pm = requireActivity().packageManager
        binding = FragmentEditIdentityDialogListDialogBinding.inflate(inflater)
        identity = requireArguments().getParcelable(ARG_IDENTITY)!!
        binding.editname.text = identity.givenname
        binding.autocompleteAppSelector.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        binding.autocompleteAppSelector.threshold = 1
        binding.autocompleteAppSelector.onItemClickListener = AdapterView.OnItemClickListener()
        { adapterView: AdapterView<*>, _: View, i: Int, _: Long ->
            val info = adapterView.getItemAtPosition(i) as ComparableApp?
            if (info != null) {
                lifecycleScope.softCancelLaunch {
                  repository.authorizeIdentity(identity, info.info.packageName)
                }
                createPermissionChip(
                        info.name,
                        binding.autocompleteAppSelector.text.length
                )
            } else {
                Log.w(TAG, "attempted to create chip for null string at $i")
            }
        }
        val apps = requireContext().packageManager.getInstalledApplications(0)
        val infoList = ArrayList<ComparableApp>()
        for (info in apps) {
            if (info.name != null) {
                Log.v(TAG, "loading package: ${info.name}")
                infoList.add(ComparableApp(pm.getApplicationLabel(info).toString(), info))
            }
        }
        adapter = AppPackageArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, infoList)
        binding.autocompleteAppSelector.setAdapter(adapter)
        adapter.notifyDataSetChanged()
        model.getApplicationInfo(identity)
                .observe(viewLifecycleOwner, { list ->
                    var offset = 0;
                    for (info in list) {
                        val str = pm.getApplicationLabel(info).toString() + ", "
                        Log.v(TAG, "restoreing saved permission chip")
                        binding.autocompleteAppSelector.append(str)
                        createPermissionChip(str, offset)
                        offset += str.length
                    }
                    adapter.notifyDataSetChanged()
                })
        return binding.root
    }
    
    private data class ComparableApp(
            public val name: String,
            public val info: ApplicationInfo
            ) {}

    private inner class AppPackageArrayAdapter<T>(
            context: Context,
            resource: Int,
            private val allVals: List<ComparableApp>
    ) : ArrayAdapter<ComparableApp>(context, resource, allVals) {
        private var items = allVals

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): ComparableApp? {
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
                    items = (p1!!.values as List<ComparableApp>)
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