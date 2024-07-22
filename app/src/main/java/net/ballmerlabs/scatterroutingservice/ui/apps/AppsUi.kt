package net.ballmerlabs.scatterroutingservice.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.random.Random

fun sbName(): String {
    val i = Random(Date().time).nextInt().absoluteValue % 1024
    return "sb$i"
}

@Composable
fun AppsView() {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    var name by remember {
        mutableStateOf(sbName())
    }
    Column {
        Text(text = "Name visible to desktop")
        Row {
           TextField(
               modifier = Modifier.weight(1F),
               value = name, 
               onValueChange = { v -> 
                   name = v 
               }
           )
            Button(onClick = {
                scope.launch {
                    model.repository.startDesktopApi(name)
                }
            }) {
                Text(text = "Start")
            }
        }
    }
}