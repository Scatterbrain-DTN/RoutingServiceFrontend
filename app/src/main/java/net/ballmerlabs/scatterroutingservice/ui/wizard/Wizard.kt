package net.ballmerlabs.scatterroutingservice.ui.wizard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.uscatterbrain.dataStore

const val SHOW_WIZARD = "show-wizard"

@Composable
fun CityAnimation(modifier: Modifier = Modifier) {
    val trans = rememberInfiniteTransition("city")
    var length by remember { mutableStateOf(IntSize.Zero) }
    val xwalk by trans.animateFloat(
        initialValue = length.width.pxToDp().value/16,
        targetValue = length.width.pxToDp().value/1.5.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "xwalk"
    )

    val ywalk by trans.animateFloat(
        initialValue = 0.dp.value,
        targetValue = length.height.pxToDp().value,
        animationSpec = infiniteRepeatable(tween(2000/2), RepeatMode.Reverse),
        label = "ywalk"
    )
    Row(
        modifier = modifier.onGloballyPositioned { v -> length = v.size },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Icon(
            modifier = Modifier.size(15.dp).offset(x = xwalk.dp, y= -1*ywalk.dp),
            painter = painterResource(R.drawable.baseline_insert_drive_file_24),
            contentDescription = "file"
        )
        Icon(
            painter = painterResource(R.drawable.city_plain),
            contentDescription = "City skyline"
        )
    }
}

@Composable
fun SendAnimation(modifier: Modifier = Modifier) {
    val trans = rememberInfiniteTransition("welcome")
    var end by remember { mutableStateOf(IntSize.Zero) }
    var start by remember { mutableStateOf(IntSize.Zero) }
    var person by remember { mutableStateOf(IntSize.Zero) }
    var showfile by remember { mutableStateOf(false) }
    val endWalk = end.width.pxToDp().value- start.width.pxToDp().value - person.width.pxToDp().value
    val walk by trans.animateFloat(
        initialValue = 0.dp.value,
        targetValue = endWalk,
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse),
        label = "walk"
    )

    if (walk in 0.toFloat()..0.5.toFloat())
        showfile = true
    if (walk in (endWalk-0.5).toFloat()..endWalk)
        showfile = false

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1F)
                .onGloballyPositioned { v -> end = v.size },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(60.dp).onGloballyPositioned { v ->
                    start = v.size
                },
                painter = painterResource(R.drawable.baseline_groups_24),
                contentDescription = "Group of people"
            )
            Row(
                modifier = Modifier.wrapContentWidth()
                    .offset(x = walk.dp)
            ) {
                if (showfile) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                        contentDescription = "file"
                    )
                }
                Icon(
                    modifier = Modifier.size(30.dp).onGloballyPositioned { v ->
                        person = v.size
                    },
                    painter = painterResource(R.drawable.baseline_person_outline_24),
                    contentDescription = "person"
                )

            }
        }
        Icon(
            modifier = Modifier.size(60.dp),
            painter = painterResource(R.drawable.baseline_groups_24),
            contentDescription = "Group of people"
        )
    }
}

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
        var state by remember { mutableStateOf(model.states[0]) }
        var currentPage by remember { mutableIntStateOf(0) }
        Surface(modifier = modifier) {
            val pagerState = rememberPagerState(pageCount = { model.states.size })
                Row(modifier = Modifier.fillMaxSize(),horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val arrowWidth = 15.dp
                    val arrowHeight = 40.dp
                    val scrollstate = rememberScrollState()
                    Icon(
                        modifier = Modifier.size(arrowWidth, arrowHeight).graphicsLayer(scaleY = 5.0.toFloat()).alpha(0.5.toFloat()),
                        painter =  painterResource(R.drawable.baseline_keyboard_double_arrow_left_24),
                        contentDescription = "next"
                    )
                    Column(modifier = modifier.padding(start = 4.dp).weight(1F).verticalScroll(scrollstate), verticalArrangement = Arrangement.SpaceBetween) {
                        HorizontalPager(modifier = Modifier.weight(1F), state = pagerState) { page ->
                            currentPage = page
                            state = model.states[page]

                            Column(modifier = Modifier.fillMaxHeight()) {
                                Text(
                                    modifier = Modifier.padding(vertical = 32.dp),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.headlineLarge,
                                    text = state.title
                                )
                                Box(
                                    modifier = Modifier.weight(1F),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        state.body()
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            if (currentPage > 0) {
                                TextButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            pagerState.animateScrollToPage(currentPage - 1)
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
                                    if (currentPage < pagerState.pageCount - 1)
                                        pagerState.animateScrollToPage(currentPage + 1)
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
                    Icon(
                        modifier =
                        Modifier.size(arrowWidth, arrowHeight).graphicsLayer(scaleY = 5.0.toFloat()).alpha(0.5.toFloat()),
                        painter =  painterResource(R.drawable.baseline_keyboard_double_arrow_right_24),
                        contentDescription = "next"
                    )
                }
        }
    } else {
        content()
    }

}