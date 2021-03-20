package net.ballmerlabs.scatterbrain.ui.identity

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
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
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentEditIdentityDialogListDialogBinding.inflate(inflater)
        identity = requireArguments().getParcelable(ARG_IDENTITY)!!
        adapter = ArrayAdapter<String>(requireContext(), binding.autocompleteAppSelector.id)

        model.viewModelScope.softCancelLaunch {
            model.getApplicationInfo(identity)
                    .observe(viewLifecycleOwner, { list ->
                        adapter.clear()
                        for (info in list) {
                            adapter.add(info.name)
                        }
                    })
        }
        return binding.root
    }

    companion object {
        fun newInstance(identity: Identity): EditIdentityDialogFragment =
                EditIdentityDialogFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_IDENTITY, identity)
                    }
                }
    }
}