package net.ballmerlabs.scatterroutingservice.ui.identity

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.databinding.FragmentIdentityHomeBinding
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import net.ballmerlabs.uscatterbrain.db.entities.ApiIdentity
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [IdentityHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class IdentityHomeFragment : Fragment() {
    lateinit var bind: FragmentIdentityHomeBinding
    private lateinit var adapter: IdentityListAdapter
    @InternalCoroutinesApi
    val model: RoutingServiceViewModel by viewModels()
    @Inject lateinit var repository: BinderWrapper
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

    private suspend fun checkConnected() {
        if (repository.isConnected()) {
            if (adapter.itemCount == 0) {
                bind.serviceNotConnected.text = requireContext().getString(R.string.noid)
                bind.serviceNotConnected.visibility = View.VISIBLE
            } else {
                bind.serviceNotConnected.visibility = View.GONE
            }

        } else {
            bind.serviceNotConnected.text = requireContext().getString(R.string.service_not_connected)
            bind.serviceNotConnected.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { checkConnected() }
    }

    @InternalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        bind = FragmentIdentityHomeBinding.inflate(inflater)
        // Inflate the layout for this fragment
        adapter = IdentityListAdapter(requireActivity().supportFragmentManager)
        bind.recyclerView.adapter = adapter
        bind.recyclerView.layoutManager = LinearLayoutManager(context)
        lifecycleScope.launch {
            if (repository.isConnected()) {
                model.observeIdentities()
                        .observe(viewLifecycleOwner) { newList ->
                            adapter.setItems(newList)
                            lifecycleScope.launch {  checkConnected() }
                        }
            }
            checkConnected()
        }
        return bind.root
    }

    @InternalCoroutinesApi
    fun removeIdentity(identity: Identity) {
        lifecycleScope.softCancelLaunch { 
            if (repository.removeIdentity(identity)) {
                model.refreshIdentities()
            }
        }
    }

    private inner class IdentityListAdapter(private val fragmentManager: FragmentManager) : RecyclerView.Adapter<IdentityListEntry>() {
        private val viewlist: MutableList<Identity> = mutableListOf()
        private lateinit var ctx: Context
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityListEntry {
            ctx = parent.context
            val view = LayoutInflater.from(parent.context).inflate(R.layout.identity_list_item, parent, false)
            return IdentityListEntry(view)
        }

        override fun getItemCount(): Int {
            return viewlist.size
        }

        fun setItems(items: List<Identity>) {
            viewlist.clear()
            viewlist.addAll(items)
            notifyDataSetChanged()
        }

        @InternalCoroutinesApi
        override fun onBindViewHolder(holder: IdentityListEntry, position: Int) {
            val id = viewlist[position]
            holder.fingerintText.text = id.fingerprint.toString()
            holder.nameText.text = id.name
            holder.identicon.hash = id.fingerprint.hashCode()
            holder.deleteButton.setOnClickListener { removeIdentity(id) }
            val deleteparams = holder.deleteButton.layoutParams as ConstraintLayout.LayoutParams
            if (id.hasPrivateKey) {
                holder.editButton.visibility = View.VISIBLE
                holder.editButton.setOnClickListener {
                    EditIdentityDialogFragment.newInstance(id).show(fragmentManager, "edit identity")
                }
                holder.ownedChip.visibility = View.VISIBLE
                deleteparams.marginStart = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24F, ctx.resources.displayMetrics).toInt()
                deleteparams.startToEnd = holder.editButton.id
                deleteparams.startToStart = View.NO_ID
            } else {
                holder.editButton.visibility = View.GONE
                deleteparams.startToEnd = View.NO_ID
                deleteparams.marginStart = 0
                deleteparams.startToStart = ConstraintSet.PARENT_ID
                holder.ownedChip.visibility = View.INVISIBLE
            }
        }

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