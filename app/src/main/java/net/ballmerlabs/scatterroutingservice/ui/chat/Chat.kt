package net.ballmerlabs.scatterroutingservice.ui.chat

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.scatterroutingservice.db.LocalChat
import net.ballmerlabs.scatterroutingservice.softCancelLaunch
import java.util.Date
import java.util.UUID

const val DEFAULT_APP = "defacto"

data class SimpleMessage(
    val text: String,
    val date: Date,
    val owned: Boolean,
    val invalid: Boolean = false
)
val uuidlen = UUID.randomUUID().toString().length


@Composable
fun ChatBubble(message: SimpleMessage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = if (message.owned)
            modifier.padding(start = 16.dp, end = 0.dp)
        else
            modifier.padding(start = 0.dp, end = 16.dp)
    ) {
        Row(
            modifier = if (message.invalid)
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            else if (message.owned)
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            else
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val df = DateFormat.getDateFormat(context)
            Text(
                modifier = Modifier.weight(1f),
                text = message.text,
                color = if (message.owned)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSecondary
            )
            Text(
                text = df.format(message.date),
                style = MaterialTheme.typography.bodySmall,
                color = if (message.owned)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@ExperimentalCoroutinesApi
@Composable
fun ChatView(modifier: Modifier = Modifier) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    val message by model.repository.observeMessages(DEFAULT_APP, 256)
        .switchMap { l ->
            liveData {
                scope.launch(Dispatchers.IO) {
                    val local = model.datastore.localChatDao()
                        .getByUuid(l.mapNotNull { v ->
                            val message = v.body?.decodeToString()

                            if (message != null && message.length > uuidlen + 1)
                                UUID.fromString(message.slice(message.length - uuidlen..<message.length))
                            else
                                null
                        })
                        .associateBy { v -> v.uuid }
                    emit(l.map { v ->
                        try {
                            val message = v.body?.decodeToString()
                            if (message != null && message.length > uuidlen + 1) {
                                val uuid =
                                    UUID.fromString(message.slice(message.length - uuidlen..<message.length))
                                SimpleMessage(
                                    text = message.removeRange(
                                        message.length - uuidlen - 1,
                                        message.length
                                    ), date = v.receiveDate,
                                    owned = local[uuid]?.owned ?: false
                                )
                            } else {
                                SimpleMessage(
                                    text = message ?: "null",
                                    date = v.receiveDate,
                                    owned = false
                                )
                            }
                        } catch (exc: Exception) {
                            SimpleMessage(
                                text = exc.message?:"Invalid",
                                date = Date(),
                                owned = false,
                                invalid = true
                            )
                        }
                    })
                }
                awaitCancellation()
            }
        }
        .observeAsState(initial = listOf())

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var chatText by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        state.scrollToItem(0)
    }
    Column(
        modifier = modifier.imePadding(),
        verticalArrangement = Arrangement.Bottom
    ) {

        LazyColumn(
            state = state,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(1f)
                .imePadding()

        ) {
            for (m in message) {
                item {
                    ChatBubble(message = m)
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
                    Text(
                        text = "Message the entire network",
                    )
                },
                onValueChange = { v ->
                    chatText = v
                })
            Button(
                onClick = {
                    coroutineScope.softCancelLaunch {
                        val uuid = UUID.randomUUID()
                        model.datastore.localChatDao().insert(
                            LocalChat(
                                uuid = uuid,
                                date = Date().time,
                                owned = true
                            )
                        )
                        model.repository.sendMessage(
                            ScatterMessage.Builder.newInstance(
                                context,
                                "$chatText\n${uuid}".encodeToByteArray()
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