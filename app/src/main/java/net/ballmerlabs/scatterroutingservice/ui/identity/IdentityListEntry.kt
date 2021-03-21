package net.ballmerlabs.scatterroutingservice.ui.identity

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lelloman.identicon.view.IdenticonView
import net.ballmerlabs.scatterroutingservice.databinding.IdentityListItemBinding

class IdentityListEntry(private val view: View) : RecyclerView.ViewHolder(view) {
    val bind:  IdentityListItemBinding = IdentityListItemBinding.bind(view)
    val identicon: IdenticonView = bind.identicon
    val nameText: TextView = bind.name
    val fingerintText: TextView = bind.fingerprint
    val editButton: Button = bind.editbutton
    val ownedChip: Chip = bind.chip4
    val deleteButton: Button = bind.deletebutton
    val constraintLayout: ConstraintLayout = bind.itemConstraintlayout
}