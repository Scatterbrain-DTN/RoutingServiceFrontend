package net.ballmerlabs.scatterbrain.ui.identity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.google.android.material.chip.ChipDrawable
import dagger.Binds
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrain.RoutingServiceViewModel
import net.ballmerlabs.scatterbrain.databinding.FragmentEditIdentityDialogListDialogBinding
import net.ballmerlabs.scatterbrain.softCancelLaunch
import net.ballmerlabs.scatterbrainsdk.Identity
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
class EditIdentityDialogFragment : BottomSheetDialogFragment() {

    lateinit var binding: FragmentEditIdentityDialogListDialogBinding
    lateinit var identity: Identity
    lateinit var adapter: ArrayAdapter<String>
    private val model: RoutingServiceViewModel by viewModels()
    
    private fun createPermissionChip(name: String) {
        val chip = ChipDrawable.createFromResource(requireContext(), R.xml.permission_chip)
        val editText = binding.autocompleteAppSelector.text
        val span = ImageSpan(chip)
        chip.text = name
        chip.setBounds(0, 0, chip.intrinsicWidth, chip.intrinsicHeight)
        editText.setSpan(span, 0, editText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    
    @SuppressLint("QueryPermissionsNeeded") //we declare the queries element
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentEditIdentityDialogListDialogBinding.inflate(inflater)
        identity = requireArguments().getParcelable(ARG_IDENTITY)!!
        adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.editname.text = identity.givenname
        binding.autocompleteAppSelector.setAdapter(adapter)
        binding.autocompleteAppSelector.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        binding.autocompleteAppSelector.threshold = 1
        binding.autocompleteAppSelector.onItemClickListener = AdapterView.OnItemClickListener()
        { _: AdapterView<*>, _: View, i: Int, _: Long ->
            val str = adapter.getItem(i)
            if (str != null) {
                createPermissionChip(str)
            } else {
                Log.w(TAG, "attempted to create chip for null string at $i")
            }
        }
        model.viewModelScope.softCancelLaunch {
            val apps = requireContext().packageManager.getInstalledApplications(0)
            for (info in apps) {
                if (info.name != null) {
                    Log.v(TAG, "loading package: ${info.name}")
                    adapter.add(info.name)
                }
            }
            adapter.notifyDataSetChanged()
            model.getApplicationInfo(identity)
                    .observe(viewLifecycleOwner, { list ->
                        for (info in list) {
                            adapter.add(info.name)
                        }
                    })
        }
        return binding.root
    }

    private inner class AppPackageArrayAdapter(context: Context, resource: Int) : ArrayAdapter<String>(context, resource) {

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