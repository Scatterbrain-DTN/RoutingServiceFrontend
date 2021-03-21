package net.ballmerlabs.scatterroutingservice.ui.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.R
import javax.inject.Inject

@AndroidEntryPoint
internal class ViewIdentityFragment @Inject constructor() : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_identity, container, false)
    }
}