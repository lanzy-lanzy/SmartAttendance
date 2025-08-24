package dev.ml.smartattendance

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.ml.smartattendance.data.database.SmartAttendanceDatabase
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.service.GeofenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartAttendanceApplication : Application() {
    
    @Inject
    lateinit var database: SmartAttendanceDatabase
    
    @Inject
    lateinit var geofenceManager: GeofenceManager
    
    override fun onCreate() {
        super.onCreate()
        initializeSampleData()
    }
    
    private fun initializeSampleData() {
        GlobalScope.launch {
            try {
                // Check if data already exists
                val eventCount = database.eventDao().getActiveEventCount()
                if (eventCount == 0) {
                    // Add sample events
                    val currentTime = System.currentTimeMillis()
                    val sampleEvents = listOf(
                        Event(
                            id = "EVENT001",
                            name = "Computer Science Lecture",
                            startTime = currentTime + (30 * 60 * 1000), // 30 minutes from now
                            endTime = currentTime + (90 * 60 * 1000), // 90 minutes from now
                            latitude = 37.7749, // Same as demo location
                            longitude = -122.4194, // Same as demo location
                            geofenceRadius = 1000.0f, // Large radius for testing
                            signInStartOffset = 15L, // 15 minutes before
                            signInEndOffset = 10L, // 10 minutes after start
                            signOutStartOffset = 5L, // 5 minutes before end
                            signOutEndOffset = 15L, // 15 minutes after end
                            isActive = true
                        ),
                        Event(
                            id = "EVENT002",
                            name = "Mobile Development Workshop",
                            startTime = currentTime + (120 * 60 * 1000), // 2 hours from now
                            endTime = currentTime + (180 * 60 * 1000), // 3 hours from now
                            latitude = 37.7749, // Same as demo location
                            longitude = -122.4194, // Same as demo location
                            geofenceRadius = 500.0f, // Large radius for testing
                            signInStartOffset = 10L,
                            signInEndOffset = 5L,
                            signOutStartOffset = 5L,
                            signOutEndOffset = 10L,
                            isActive = true
                        ),
                        Event(
                            id = "EVENT003",
                            name = "Database Systems Seminar",
                            startTime = currentTime - (30 * 60 * 1000), // 30 minutes ago (current event)
                            endTime = currentTime + (30 * 60 * 1000), // 30 minutes from now
                            latitude = 37.7749, // Same as demo location
                            longitude = -122.4194, // Same as demo location
                            geofenceRadius = 750.0f, // Large radius for testing
                            signInStartOffset = 20L,
                            signInEndOffset = 15L,
                            signOutStartOffset = 10L,
                            signOutEndOffset = 20L,
                            isActive = true
                        )
                    )
                    
                    database.eventDao().insertEvents(sampleEvents)
                    
                    // Create geofences for all sample events
                    sampleEvents.forEach { event ->
                        geofenceManager.createGeofence(
                            eventId = event.id,
                            location = LatLng(event.latitude, event.longitude),
                            radius = event.geofenceRadius
                        )
                    }
                    
                    // Add sample students
                    val sampleStudents = listOf(
                        Student(
                            id = "STUDENT001",
                            name = "John Doe",
                            course = "Computer Science",
                            enrollmentDate = currentTime
                        ),
                        Student(
                            id = "STUDENT002",
                            name = "Jane Smith",
                            course = "Computer Science",
                            enrollmentDate = currentTime
                        ),
                        Student(
                            id = "STUDENT003",
                            name = "Mike Johnson",
                            course = "Software Engineering",
                            enrollmentDate = currentTime
                        ),
                        Student(
                            id = "STUDENT004",
                            name = "Sarah Wilson",
                            course = "Data Science",
                            enrollmentDate = currentTime
                        ),
                        Student(
                            id = "STUDENT005",
                            name = "David Brown",
                            course = "Computer Science",
                            enrollmentDate = currentTime
                        ),
                        Student(
                            id = "STUDENT006",
                            name = "Emily Davis",
                            course = "Software Engineering",
                            enrollmentDate = currentTime
                        )
                    )
                    
                    database.studentDao().insertStudents(sampleStudents)
                    
                    // Add sample attendance records for demonstration
                    val sampleAttendanceRecords = listOf(
                        AttendanceRecord(
                            id = "ATTENDANCE001",
                            studentId = "STUDENT001",
                            eventId = "EVENT001",
                            timestamp = currentTime - (24 * 60 * 60 * 1000), // Yesterday
                            status = dev.ml.smartattendance.domain.model.AttendanceStatus.PRESENT,
                            penalty = null,
                            latitude = 37.7749,
                            longitude = -122.4194,
                            synced = true,
                            notes = "On time"
                        ),
                        AttendanceRecord(
                            id = "ATTENDANCE002",
                            studentId = "STUDENT002",
                            eventId = "EVENT001",
                            timestamp = currentTime - (24 * 60 * 60 * 1000) + (10 * 60 * 1000), // Yesterday, 10 min late
                            status = dev.ml.smartattendance.domain.model.AttendanceStatus.LATE,
                            penalty = dev.ml.smartattendance.domain.model.PenaltyType.WARNING,
                            latitude = 37.7749,
                            longitude = -122.4194,
                            synced = true,
                            notes = "10 minutes late"
                        ),
                        AttendanceRecord(
                            id = "ATTENDANCE003",
                            studentId = "STUDENT003",
                            eventId = "EVENT002",
                            timestamp = currentTime - (2 * 60 * 60 * 1000), // 2 hours ago
                            status = dev.ml.smartattendance.domain.model.AttendanceStatus.PRESENT,
                            penalty = null,
                            latitude = 37.7749,
                            longitude = -122.4194,
                            synced = true,
                            notes = "Present"
                        ),
                        AttendanceRecord(
                            id = "ATTENDANCE004",
                            studentId = "STUDENT004",
                            eventId = "EVENT002",
                            timestamp = currentTime - (2 * 60 * 60 * 1000) + (20 * 60 * 1000), // 2 hours ago, 20 min late
                            status = dev.ml.smartattendance.domain.model.AttendanceStatus.LATE,
                            penalty = dev.ml.smartattendance.domain.model.PenaltyType.MINOR,
                            latitude = 37.7749,
                            longitude = -122.4194,
                            synced = true,
                            notes = "20 minutes late"
                        )
                    )
                    
                    database.attendanceRecordDao().insertAttendanceRecords(sampleAttendanceRecords)
                }
            } catch (e: Exception) {
                // Ignore errors during sample data initialization
            }
        }
    }
}