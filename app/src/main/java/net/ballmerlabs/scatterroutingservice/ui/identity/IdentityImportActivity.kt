

package net.ballmerlabs.scatterroutingservice.ui.identity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.databinding.ActivityIdentityImportBinding
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class IdentityImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIdentityImportBinding

    private var count by Delegates.notNull<Int>()

    private val viewModel by viewModels<IdentityImportViewModel>()

    @Inject lateinit var repository: BinderWrapper

    private suspend fun getIdentities() = withContext(Dispatchers.IO) {
        val identities: List<Identity> = repository.getIdentities().filter { i -> i.isOwned }
        Log.v("debug", "got identities ${identities.size}")
        val adapter = ImportListAdapter(identities)
        withContext(Dispatchers.Main) { binding.importContent.importRecyclerview?.adapter = adapter }
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
        binding = ActivityIdentityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        count = intent.getIntExtra(ScatterbrainApi.EXTRA_NUM_IDENTITIES, 1)
        setSupportActionBar(findViewById(R.id.toolbar))
        binding.confirmFab.setOnClickListener { view ->
            lifecycleScope.launch{
                val res = authorizeIdentities()
                val resultList = arrayListOf<Identity>()
                resultList.addAll(viewModel.selected)
                val intent = Intent(ScatterbrainApi.IMPORT_IDENTITY_ACTION).apply {
                    putParcelableArrayListExtra(ScatterbrainApi.EXTRA_IDENTITY_RESULT, resultList)
                }
                setResult(res, intent)
                finish()
            }
        }
        lifecycleScope.launch { getIdentities() }
    }


    private inner class ImportListAdapter(private val identities: List<Identity>): RecyclerView.Adapter<IdentityImportListEntry>() {
        private lateinit var ctx: Context
        private val unSelectedCheckBoxes = mutableSetOf<CheckBox>()
        private fun lock(lock: Boolean) {
            unSelectedCheckBoxes.forEach { box ->
                box.isEnabled = !lock
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityImportListEntry {
            ctx = parent.context
            val view = LayoutInflater.from(parent.context).inflate(R.layout.identity_import_item, parent, false)
            return IdentityImportListEntry(view)
        }

        override fun onBindViewHolder(holder: IdentityImportListEntry, position: Int) {
            val id = identities[position]
            holder.bind.checkbox.setOnCheckedChangeListener { view, check ->
                if (check) {
                    viewModel.selected.add(id)
                    unSelectedCheckBoxes.remove(holder.bind.checkbox)
                } else {
                    viewModel.selected.remove(id)
                    unSelectedCheckBoxes.add(holder.bind.checkbox)
                }

                if (viewModel.selected.size >= count) {
                    lock(true)
                } else {
                    lock(false)
                }
            }
            if (!holder.bind.checkbox.isChecked) {
                unSelectedCheckBoxes.add(holder.bind.checkbox)
            }
            holder.fingerintText.text = id.fingerprint.toString()
            holder.nameText.text = id.name
            holder.identicon.hash = id.fingerprint.hashCode()
        }

        override fun getItemCount(): Int {
            return identities.size
        }

    }


    companion object {
        const val ERR_AUTH_FAILED = 999
        const val ERR_CALLING_PACKAGE_INVALID = 998
    }
}