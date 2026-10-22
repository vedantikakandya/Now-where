package com.example.now_where.data

import android.content.Context
import com.example.now_where.domain.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

interface EventRepository {
    val events: StateFlow<List<Event>>
    fun addEvent(event: Event)
    fun updateEvent(event: Event)
    fun deleteEvent(eventId: Long)
}

class InMemoryuEventRepository(
    private val context: Context? = null
) : EventRepository {

    private val prefs = context?.getSharedPreferences("nowwhere_user_events_prefs", Context.MODE_PRIVATE)
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {
        loadEvents()
    }

    override fun addEvent(event: Event) {
        _events.value = (_events.value + event).sortedBy { it.start }
        saveEvents()
    }

    override fun updateEvent(event: Event) {
        _events.value = _events.value.map { if (it.id == event.id) event else it }.sortedBy { it.start }
        saveEvents()
    }

    override fun deleteEvent(eventId: Long) {
        _events.value = _events.value.filterNot { it.id == eventId }
        saveEvents()
    }

    private fun saveEvents() {
        val prefs = prefs ?: return
        val serialized = _events.value.joinToString(";;") { e ->
            "${e.id}||${e.title}||${e.start}||${e.end}"
        }
        prefs.edit().putString("events_storage_v1", serialized).apply()
    }

    private fun loadEvents() {
        val prefs = prefs ?: return
        val raw = prefs.getString("events_storage_v1", null) ?: return
        if (raw.isBlank()) return
        val parsed = raw.split(";;").mapNotNull { entry ->
            val parts = entry.split("||")
            if (parts.size == 4) {
                try {
                    Event(
                        id = parts[0].toLong(),
                        title = parts[1],
                        start = LocalDateTime.parse(parts[2]),
                        end = LocalDateTime.parse(parts[3])
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        _events.value = parsed.sortedBy { it.start }
    }
}
