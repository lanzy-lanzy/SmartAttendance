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
import kotlinx.coroutines.delay
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
            android.util.Log.d("MarkAttendanceUseCase", "Starting attendance marking for student: '$studentId', event: '$eventId'")
            
            // 1. Validate student exists using Firestore directly
            android.util.Log.d("MarkAttendanceUseCase", "Checking student in Firestore...")
            var student: dev.ml.smartattendance.data.entity.Student? = null
            var studentRetryCount = 0
            val maxStudentRetries = 3
            
            while (student == null && studentRetryCount < maxStudentRetries) {
                try {
                    val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                    student = firestoreService.getStudent(studentId)
                    
                    if (student != null) {
                        android.util.Log.d("MarkAttendanceUseCase", "Successfully fetched student on try ${studentRetryCount + 1}: ${student.name}")
                        break
                    } else {
                        android.util.Log.w("MarkAttendanceUseCase", "Student not found on try ${studentRetryCount + 1}, retrying...")
                        studentRetryCount++
                        if (studentRetryCount < maxStudentRetries) {
                            kotlinx.coroutines.delay(500L * (studentRetryCount + 1))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MarkAttendanceUseCase", "Error getting student from Firestore on try ${studentRetryCount + 1}: ${e.message}", e)
                    studentRetryCount++
                    if (studentRetryCount < maxStudentRetries) {
                        kotlinx.coroutines.delay(500L * (studentRetryCount + 1))
                    }
                }
            }
            
            if (student == null) {
                android.util.Log.e("MarkAttendanceUseCase", "Student not found in Firestore after $maxStudentRetries retries: '$studentId'")
                return AttendanceResult.Error("Student with ID '$studentId' not found in Firestore. Please ensure you are properly registered in the system.")
            }
            
            if (!student.isActive) {
                android.util.Log.e("MarkAttendanceUseCase", "Student account is inactive: '$studentId'")
                return AttendanceResult.Error("Student account is inactive. Please contact administrator.")
            }
            
            // 2. Check if event exists and is active using Firestore directly with improved retries
            android.util.Log.d("MarkAttendanceUseCase", "Checking event in Firestore...")
            var event: dev.ml.smartattendance.data.entity.Event? = null
            var retryCount = 0
            val maxRetries = 5 // Increased from 3 to 5 for better reliability
            
            while (event == null && retryCount < maxRetries) {
                try {
                    val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                    event = firestoreService.getEvent(eventId)
                    
                    if (event != null) {
                        android.util.Log.d("MarkAttendanceUseCase", "Successfully fetched event on try ${retryCount + 1}: ${event.name}")
                        break
                    } else {
                        android.util.Log.w("MarkAttendanceUseCase", "Event not found on try ${retryCount + 1}, retrying...")
                        retryCount++
                        if (retryCount < maxRetries) {
                            // Only delay if we're going to retry
                            kotlinx.coroutines.delay(500L * (retryCount + 1)) // Increased delay for better reliability
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MarkAttendanceUseCase", "Error getting event from Firestore on try ${retryCount + 1}: ${e.message}", e)
                    retryCount++
                    if (retryCount < maxRetries) {
                        // Only delay if we're going to retry
                        kotlinx.coroutines.delay(500L * (retryCount + 1)) // Increased delay for better reliability
                    }
                }
            }
            
            if (event == null) {
                android.util.Log.e("MarkAttendanceUseCase", "Event not found in Firestore after $maxRetries retries: '$eventId'")
                return AttendanceResult.Error("Event not found or has been removed from Firestore. Please try again or contact administrator.")
            }
            
            if (!event.isActive) {
                android.util.Log.e("MarkAttendanceUseCase", "Event is not active: '$eventId'")
                return AttendanceResult.Error("Event is no longer active.")
            }
            
            // 3. Check if current time is within the allowed sign-in window
            val currentTime = System.currentTimeMillis()
            val eventStartTime = event.startTime
            val signInWindow = event.signInWindow
            
            val allowedStartTime = eventStartTime - (signInWindow.startOffset * 60 * 1000)
            val allowedEndTime = eventStartTime + (signInWindow.endOffset * 60 * 1000)
            
            android.util.Log.d("MarkAttendanceUseCase", "Checking attendance timing: Current time: ${java.util.Date(currentTime)}, " +
                          "Allowed window: ${java.util.Date(allowedStartTime)} to ${java.util.Date(allowedEndTime)}")
            
            // Enforce time window restrictions (always in production)
            if (currentTime < allowedStartTime) {
                // Too early - Calculate minutes until sign-in window opens
                val minutesUntilStart = (allowedStartTime - currentTime) / (60 * 1000)
                android.util.Log.e("MarkAttendanceUseCase", "Too early for attendance: $minutesUntilStart minutes before window opens")
                return AttendanceResult.Error("Attendance window is not open yet. Please try again in $minutesUntilStart minutes.")
            }
            
            if (currentTime > allowedEndTime) {
                // Too late - Attendance window closed
                android.util.Log.e("MarkAttendanceUseCase", "Too late for attendance: Sign-in window closed")
                return AttendanceResult.Error("Attendance window has closed. Contact your administrator if you need to mark attendance.")
            }

            // 4. Check if attendance already marked using Firestore directly
            android.util.Log.d("MarkAttendanceUseCase", "Checking existing attendance in Firestore...")
            val existingRecord = try {
                val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                firestoreService.getAttendanceRecord(studentId, eventId)
            } catch (e: Exception) {
                android.util.Log.e("MarkAttendanceUseCase", "Error checking existing attendance: ${e.message}", e)
                null
            }
            
            if (existingRecord != null) {
                android.util.Log.e("MarkAttendanceUseCase", "Attendance already marked in Firestore")
                return AttendanceResult.Error("Attendance already marked for this event")
            }
            
            // FOR DEMO/TESTING: Skip location and geofence checks
            // In production, you would uncomment the location validation below:
            /*
            // 5. Get current location
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation == null) {
                return AttendanceResult.Error("Unable to get current location. Please ensure location services are enabled and try again.")
            }
            
            // 6. Check geofence
            val isWithinGeofence = geofenceManager.isWithinGeofence(eventId, currentLocation)
            if (!isWithinGeofence) {
                return AttendanceResult.Error("You are not within the event location")
            }
            */
            
            // 7. Determine attendance status and penalty
            val (status, penalty) = determineAttendanceStatus(event, currentTime)
            
            // 8. Create attendance record (with demo location)
            val attendanceRecordId = UUID.randomUUID().toString()
            android.util.Log.d("MarkAttendanceUseCase", "Creating attendance record with ID: '$attendanceRecordId'")
            val attendanceRecord = AttendanceRecord(
                id = attendanceRecordId,
                studentId = studentId,
                eventId = eventId,
                timestamp = currentTime,
                status = status,
                penalty = penalty,
                latitude = event.latitude, // Use event location for demo
                longitude = event.longitude, // Use event location for demo
                synced = true // Mark as synced since we're creating directly in Firestore
            )
            
            // 9. Save attendance record directly to Firestore
            android.util.Log.d("MarkAttendanceUseCase", "Saving attendance record to Firestore...")
            val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
            val success = firestoreService.createAttendanceRecord(attendanceRecord)
            
            if (!success) {
                android.util.Log.e("MarkAttendanceUseCase", "Failed to save attendance record to Firestore")
                return AttendanceResult.Error("Failed to save attendance record to Firestore. Please try again.")
            }
            
            // 10. Now save to local database for caching/offline access
            try {
                attendanceRepository.insertAttendanceRecord(attendanceRecord)
                android.util.Log.d("MarkAttendanceUseCase", "Attendance record cached locally")
            } catch (e: Exception) {
                android.util.Log.w("MarkAttendanceUseCase", "Could not cache attendance record locally: ${e.message}", e)
                // Continue anyway since Firestore save was successful
            }
            
            android.util.Log.d("MarkAttendanceUseCase", "Attendance marked successfully")
            return AttendanceResult.Success
            
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Handle foreign key constraint violations specifically
            android.util.Log.e("MarkAttendanceUseCase", "SQLite constraint error: ${e.message}", e)
            return AttendanceResult.Error("Database constraint error: Please ensure you are a registered student and the event exists. Contact administrator if this persists.")
        } catch (e: Exception) {
            android.util.Log.e("MarkAttendanceUseCase", "Failed to mark attendance: ${e.message}", e)
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