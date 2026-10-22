package com.example.now_where.data

import com.example.now_where.domain.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

interface EventRepository {
    val events: StateFlow<List<Event>>
    fun addEvent(event: Event)
}

class InMemoryuEventRepository: EventRepository {
    private val today = LocalDate.now()

    private fun event(id: Long, title: String, startHour: Int, startMinute: Int, durationMin: Long): Event {
        val start = today.atTime(startHour, startMinute)
        return Event(id, title, start, start.plusMinutes(durationMin))
    }

    private val _events = MutableStateFlow(
        listOf(
            event(1, "College", 9, 0, 240),
            event(2, "Lunch", 13, 0, 60),
            event(3, "Gym", 17, 0, 90),
            event(4, "Study", 19, 0, 90),
            event(5, "Dinner", 21, 0, 60),
        )
    )

    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    override fun addEvent(event: Event) {
        _events.value = (_events.value + event).sortedBy { it.start }
    }
}
