package dev.ml.smartattendance

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.ml.smartattendance.data.database.SmartAttendanceDatabase
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
                    
                    // Add sample student
                    val sampleStudent = Student(
                        id = "STUDENT001",
                        name = "John Doe",
                        course = "Computer Science",
                        enrollmentDate = currentTime
                    )
                    
                    database.studentDao().insertStudent(sampleStudent)
                }
            } catch (e: Exception) {
                // Ignore errors during sample data initialization
            }
        }
    }
}