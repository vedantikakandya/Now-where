package com.example.now_where.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.now_where.domain.Event
import com.example.now_where.ui.common.AddEventDialog
import com.example.now_where.ui.common.NowWhereTopBar
import com.example.now_where.ui.common.ProfileScreen
import com.example.now_where.ui.common.ProfileUiState
import com.example.now_where.ui.common.SelectTimePreferenceScreen
import com.example.now_where.ui.common.TimePerception
import com.example.now_where.ui.theme.LocalHapticsEnabled
import com.example.now_where.ui.theme.NowWhereColors
import com.example.now_where.ui.theme.VibrationHelper
import com.example.now_where.ui.theme.touchHaptics
import com.example.now_where.ui.visualization.VisualizationType
import com.example.now_where.ui.visualization.block.BlocksVisualization
import com.example.now_where.ui.visualization.circular_day.CircularDayVisualization
import com.example.now_where.ui.visualization.clock.ClockVisualization
import com.example.now_where.ui.visualization.day_progress.DayProgressVisualization
import com.example.now_where.ui.visualization.distance.DistanceVisualization
import com.example.now_where.ui.visualization.ladder.LadderVisualization
import com.example.now_where.ui.visualization.route.RouteVisualization
import com.example.now_where.ui.visualization.water.WaterFlowVisualization

fun TimePerception.toVisualizationType(): VisualizationType = when (this) {
    TimePerception.Route -> VisualizationType.ROUTE
    TimePerception.Distance -> VisualizationType.DISTANCE
    TimePerception.DayProgress -> VisualizationType.DAYPROGRESS
    TimePerception.Blocks -> VisualizationType.BLOCK
    TimePerception.Clock -> VisualizationType.CLOCK
    TimePerception.CircularDay -> VisualizationType.CIRCULAR
    TimePerception.FlowOfWater -> VisualizationType.WATER
    TimePerception.TimeLadder -> VisualizationType.LADDER
    else -> VisualizationType.ROUTE
}

fun VisualizationType.toTimePerception(): TimePerception = when (this) {
    VisualizationType.ROUTE -> TimePerception.Route
    VisualizationType.DISTANCE -> TimePerception.Distance
    VisualizationType.DAYPROGRESS -> TimePerception.DayProgress
    VisualizationType.BLOCK -> TimePerception.Blocks
    VisualizationType.CIRCULAR -> TimePerception.CircularDay
    VisualizationType.CLOCK -> TimePerception.Clock
    VisualizationType.LADDER -> TimePerception.TimeLadder
    VisualizationType.WATER -> TimePerception.FlowOfWater
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    val context = LocalContext.current

    val onEventClick: (Event) -> Unit = { event ->
        VibrationHelper.performTouchHaptic(context, state.haptics)
        editingEvent = event
        showAddDialog = true
    }
    CompositionLocalProvider(LocalHapticsEnabled provides state.haptics) {

        Column(
            Modifier
                .fillMaxSize()
                .background(NowWhereColors.Background).touchHaptics()
        ) {
            // Top app bar is present at all times!
            NowWhereTopBar(
                onLogoClick = { viewModel.navigateTo(Screen.PROFILE) },
                onPreferenceClick = { viewModel.navigateTo(Screen.SELECT_TIME_PREFERENCE) },
                haptics = state.haptics
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.currentScreen) {
                    Screen.MAIN -> {
                        Box(Modifier.fillMaxSize()) {
                            when (state.visualization) {
                                VisualizationType.ROUTE -> RouteVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize(),
                                    onEventClick = onEventClick
                                )

                                VisualizationType.DISTANCE -> DistanceVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize(),
                                    onEventClick = onEventClick
                                )

                                VisualizationType.DAYPROGRESS -> DayProgressVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize()
                                )

                                VisualizationType.BLOCK -> BlocksVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize()
                                )

                                VisualizationType.CIRCULAR -> CircularDayVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize()
                                )

                                VisualizationType.CLOCK -> ClockVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize()
                                )

                                VisualizationType.LADDER -> LadderVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize(),
                                    onEventClick = onEventClick
                                )

                                VisualizationType.WATER -> WaterFlowVisualization(
                                    day = state.day,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            EdgeFade(top = false, modifier = Modifier.align(Alignment.BottomCenter))

                            CircleIconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "Add an event",
                                onClick = {
                                    editingEvent = null
                                    showAddDialog = true
                                },
                                size = 56.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 24.dp),
                            )
                        }
                    }

                    Screen.PROFILE -> {
                        ProfileScreen(
                            state = ProfileUiState(
                                defaultVisualizationName = state.visualization.toTimePerception().title,
                                notation = state.notation,
                                haptics = state.haptics,
                            ),
                            onBack = { viewModel.navigateTo(Screen.MAIN) },
                            onDefaultVisualizationClick = { viewModel.navigateTo(Screen.SELECT_TIME_PREFERENCE) },
                            onNotationChange = { viewModel.setNotation(it) },
                            onHapticsChange = { viewModel.setHaptics(it) },

                        )
                    }

                    Screen.SELECT_TIME_PREFERENCE -> {
                        SelectTimePreferenceScreen(
                            current = state.visualization.toTimePerception(),
                            onConfirm = { perception ->
                                viewModel.setVisualization(perception.toVisualizationType())
                                viewModel.navigateTo(Screen.MAIN)
                            },
                            haptics = state.haptics
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEventDialog(
                onDismiss = {
                    showAddDialog = false
                    editingEvent = null
                },
                onAdd = { draft ->
                    viewModel.addEvent(draft)
                    showAddDialog = false
                    editingEvent = null
                },
                onUpdate = { draft ->
                    editingEvent?.let { viewModel.updateEvent(it.id, draft) }
                    showAddDialog = false
                    editingEvent = null
                },
                onDelete = if (editingEvent != null) {
                    {
                        editingEvent?.let { viewModel.deleteEvent(it.id) }
                        showAddDialog = false
                        editingEvent = null
                    }
                } else null,
                editingEvent = editingEvent,
                notation = state.notation
            )
        }
    }
}

@Composable
private fun EdgeFade(top: Boolean, modifier: Modifier = Modifier) {
    val colors = listOf(NowWhereColors.Background, Color.Transparent)
    Box(
        modifier
            .fillMaxWidth()
            .height(if (top) 112.dp else 140.dp)
            .background(Brush.verticalGradient(if (top) colors else colors.reversed()))
    )
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NowWhereColors.Surface)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = NowWhereColors.Ink)
    }
}
