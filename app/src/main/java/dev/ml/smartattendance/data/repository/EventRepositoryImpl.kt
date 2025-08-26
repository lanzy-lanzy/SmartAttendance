package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.EventDao
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val firestoreService: FirestoreService
) : EventRepository {
    
    override fun getAllActiveEvents(): Flow<List<Event>> {
        // Firebase-first approach: Get active events from Firebase, then use local as fallback
        return firestoreService.getEventsFlow()
            .map { firebaseEvents ->
                val activeEvents = firebaseEvents.filter { it.isActive }
                if (activeEvents.isNotEmpty()) {
                    // Cache Firebase data locally
                    eventDao.insertEvents(firebaseEvents)
                    activeEvents
                } else {
                    // Fallback to local data only if Firebase returns empty list
                    eventDao.getAllActiveEvents().first()
                }
            }
            .catch { e ->
                // If Firebase throws an error, fallback to local
                emit(eventDao.getAllActiveEvents().first())
            }
    }
    
    override fun getAllEvents(): Flow<List<Event>> {
        // Firebase-first approach: Get all events from Firebase, then use local as fallback
        return firestoreService.getEventsFlow()
            .map { firebaseEvents ->
                if (firebaseEvents.isNotEmpty()) {
                    // Cache Firebase data locally
                    eventDao.insertEvents(firebaseEvents)
                    firebaseEvents
                } else {
                    // Fallback to local data only if Firebase returns empty list
                    eventDao.getAllEvents().first()
                }
            }
            .catch { e ->
                // If Firebase throws an error, fallback to local
                emit(eventDao.getAllEvents().first())
            }
    }
    
    override suspend fun getEventById(eventId: String): Event? {
        android.util.Log.d("EventRepository", "Getting event by ID: '$eventId'")
        
        if (eventId.isBlank()) {
            android.util.Log.e("EventRepository", "Invalid event ID (blank)")
            return null
        }
        
        // Clean the ID to prevent whitespace issues
        val cleanId = eventId.trim()
        
        // Try Firebase first
        try {
            android.util.Log.d("EventRepository", "Fetching event from Firebase: '$cleanId'")
            val firebaseEvent = firestoreService.getEvent(cleanId)
            
            if (firebaseEvent != null) {
                android.util.Log.d("EventRepository", "Successfully retrieved event from Firebase: ${firebaseEvent.name}")
                // Make sure ID is set properly
                val fixedEvent = firebaseEvent.copy(id = cleanId)
                // Cache the event locally for future use
                try {
                    eventDao.insertEvent(fixedEvent)
                    android.util.Log.d("EventRepository", "Cached event in local database")
                } catch (cacheEx: Exception) {
                    android.util.Log.e("EventRepository", "Failed to cache event: ${cacheEx.message}")
                }
                return fixedEvent
            } else {
                android.util.Log.w("EventRepository", "Event not found in Firebase: '$cleanId'")
            }
        } catch (e: Exception) {
            // If Firebase fails, fall back to local cache
            android.util.Log.e("EventRepository", "Error fetching from Firebase: ${e.message}")
        }
        
        // Try local database
        try {
            android.util.Log.d("EventRepository", "Falling back to local database for event: '$cleanId'")
            val localEvent = eventDao.getEventById(cleanId)
            
            if (localEvent != null) {
                android.util.Log.d("EventRepository", "Found event in local database: ${localEvent.name}")
                
                // Attempt to refresh from Firebase in background
                try {
                    android.util.Log.d("EventRepository", "Attempting to refresh stale data from Firebase")
                    val refreshedEvent = firestoreService.getEvent(cleanId)
                    if (refreshedEvent != null && refreshedEvent != localEvent) {
                        android.util.Log.d("EventRepository", "Updating local cache with fresh data")
                        eventDao.insertEvent(refreshedEvent)
                        return refreshedEvent
                    }
                } catch (refreshEx: Exception) {
                    android.util.Log.e("EventRepository", "Failed to refresh data: ${refreshEx.message}")
                }
                
                return localEvent
            } else {
                android.util.Log.w("EventRepository", "Event not found in local database: '$cleanId'")
            }
        } catch (e: Exception) {
            android.util.Log.e("EventRepository", "Error fetching from local database: ${e.message}")
        }
        
        // Try one more direct approach to Firestore
        try {
            android.util.Log.d("EventRepository", "Trying direct Firestore module as last resort for: '$cleanId'")
            val firestoreModule = dev.ml.smartattendance.data.service.FirebaseModule
            val directService = firestoreModule.provideFirestoreService()
            val directEvent = directService.getEvent(cleanId)
            
            if (directEvent != null) {
                android.util.Log.d("EventRepository", "Found event via direct Firestore module: ${directEvent.name}")
                return directEvent
            }
        } catch (directEx: Exception) {
            android.util.Log.e("EventRepository", "Direct Firestore attempt failed: ${directEx.message}")
        }
        
        android.util.Log.e("EventRepository", "Event not found anywhere: '$cleanId'")
        return null
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