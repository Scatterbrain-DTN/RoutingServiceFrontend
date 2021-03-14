package net.ballmerlabs.scatterbrain.ui.identity

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lelloman.identicon.view.IdenticonView
import net.ballmerlabs.scatterbrain.databinding.IdentityListItemBinding

class IdentityListEntry(private val view: View) : RecyclerView.ViewHolder(view) {
    val bind:  IdentityListItemBinding = IdentityListItemBinding.bind(view)
    val identicon: IdenticonView = bind.identicon
    val nameText: TextView = bind.name
    val fingerintText: TextView = bind.fingerprint



}