package net.ballmerlabs.scatterroutingservice.ui.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.lelloman.identicon.drawable.GithubIdenticonDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterroutingservice.BottomSheetContent
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.ui.theme.ScatterbrainTheme
import javax.inject.Inject
import kotlin.properties.Delegates


@AndroidEntryPoint
class IdentityImportActivity : AppCompatActivity() {


    private var count by Delegates.notNull<Int>()

    private val viewModel by viewModels<IdentityImportViewModel>()

    @Inject
    lateinit var repository: BinderWrapper

    private suspend fun getIdentities(): List<Identity> = withContext(Dispatchers.IO) {
        val identities: List<Identity> = repository.getIdentities().filter { i -> i.isOwned }
        Log.v("debug", "got identities ${identities.size}")
        identities
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SelectableIdentityView(identity: Identity) {
        val identicon = GithubIdenticonDrawable(256, 256, identity.fingerprint.hashCode())
        val painter = BitmapPainter(identicon.toBitmap(width = 256, height = 256).asImageBitmap())
        val sheetState = rememberModalBottomSheetState()
        var showBottomSheet by remember { mutableStateOf(false) }
        var checkedState by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    painter = painter,
                    contentDescription = "identicon"
                )
                Text(
                    modifier = Modifier.padding(end = 10.dp),
                    text = identity.name, style = MaterialTheme.typography.headlineMedium
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Checkbox(checked = checkedState, onCheckedChange = { checked ->
                        checkedState = checked
                        if (checked)
                            viewModel.selected.add(identity)
                        else
                            viewModel.selected.remove(identity)
                    })
                }

            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetContent(modifier = Modifier, identity)
            }
        }
    }

    @Composable
    fun SelectableIdentityList(modifier: Modifier = Modifier) {
        val model: RoutingServiceViewModel = hiltViewModel()
        val identities by model.identities.observeAsState()
        if (identities!!.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No identities yet")
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                identities?.filter { v -> v.isOwned }?.forEach { id ->
                    item {
                        SelectableIdentityView(id)
                    }
                }

                identities?.filter { v -> !v.isOwned }?.forEach { id ->
                    item {
                        SelectableIdentityView(id)
                    }
                }
            }
        }
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        count = intent.getIntExtra(ScatterbrainApi.EXTRA_NUM_IDENTITIES, 1)
        setContent {
            val model: RoutingServiceViewModel = hiltViewModel()
            LaunchedEffect(key1 = model) {
                val ids = getIdentities()
                model.identities.postValue(ids)
            }
            ScatterbrainTheme {
                Scaffold(
                    content = { pad ->
                        SelectableIdentityList(modifier = Modifier.padding(pad))
                    },
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text(text = getString(R.string.import_identities)) },
                                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                lifecycleScope.launch {
                                    val res = authorizeIdentities()
                                    val resultList = arrayListOf<Identity>()
                                    resultList.addAll(viewModel.selected)
                                    val intent =
                                        Intent(ScatterbrainApi.IMPORT_IDENTITY_ACTION).apply {
                                            putParcelableArrayListExtra(
                                                ScatterbrainApi.EXTRA_IDENTITY_RESULT,
                                                resultList
                                            )
                                        }
                                    setResult(res, intent)
                                    finish()
                                }

                            }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_identity_add_24),
                                contentDescription = "Import identity"
                            )
                        }
                    }

                )
            }
        }
    }

    /*
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
     */


    companion object {
        const val ERR_AUTH_FAILED = 999
        const val ERR_CALLING_PACKAGE_INVALID = 998
    }
}