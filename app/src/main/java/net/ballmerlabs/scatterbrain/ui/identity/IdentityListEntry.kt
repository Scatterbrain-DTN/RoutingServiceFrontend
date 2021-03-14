package net.ballmerlabs.scatterbrain.ui.identity

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.scatterbrain.databinding.IdentityListItemBinding

class IdentityListEntry(private val view: View) : RecyclerView.ViewHolder(view) {
    val bind:  IdentityListItemBinding = IdentityListItemBinding.bind(view)
    val identicon: ImageView = bind.identicon
    val nameText: TextView = bind.name
    val fingerintText: TextView = bind.fingerprint



}