package net.ballmerlabs.scatterroutingservice.ui.debug

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel

@Composable
fun DebugView(padding: PaddingValues) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scroll = rememberScrollState()
    val livedata by model.logObserver.observeLogs().observeAsState(listOf())
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(padding)) {
        for (x in livedata) {
            item {
                Text(text = x)
            }
        }
    }

}