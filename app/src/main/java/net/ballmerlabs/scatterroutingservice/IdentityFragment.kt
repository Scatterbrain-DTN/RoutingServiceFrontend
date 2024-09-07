package net.ballmerlabs.scatterroutingservice

import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.lelloman.identicon.drawable.GithubIdenticonDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterroutingservice.ui.SbCard
import net.ballmerlabs.scatterroutingservice.ui.ScopeScatterbrainPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityView(identity: Identity) {
    val identicon = GithubIdenticonDrawable(256, 256, identity.fingerprint.hashCode())
    val painter = BitmapPainter(identicon.toBitmap(width = 256, height = 256).asImageBitmap())
    val model: RoutingServiceViewModel = hiltViewModel()
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var menuState by remember {
        mutableStateOf(false)
    }
    SbCard(
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
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                if (identity.isOwned) {
                    Badge { Text(text = "Owned!") }

                    Box {
                        Image(
                            modifier = Modifier.clickable { menuState = true },
                            painter = painterResource(id = R.drawable.ic_baseline_menu_24),
                            contentDescription = "Menu",
                        )

                        DropdownMenu(
                            expanded = menuState,
                            onDismissRequest = { menuState = false }) {
                            DropdownMenuItem(text = {
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.onBackground
                                ) }, onClick = {
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        model.repository.removeIdentity(identity)
                                    } catch (exc: RemoteException) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                ctx,
                                                "Failed to delete identity $exc",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            })
                            DropdownMenuItem(text = { Text(
                                text = "Permissions",
                                color = MaterialTheme.colorScheme.onBackground
                            ) }, onClick = {
                                showBottomSheet = true
                                menuState = false
                            })
                        }

                    }
                }
            }

        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
            BottomSheetContent(modifier = Modifier, identity)
        }
    }
}

@Composable
fun PermissionCard(
    granted: Boolean,
    p: NamePackage,
    identity: Identity,
    modifier: Modifier = Modifier,
) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    var grantState by remember {
        mutableStateOf(granted)
    }
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = p.name)
            Checkbox(checked = grantState, onCheckedChange = { c ->
                Log.v("debug", "auth checked $c")
                scope.softCancelLaunch {
                    if (grantState) {
                        model.repository.deauthorizeIdentity(identity, p.info.packageName)
                    } else {
                        model.repository.authorizeIdentity(identity, p.info.packageName)
                    }
                    grantState = c
                }
            })
        }
    }
}

@Composable
fun BottomSheetContent(modifier: Modifier = Modifier, identity: Identity) {
    val model: RoutingServiceViewModel = hiltViewModel()
    val notgranted by model.getPackages().observeAsState()
    val packages by model.getPermissions(identity).observeAsState()
    if (notgranted?.size != 0) {

        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 160.dp),
        ) {
            packages?.forEach { p ->
                item {
                    PermissionCard(
                        granted = true,
                        p = p,
                        identity,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            notgranted?.forEach { p ->
                if (packages?.contains(p) != true) {
                    item {
                        PermissionCard(
                            granted = false,
                            p = p,
                            identity,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 160.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center,
                text = "No scatterbrain clients found. Try opening a scatterbrain compatible app"
            )
        }
    }
}

@Composable
fun IdentityList(modifier: Modifier = Modifier) {
    Log.v("debug", "IdentityList recompose")
    val model: RoutingServiceViewModel = hiltViewModel()
    val identities by model.repository.observeIdentitiesLiveData().observeAsState()
    if (identities?.isNotEmpty() == false) {
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
                    IdentityView(id)
                }
            }

            identities?.filter { v -> !v.isOwned }?.forEach { id ->
                item {
                    IdentityView(id)
                }
            }
        }
    }
}


@Composable
fun IdentityManagement() {
    ScopeScatterbrainPermissions(modifier = Modifier.fillMaxSize()) {
        IdentityList()
    }
}