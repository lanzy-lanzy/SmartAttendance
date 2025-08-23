package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.GeofenceManager
import java.util.*
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val geofenceManager: GeofenceManager
) {
    
    sealed class EventCreationResult {
        object Success : EventCreationResult()
        data class Error(val message: String) : EventCreationResult()
    }
    
    suspend fun execute(
        name: String,
        startTime: Long,
        endTime: Long,
        location: LatLng,
        geofenceRadius: Float = 50f, // Default 50 meters
        signInStartOffset: Long = 30, // 30 minutes before
        signInEndOffset: Long = 15,   // 15 minutes after start
        signOutStartOffset: Long = 30, // 30 minutes before end
        signOutEndOffset: Long = 15    // 15 minutes after end
    ): EventCreationResult {
        try {
            // Validate input
            if (name.isBlank()) {
                return EventCreationResult.Error("Event name cannot be empty")
            }
            
            if (startTime >= endTime) {
                return EventCreationResult.Error("End time must be after start time")
            }
            
            if (startTime <= System.currentTimeMillis()) {
                return EventCreationResult.Error("Event start time must be in the future")
            }
            
            if (geofenceRadius <= 0) {
                return EventCreationResult.Error("Geofence radius must be positive")
            }
            
            // Create event ID
            val eventId = UUID.randomUUID().toString()
            
            // Create event
            val event = Event(
                id = eventId,
                name = name.trim(),
                startTime = startTime,
                endTime = endTime,
                latitude = location.latitude,
                longitude = location.longitude,
                geofenceRadius = geofenceRadius,
                signInStartOffset = signInStartOffset,
                signInEndOffset = signInEndOffset,
                signOutStartOffset = signOutStartOffset,
                signOutEndOffset = signOutEndOffset,
                isActive = true
            )
            
            // Save event
            eventRepository.insertEvent(event)
            
            // Create geofence
            geofenceManager.createGeofence(eventId, location, geofenceRadius)
            
            return EventCreationResult.Success
            
        } catch (e: Exception) {
            return EventCreationResult.Error("Failed to create event: ${e.message}")
        }
    }
}