package dev.ml.smartattendance.domain.repository

import dev.ml.smartattendance.data.entity.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getAllActiveEvents(): Flow<List<Event>>
    fun getAllEvents(): Flow<List<Event>>
    suspend fun getEventById(eventId: String): Event?
    suspend fun getCurrentEvents(currentTime: Long): List<Event>
    suspend fun getUpcomingEvents(currentTime: Long): List<Event>
    suspend fun getPastEvents(currentTime: Long): List<Event>
    suspend fun insertEvent(event: Event)
    suspend fun insertEvents(events: List<Event>)
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(event: Event)
    suspend fun deactivateEvent(eventId: String)
    suspend fun getActiveEventCount(): Int
}