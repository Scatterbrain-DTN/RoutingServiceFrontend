package net.ballmerlabs.scatterroutingservice.ui.power

import android.os.RemoteException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.RouterState
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.RoutingServiceViewModel
import net.ballmerlabs.uscatterbrain.setActive
import net.ballmerlabs.uscatterbrain.setActiveBlocking
import net.ballmerlabs.uscatterbrain.setPassive
import net.ballmerlabs.uscatterbrain.setPassiveBlocking

@Composable
fun PowerFragment(paddingValues: PaddingValues) {
    Button(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxWidth()
            .fillMaxHeight(),
        onClick = { /*TODO*/ }
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            painter = painterResource(id = R.drawable.ic_baseline_router_disabled),
            contentDescription = stringResource(id = R.string.enable_disable))
    }
}

@Composable
fun PowerToggle(paddingValues: PaddingValues) {
    val scope = rememberCoroutineScope()
    val model: RoutingServiceViewModel = hiltViewModel()
    val context = LocalContext.current
    val state = model.repository.observeRouterState().observeAsState()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(checked = state.value == RouterState.DISCOVERING, onCheckedChange = { s ->
                scope.launch {
                    try {
                        if (s) {
                            setActive(context)
                            model.repository.startDiscover()
                        } else {
                            setPassive(context)
                            model.repository.stopDiscover()
                        }
                    } catch (exc: RemoteException) {
                        Toast.makeText(context, "Failed to start discovery $exc", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            })
            Text(text = "Toggle discovery")
        }
    }
}