package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.model.biometric.AuthResult
import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.BiometricAuthenticator
import dev.ml.smartattendance.domain.service.GeofenceManager
import dev.ml.smartattendance.domain.service.LocationProvider
import dev.ml.smartattendance.domain.service.PenaltyCalculationService
import java.util.UUID
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val eventRepository: EventRepository,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val locationProvider: LocationProvider,
    private val geofenceManager: GeofenceManager,
    private val penaltyCalculationService: PenaltyCalculationService
) {
    
    sealed class AttendanceResult {
        object Success : AttendanceResult()
        data class Error(val message: String) : AttendanceResult()
    }
    
    suspend fun execute(studentId: String, eventId: String): AttendanceResult {
        try {
            // 1. Check if event exists and is active
            val event = eventRepository.getEventById(eventId)
                ?: return AttendanceResult.Error("Event not found")
            
            // 2. Check if attendance already marked
            val existingRecord = attendanceRepository.getAttendanceRecord(studentId, eventId)
            if (existingRecord != null) {
                return AttendanceResult.Error("Attendance already marked for this event")
            }
            
            // 3. Get current location
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation == null) {
                return AttendanceResult.Error("Unable to get current location. Please ensure location services are enabled and try again.")
            }
            
            // 4. Check geofence
            val isWithinGeofence = geofenceManager.isWithinGeofence(eventId, currentLocation)
            if (!isWithinGeofence) {
                return AttendanceResult.Error("You are not within the event location")
            }
            
            // 5. Perform biometric authentication - SKIPPED (handled in UI)
            // Authentication is already completed in the UI layer before calling this use case
            
            // 6. Determine attendance status and penalty
            val currentTime = System.currentTimeMillis()
            val (status, penalty) = determineAttendanceStatus(event, currentTime)
            
            // 7. Create attendance record
            val attendanceRecord = AttendanceRecord(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                eventId = eventId,
                timestamp = currentTime,
                status = status,
                penalty = penalty,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                synced = false
            )
            
            // 8. Save attendance record
            attendanceRepository.insertAttendanceRecord(attendanceRecord)
            
            return AttendanceResult.Success
            
        } catch (e: Exception) {
            return AttendanceResult.Error("Failed to mark attendance: ${e.message}")
        }
    }
    
    private fun determineAttendanceStatus(event: dev.ml.smartattendance.data.entity.Event, currentTime: Long): Pair<AttendanceStatus, PenaltyType?> {
        val eventStartTime = event.startTime
        val signInWindow = event.signInWindow
        
        val allowedStartTime = eventStartTime - (signInWindow.startOffset * 60 * 1000)
        val allowedEndTime = eventStartTime + (signInWindow.endOffset * 60 * 1000)
        
        return when {
            currentTime < allowedStartTime -> {
                // Too early
                AttendanceStatus.PRESENT to null
            }
            currentTime <= eventStartTime -> {
                // On time
                AttendanceStatus.PRESENT to null
            }
            currentTime <= allowedEndTime -> {
                // Late but within allowed window
                val minutesLate = (currentTime - eventStartTime) / (60 * 1000)
                val penalty = when {
                    minutesLate <= 5 -> PenaltyType.WARNING
                    minutesLate <= 15 -> PenaltyType.MINOR
                    minutesLate <= 30 -> PenaltyType.MAJOR
                    else -> PenaltyType.CRITICAL
                }
                AttendanceStatus.LATE to penalty
            }
            else -> {
                // Too late
                AttendanceStatus.ABSENT to PenaltyType.CRITICAL
            }
        }
    }
}