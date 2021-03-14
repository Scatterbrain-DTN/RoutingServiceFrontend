package net.ballmerlabs.scatterbrain.ui.identity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrainsdk.Identity

class IdentityListAdapter : RecyclerView.Adapter<IdentityListEntry>() {
    private val viewlist: MutableList<Identity> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityListEntry {
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

    override fun onBindViewHolder(holder: IdentityListEntry, position: Int) {
        val id = viewlist[position]
        holder.fingerintText.text = id.fingerprint
        holder.nameText.text = id.givenname
        holder.identicon.hash = id.fingerprint.hashCode()
    }

}