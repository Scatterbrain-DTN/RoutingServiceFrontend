package net.ballmerlabs.scatterroutingservice

import android.os.RemoteException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.lelloman.identicon.drawable.GithubIdenticonDrawable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterroutingservice.R

@Composable
fun IdentityView(identity: Identity) {
    val identicon = GithubIdenticonDrawable(256, 256, identity.fingerprint.hashCode())
    val painter = BitmapPainter(identicon.toBitmap(width = 256, height = 256).asImageBitmap())
    val model: RoutingServiceViewModel = hiltViewModel()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuState by remember {
        mutableStateOf(false)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(painter = painter, contentDescription = "identicon")
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = identity.name, style = MaterialTheme.typography.headlineMedium
            )
            Box {
                Image(
                    modifier = Modifier.clickable { menuState = true },
                    painter = painterResource(id = R.drawable.ic_baseline_menu_24),
                    contentDescription = "Menu",
                )
                DropdownMenu(expanded = menuState, onDismissRequest = { menuState = false }) {
                    DropdownMenuItem(text = { Text(text = "Delete") }, onClick = {
                        scope.launch {
                            try {
                                model.repository.removeIdentity(identity)
                            } catch (exc: RemoteException) {
                                Toast.makeText(
                                    ctx,
                                    "Failed to delete identity $exc",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun IdentityList() {
    val model: RoutingServiceViewModel = hiltViewModel()
    val identities =  model.repository.observeIdentities().collectAsState(initial = listOf())
    if (identities.value.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No identities yet")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            identities.value.forEach { id ->
                item {
                    IdentityView(id)
                }
            }
        }
    }
}


@Composable
fun IdentityManagement(paddingValues: PaddingValues) {
    Box(modifier = Modifier.padding(paddingValues)) {
        IdentityList()
    }
}