package net.ballmerlabs.scatterroutingservice.ui.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentIdentityHomeBinding
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.uscatterbrain.db.entities.ApiIdentity
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [IdentityHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class IdentityHomeFragment @Inject constructor() : Fragment() {
    lateinit var bind: FragmentIdentityHomeBinding
    lateinit var adapter: IdentityListAdapter
    val model: RoutingServiceViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun addTestData() {
        adapter.setItems(listOf(ApiIdentity
                .newBuilder()
                .setName("fmef")
                .setSig("fmef".encodeToByteArray())
                .addKeys(mapOf(Pair(ScatterbrainApi.PROTOBUF_PRIVKEY_KEY, "fmef".encodeToByteArray())))
                .build()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        bind = FragmentIdentityHomeBinding.inflate(inflater)
        // Inflate the layout for this fragment
        adapter = IdentityListAdapter(requireActivity().supportFragmentManager)
        bind.recyclerView.adapter = adapter
        bind.recyclerView.layoutManager = LinearLayoutManager(context)
        model.observeIdentities()
                .observe(viewLifecycleOwner) { newList ->
                    adapter.setItems(newList)
                }
        return bind.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment IdentityHomeFragment.
         */
        fun newInstance(): IdentityHomeFragment {
            val fragment = IdentityHomeFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}