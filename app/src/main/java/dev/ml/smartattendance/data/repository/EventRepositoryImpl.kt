package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.EventDao
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val firestoreService: FirestoreService
) : EventRepository {
    
    override fun getAllActiveEvents(): Flow<List<Event>> {
        // Cloud-first approach: Get active events from Firebase with local fallback
        return firestoreService.getEventsFlow()
            .onStart {
                // Start with local data for immediate display
                emit(eventDao.getAllActiveEvents().first())
            }
            .map { firebaseEvents ->
                val activeEvents = firebaseEvents.filter { it.isActive }
                if (activeEvents.isNotEmpty()) {
                    // Cache Firebase data locally
                    eventDao.insertEvents(firebaseEvents)
                    activeEvents
                } else {
                    // Fallback to local data
                    eventDao.getAllActiveEvents().first()
                }
            }
    }
    
    override fun getAllEvents(): Flow<List<Event>> {
        // Cloud-first approach: Firebase events with local fallback
        return firestoreService.getEventsFlow()
            .onStart {
                // Start with local data for immediate display
                emit(eventDao.getAllEvents().first())
            }
            .map { firebaseEvents ->
                if (firebaseEvents.isNotEmpty()) {
                    // Cache Firebase data locally
                    eventDao.insertEvents(firebaseEvents)
                    firebaseEvents
                } else {
                    // Fallback to local data
                    eventDao.getAllEvents().first()
                }
            }
    }
    
    override suspend fun getEventById(eventId: String): Event? {
        // Try Firebase first, fallback to local
        return try {
            firestoreService.getEvent(eventId) ?: eventDao.getEventById(eventId)
        } catch (e: Exception) {
            eventDao.getEventById(eventId)
        }
    }
    
    override suspend fun getCurrentEvents(currentTime: Long): List<Event> {
        return try {
            val allEvents = firestoreService.getAllEvents()
            allEvents.filter { event ->
                event.isActive && 
                event.startTime <= currentTime && 
                event.endTime >= currentTime
            }
        } catch (e: Exception) {
            eventDao.getCurrentEvents(currentTime)
        }
    }
    
    override suspend fun getUpcomingEvents(currentTime: Long): List<Event> {
        return try {
            val allEvents = firestoreService.getAllEvents()
            allEvents.filter { event ->
                event.isActive && event.startTime > currentTime
            }
        } catch (e: Exception) {
            eventDao.getUpcomingEvents(currentTime)
        }
    }
    
    override suspend fun getPastEvents(currentTime: Long): List<Event> {
        return try {
            val allEvents = firestoreService.getAllEvents()
            allEvents.filter { event ->
                event.endTime < currentTime
            }
        } catch (e: Exception) {
            eventDao.getPastEvents(currentTime)
        }
    }
    
    override suspend fun insertEvent(event: Event) {
        try {
            // Save to Firebase first
            val success = firestoreService.createEvent(event)
            if (success) {
                // Cache locally for offline access
                eventDao.insertEvent(event)
            } else {
                throw Exception("Failed to save event to Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            eventDao.insertEvent(event)
            throw e
        }
    }
    
    override suspend fun insertEvents(events: List<Event>) {
        // Insert events one by one to Firebase with local caching
        events.forEach { event ->
            try {
                val success = firestoreService.createEvent(event)
                if (success) {
                    eventDao.insertEvent(event)
                }
            } catch (e: Exception) {
                // Continue with other events
            }
        }
    }
    
    override suspend fun updateEvent(event: Event) {
        try {
            // Update in Firebase first
            val success = firestoreService.updateEvent(event)
            if (success) {
                // Update local cache
                eventDao.updateEvent(event)
            } else {
                throw Exception("Failed to update event in Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            eventDao.updateEvent(event)
            throw e
        }
    }
    
    override suspend fun deleteEvent(event: Event) {
        try {
            // Delete from Firebase with cascade (includes attendance records)
            val success = firestoreService.deleteEventWithCascade(event.id)
            if (success) {
                // Delete from local cache
                eventDao.deleteEvent(event)
            } else {
                throw Exception("Failed to delete event from Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            eventDao.deleteEvent(event)
            throw e
        }
    }
    
    override suspend fun deactivateEvent(eventId: String) {
        try {
            // Get event, update status, and save
            val event = getEventById(eventId)
            if (event != null) {
                val updatedEvent = event.copy(isActive = false)
                updateEvent(updatedEvent)
            }
        } catch (e: Exception) {
            // Fallback to local only
            eventDao.deactivateEvent(eventId)
            throw e
        }
    }
    
    override suspend fun getActiveEventCount(): Int {
        return try {
            val allEvents = firestoreService.getAllEvents()
            allEvents.count { it.isActive }
        } catch (e: Exception) {
            eventDao.getActiveEventCount()
        }
    }
}