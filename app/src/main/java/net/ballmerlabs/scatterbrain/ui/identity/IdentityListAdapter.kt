package net.ballmerlabs.scatterbrain.ui.identity

import android.app.Activity
import android.content.Context
import android.opengl.Visibility
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginStart
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.scatterbrain.R
import net.ballmerlabs.scatterbrainsdk.Identity

class IdentityListAdapter(private val fragmentManager: FragmentManager) : RecyclerView.Adapter<IdentityListEntry>() {
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

    override fun onBindViewHolder(holder: IdentityListEntry, position: Int) {
        val id = viewlist[position]
        holder.fingerintText.text = id.fingerprint
        holder.nameText.text = id.givenname
        holder.identicon.hash = id.fingerprint.hashCode()
        val deleteparams = holder.deleteButton.layoutParams as ConstraintLayout.LayoutParams
        if (id.hasPrivateKey()) {
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