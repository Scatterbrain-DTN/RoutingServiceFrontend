package net.ballmerlabs.scatterroutingservice.ui.identity

import androidx.lifecycle.ViewModel
import net.ballmerlabs.scatterbrainsdk.Identity

class IdentityImportViewModel: ViewModel() {
    val selected: MutableSet<Identity> = mutableSetOf()
}