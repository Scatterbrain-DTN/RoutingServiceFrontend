package net.ballmerlabs.scatterroutingservice.ui.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [IdentityHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
@InternalCoroutinesApi
class IdentityHomeFragment : Fragment() {
    @InternalCoroutinesApi
    val model: RoutingServiceViewModel by viewModels()
    @Inject lateinit var repository: BinderWrapper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {  }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment IdentityHomeFragment.
         */
        fun newInstance(): IdentityHomeFragment {
            val fragment = IdentityHomeFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}