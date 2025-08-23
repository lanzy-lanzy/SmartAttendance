package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    
    fun getAllActiveEvents(): Flow<List<Event>> {
        return eventRepository.getAllActiveEvents()
    }
    
    fun getAllEvents(): Flow<List<Event>> {
        return eventRepository.getAllEvents()
    }
    
    suspend fun getCurrentEvents(): List<Event> {
        val currentTime = System.currentTimeMillis()
        return eventRepository.getCurrentEvents(currentTime)
    }
    
    suspend fun getUpcomingEvents(): List<Event> {
        val currentTime = System.currentTimeMillis()
        return eventRepository.getUpcomingEvents(currentTime)
    }
    
    suspend fun getPastEvents(): List<Event> {
        val currentTime = System.currentTimeMillis()
        return eventRepository.getPastEvents(currentTime)
    }
    
    suspend fun getEventById(eventId: String): Event? {
        return eventRepository.getEventById(eventId)
    }
}