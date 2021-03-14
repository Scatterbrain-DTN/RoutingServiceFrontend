package net.ballmerlabs.scatterbrain

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ballmerlabs.scatterbrainsdk.Identity
import javax.inject.Inject

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        val repository: ServiceConnectionRepository
) : ViewModel() {
}