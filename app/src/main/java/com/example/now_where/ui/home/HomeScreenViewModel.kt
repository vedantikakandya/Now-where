package com.example.now_where.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.now_where.data.EventRepository
import com.example.now_where.data.InMemoryuEventRepository
import com.example.now_where.data.NowTicker
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import com.example.now_where.ui.common.NewEventDraft
import com.example.now_where.ui.visualization.VisualizationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

enum class Screen { MAIN, PROFILE, SELECT_TIME_PREFERENCE }

data class HomeUiState(
    val day: DayState,
    val visualization: VisualizationType = VisualizationType.ROUTE,
    val currentScreen: Screen = Screen.MAIN,
    val notation: TimeNotation = TimeNotation.H24,
    val haptics: Boolean = true,
)

class HomeViewModel(
    private val repository: EventRepository = InMemoryuEventRepository(),
    ticker: Flow<LocalDateTime> = NowTicker(),
) : ViewModel() {

    private val _visualization = MutableStateFlow(VisualizationType.ROUTE)
    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    private val _notation = MutableStateFlow(TimeNotation.H24)
    private val _haptics = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(ticker, repository.events) { now, events -> now to events },
        _visualization,
        _currentScreen,
        combine(_notation, _haptics) { notation, haptic -> notation to haptic }
    ) { (now, events), viz, screen, (notation, haptic) ->
        HomeUiState(
            day = DayState(now, events, notation),
            visualization = viz,
            currentScreen = screen,
            notation = notation,
            haptics = haptic,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            day = DayState(LocalDateTime.now(), emptyList(), TimeNotation.H24),
            visualization = VisualizationType.ROUTE,
            currentScreen = Screen.MAIN,
            notation = TimeNotation.H24,
            haptics = true,
        ),
    )

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setVisualization(visualization: VisualizationType) {
        _visualization.value = visualization
    }

    fun setNotation(notation: TimeNotation) {
        _notation.value = notation
    }

    fun setHaptics(enabled: Boolean) {
        _haptics.value = enabled
    }

    fun addEvent(draft: NewEventDraft) {
        val today = LocalDate.now()
        val start = today.atTime(draft.startMinutes / 60, draft.startMinutes % 60)
        val end = today.atTime(draft.endMinutes / 60, draft.endMinutes % 60)
        val newEvent = Event(
            id = System.currentTimeMillis(),
            title = draft.title,
            start = start,
            end = end,
        )
        repository.addEvent(newEvent)
    }
}
