package net.ballmerlabs.scatterroutingservice.ui.identity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class IdentityImportActivity : AppCompatActivity() {

    private var count by Delegates.notNull<Int>()

    private val viewModel by viewModels<IdentityImportViewModel>()

    @Inject
    lateinit var repository: BinderWrapper

    private suspend fun getIdentities() = withContext(Dispatchers.IO) {
        val identities: List<Identity> = repository.getIdentities().filter { i -> i.isOwned }
    }

    private suspend fun authorizeIdentities(): Int = withContext(Dispatchers.Default) {
        try {
            val callingPackage = callingActivity?.packageName
            if (callingPackage != null) {
                viewModel.selected.forEach { id ->
                    repository.authorizeIdentity(id, callingPackage)
                }
                Activity.RESULT_OK
            } else {
                ERR_CALLING_PACKAGE_INVALID
            }
        } catch (exception: Exception) {
            ERR_AUTH_FAILED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        count = intent.getIntExtra(ScatterbrainApi.EXTRA_NUM_IDENTITIES, 1)
    }

    companion object {
        const val ERR_AUTH_FAILED = 999
        const val ERR_CALLING_PACKAGE_INVALID = 998
    }
}