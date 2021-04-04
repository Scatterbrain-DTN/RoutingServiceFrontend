package net.ballmerlabs.scatterroutingservice

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.databinding.FragmentAboutBinding
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment @Inject constructor() : Fragment() {

    lateinit var bind: FragmentAboutBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        bind = FragmentAboutBinding.inflate(inflater)
        bind.tosText.movementMethod = LinkMovementMethod.getInstance()
        return bind.root
    }
}