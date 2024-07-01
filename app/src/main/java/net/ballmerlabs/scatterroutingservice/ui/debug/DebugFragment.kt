package net.ballmerlabs.scatterroutingservice.ui.debug

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        scope.launch(Dispatchers.IO) {
            model.repository.dumpDatastore(uri)
        }
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
                                    MaterialTheme.colorScheme.secondaryContainer),
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