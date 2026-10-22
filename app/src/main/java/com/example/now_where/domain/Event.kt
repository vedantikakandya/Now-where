package com.example.now_where.domain

import java.time.LocalDateTime

data class Event(
    val id: Long,
    val title: String,
    var start: LocalDateTime,
    var end: LocalDateTime,
)

enum class EventPhase { PAST, ONGOING, UPCOMING }


fun Event.phaseAt(now: LocalDateTime): EventPhase = when {
    now.isBefore(start) -> EventPhase.UPCOMING
    now.isBefore(end) -> EventPhase.ONGOING
    else -> EventPhase.PAST
}