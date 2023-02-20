package net.ballmerlabs.scatterroutingservice.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel

@Composable
fun DebugView(padding: PaddingValues) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val livedata by model.logObserver.observeLogs().observeAsState(listOf())
    if (livedata.isNotEmpty()) {
        val scroll = rememberLazyListState(livedata.size-1)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = scroll,
            reverseLayout = false
        ) {
            for (x in livedata) {
                item {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                            Text(text = x.shortScope(), fontWeight = FontWeight.ExtraBold)
                            Text(text = x.text)
                        }
                    }
                }
            }

        }
    }

}