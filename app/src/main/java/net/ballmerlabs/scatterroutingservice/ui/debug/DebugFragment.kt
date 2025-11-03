package net.ballmerlabs.scatterroutingservice.ui.debug

import android.net.Uri
import android.widget.Spinner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel

@Composable
fun DatastoreBackup() {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    var rebuilding by remember { mutableStateOf(false) }
    var dismiss by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        scope.launch(Dispatchers.IO) {
            model.repository.dumpDatastore(uri)
        }
    }

    if (rebuilding && !dismiss) {
        AlertDialog(
            title = { Text("Rebuilding merkle datastore") },
            text = {
                Column {
                    CircularProgressIndicator()
                    Text("This process will continue in the background ")
                }
            },
            onDismissRequest = { dismiss = true },
            confirmButton = {
                Button(onClick = { dismiss = true }) { Text("Continue in background") }
            }
        )
    }


    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(onClick = {
            launcher.launch("output.sqlite")
        }) {
            Text(text = "Export database")
        }

        if (!rebuilding)
            Button(onClick = {
                scope.launch {
                    rebuilding = true
                    dismiss = false
                    model.repository.merkleRebuild()
                    rebuilding = false
                }
            }) {
                Text(text = "Rebuild merkle store")
            }
    }
}


@Composable
fun DebugView() {
    val model: RoutingServiceViewModel = hiltViewModel()
    val livedata by model.logObserver.observeLogs().observeAsState(listOf())
    Column(
        Modifier
            .fillMaxSize()
    ) {
        DatastoreBackup()
        if (livedata.isNotEmpty()) {
            val scroll = rememberLazyListState(livedata.size - 1)
            LazyColumn(
                modifier = Modifier,
                state = scroll,
                reverseLayout = false
            ) {
                for (x in livedata) {
                    item {
                        Card(
                            colors = CardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.contentColorFor(
                                    MaterialTheme.colorScheme.secondaryContainer
                                ),
                                disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                                    MaterialTheme.colorScheme.secondaryContainer
                                )

                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp)
                            ) {
                                Text(text = x.shortScope(), fontWeight = FontWeight.ExtraBold)
                                Text(text = x.text)
                            }
                        }
                    }
                }

            }
        } else {
            Text(text = "no logs")
        }
    }

}