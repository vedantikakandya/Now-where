package com.example.now_where.domain

import java.time.Duration
import java.time.LocalDateTime

data class DayState(
    val now: LocalDateTime,
    val events: List<Event>,
    val notion: TimeNotation
) {
    val startOfDay: LocalDateTime get() = now.toLocalDate().atStartOfDay()
    val sortedEvents: List<Event> get() = events.sortedBy { it.start }
    val nextEvent: Event? by lazy { sortedEvents.firstOrNull { it.phaseAt(now) == EventPhase.UPCOMING } }
    val notation: TimeNotation get() = notion

    fun minutesUntil(event: Event): Long {
        val seconds = Duration.between(now, event.start).seconds.coerceAtLeast(0)
        return (seconds + 59) / 60
    }
}
