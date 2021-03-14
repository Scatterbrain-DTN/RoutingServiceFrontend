package net.ballmerlabs.scatterbrain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import javax.inject.Inject

@HiltViewModel
class RoutingServiceViewModel @Inject constructor(
        private val repository: ServiceConnectionRepository
) : ViewModel() {
    suspend fun observeMessages(application: String): LiveData<List<ScatterMessage>> {
        return repository.observeMessages(application).asLiveData()
    }

    suspend fun observeIdentities() : LiveData<List<Identity>> {
        return repository.observeIdentities().asLiveData()
    }
}