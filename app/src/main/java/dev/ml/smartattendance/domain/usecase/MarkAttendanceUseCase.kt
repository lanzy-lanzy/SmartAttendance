package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.model.biometric.AuthResult
import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.service.BiometricAuthenticator
import dev.ml.smartattendance.domain.service.GeofenceManager
import dev.ml.smartattendance.domain.service.LocationProvider
import dev.ml.smartattendance.domain.service.PenaltyCalculationService
import java.util.UUID
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val eventRepository: EventRepository,
    private val studentRepository: StudentRepository,
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
            // 1. Validate student exists
            val student = studentRepository.getStudentById(studentId)
            if (student == null) {
                return AttendanceResult.Error("Student with ID '$studentId' not found. Please ensure you are properly registered in the system.")
            }
            
            if (!student.isActive) {
                return AttendanceResult.Error("Student account is inactive. Please contact administrator.")
            }
            
            // 2. Check if event exists and is active
            val event = eventRepository.getEventById(eventId)
            if (event == null) {
                return AttendanceResult.Error("Event not found or has been removed.")
            }
            
            if (!event.isActive) {
                return AttendanceResult.Error("Event is no longer active.")
            }

            // 3. Check if attendance already marked
            val existingRecord = attendanceRepository.getAttendanceRecord(studentId, eventId)
            if (existingRecord != null) {
                return AttendanceResult.Error("Attendance already marked for this event")
            }
            
            // FOR DEMO/TESTING: Skip location and geofence checks
            // In production, you would uncomment the location validation below:
            /*
            // 4. Get current location
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation == null) {
                return AttendanceResult.Error("Unable to get current location. Please ensure location services are enabled and try again.")
            }
            
            // 5. Check geofence
            val isWithinGeofence = geofenceManager.isWithinGeofence(eventId, currentLocation)
            if (!isWithinGeofence) {
                return AttendanceResult.Error("You are not within the event location")
            }
            */
            
            // 6. Determine attendance status and penalty
            val currentTime = System.currentTimeMillis()
            val (status, penalty) = determineAttendanceStatus(event, currentTime)
            
            // 7. Create attendance record (with demo location)
            val attendanceRecord = AttendanceRecord(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                eventId = eventId,
                timestamp = currentTime,
                status = status,
                penalty = penalty,
                latitude = event.latitude, // Use event location for demo
                longitude = event.longitude, // Use event location for demo
                synced = false
            )
            
            // 8. Save attendance record
            attendanceRepository.insertAttendanceRecord(attendanceRecord)
            
            return AttendanceResult.Success
            
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Handle foreign key constraint violations specifically
            return AttendanceResult.Error("Database constraint error: Please ensure you are a registered student and the event exists. Contact administrator if this persists.")
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