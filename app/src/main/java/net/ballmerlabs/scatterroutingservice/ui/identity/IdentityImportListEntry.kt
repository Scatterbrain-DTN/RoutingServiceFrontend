package net.ballmerlabs.scatterroutingservice.ui.identity

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.lelloman.identicon.view.IdenticonView
import net.ballmerlabs.scatterroutingservice.databinding.IdentityImportItemBinding

class IdentityImportListEntry(view: View): RecyclerView.ViewHolder(view) {
    val bind: IdentityImportItemBinding = IdentityImportItemBinding.bind(view)
    val identicon: IdenticonView = bind.identicon
    val nameText: TextView = bind.name
    val fingerintText: TextView = bind.fingerprint
    val constraintLayout: ConstraintLayout = bind.itemConstraintlayout
}