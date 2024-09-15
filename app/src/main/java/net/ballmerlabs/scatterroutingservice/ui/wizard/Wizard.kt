package net.ballmerlabs.scatterroutingservice.ui.wizard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.uscatterbrain.dataStore

const val SHOW_WIZARD = "show-wizard"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FirstStartWizard(
    modifier: Modifier = Modifier,
    batteryGranted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val model: WizardViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    val show by context.dataStore.data.map { v ->
        withContext(Dispatchers.IO) {
            v[booleanPreferencesKey(SHOW_WIZARD)] ?: true
        }
    }.collectAsState(false)

    if (show && model.states.isNotEmpty()) {
        Surface {

            val pagerState = rememberPagerState(pageCount = { model.states.size })

            HorizontalPager(state = pagerState) { page ->
                val state = model.states[page]
                Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
                    val scrollstate = rememberScrollState()
                    Column(
                        modifier = Modifier.scrollable(
                            scrollstate,
                            orientation = Orientation.Vertical
                        )
                    ) {
                        Text(
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineLarge,
                            text = state.title
                        )
                        state.body()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        if (page > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        pagerState.animateScrollToPage(page - 1)
                                    }
                                }) { Text("Previous") }
                        } else {
                            Column { }
                        }

                        if (state.battery && !model.batteryState.value) {
                            Button(onClick = model.onBattery) {
                                Text("Grant battery permission")
                            }
                        } else if (model.batteryState.value && state.battery) {
                            Text("Granted!")
                        }
                        state.Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                if (page < pagerState.pageCount - 1)
                                    pagerState.animateScrollToPage(page + 1)
                                else
                                    withContext(Dispatchers.IO) {
                                        context.dataStore.edit { p ->
                                            p[booleanPreferencesKey(SHOW_WIZARD)] = false
                                        }
                                    }
                            }
                        })
                    }
                }
            }
        }
    } else {
        content()
    }

}