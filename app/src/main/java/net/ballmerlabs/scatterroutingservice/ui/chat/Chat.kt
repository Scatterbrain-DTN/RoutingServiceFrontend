package net.ballmerlabs.scatterroutingservice.ui.chat

import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import java.util.Date
import java.util.UUID

const val DEFAULT_APP = "defacto"

data class SimpleMessage(
    val text: String,
    val date: Date
)

@ExperimentalCoroutinesApi
@Composable
fun ChatView(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val message by model.repository.observeMessages(DEFAULT_APP)
        .map { l ->
            l.map { v ->
                val message = v.body?.decodeToString()
                val uuidlen = UUID.randomUUID().toString().length
                if (message != null && message.length > uuidlen +1)
                    SimpleMessage( text = message.removeRange(message.length - uuidlen -1, message.length), date = v.receiveDate)
                else
                    SimpleMessage(text = message ?: "null", date = v.receiveDate)
            }
        }
        .observeAsState(initial = listOf())
    Log.v("debug", "recompose ${message.size}")

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = rememberLazyListState()
    var chatText by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        state.scrollToItem(message.size)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom
    ) {

        LazyColumn(
            state = state,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(1f)
        ) {
            for (m in message) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            ,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val df = DateFormat.getDateFormat(context)
                        Text(
                            modifier = Modifier.weight(1f),
                            text = m.text,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            text = df.format(m.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                value = chatText,
                placeholder = {
                    Text(text = "Message the entire network")
                },
                onValueChange = { v ->
                    chatText = v
                })
            Button(
                onClick = {
                coroutineScope.softCancelLaunch {
                    model.repository.sendMessage(
                        ScatterMessage.Builder.newInstance(
                            context,
                            "$chatText\n${UUID.randomUUID()}".encodeToByteArray()
                        )
                            .setApplication(DEFAULT_APP)
                            .build()
                    )
                    //tate.scrollToItem(message.size+1)
                    chatText = ""
                }
            }) {
                Text(text = "Send")
            }
        }
    }
}