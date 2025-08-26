package dev.ml.smartattendance.data.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.auth.User
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreService {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val STUDENTS_COLLECTION = "students"
        private const val EVENTS_COLLECTION = "events"
        private const val ATTENDANCE_COLLECTION = "attendance_records"
    }

    // User operations
    override suspend fun createUser(user: User): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Creating user: ${user.uid}")
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()
            android.util.Log.d("FirestoreService", "Successfully created user: ${user.uid}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error creating user ${user.uid}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getUser(uid: String): User? {
        return try {
            android.util.Log.d("FirestoreService", "Fetching user: $uid")
            val document = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                val user = document.toObject<User>()
                android.util.Log.d("FirestoreService", "Successfully fetched user: $uid")
                user
            } else {
                android.util.Log.w("FirestoreService", "User not found: $uid")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching user $uid: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Updating user: ${user.uid}")
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()
            android.util.Log.d("FirestoreService", "Successfully updated user: ${user.uid}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error updating user ${user.uid}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            android.util.Log.d("FirestoreService", "Fetching all users...")
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            android.util.Log.d("FirestoreService", "Successfully fetched ${snapshot.size()} user documents")
            val users = snapshot.toObjects(User::class.java)
            android.util.Log.d("FirestoreService", "Successfully converted to ${users.size} User objects")
            users
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching all users: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getUsersFlow(): Flow<List<User>> = callbackFlow {
        android.util.Log.d("FirestoreService", "Setting up users flow listener...")
        val listener = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Error in users flow listener: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        android.util.Log.d("FirestoreService", "Received ${snapshot.size()} user documents in flow")
                        val users = snapshot.toObjects(User::class.java)
                        android.util.Log.d("FirestoreService", "Successfully converted to ${users.size} User objects in flow")
                        trySend(users)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreService", "Error converting user documents in flow: ${e.message}", e)
                        e.printStackTrace()
                        trySend(emptyList())
                    }
                } else {
                    android.util.Log.w("FirestoreService", "Users snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            android.util.Log.d("FirestoreService", "Removing users flow listener")
            listener.remove()
        }
    }

    // Student operations
    override suspend fun createStudent(student: Student): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Creating student: ${student.id}")
            firestore.collection(STUDENTS_COLLECTION)
                .document(student.id)
                .set(student)
                .await()
            android.util.Log.d("FirestoreService", "Successfully created student: ${student.id}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error creating student ${student.id}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getStudent(studentId: String): Student? {
        return try {
            android.util.Log.d("FirestoreService", "Fetching student: $studentId")
            val document = firestore.collection(STUDENTS_COLLECTION)
                .document(studentId)
                .get()
                .await()
            
            if (document.exists()) {
                val student = document.toObject<Student>()
                android.util.Log.d("FirestoreService", "Successfully fetched student: $studentId")
                student
            } else {
                android.util.Log.w("FirestoreService", "Student not found: $studentId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching student $studentId: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateStudent(student: Student): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Updating student: ${student.id}")
            firestore.collection(STUDENTS_COLLECTION)
                .document(student.id)
                .set(student)
                .await()
            android.util.Log.d("FirestoreService", "Successfully updated student: ${student.id}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error updating student ${student.id}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteStudent(studentId: String): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Deleting student: $studentId")
            firestore.collection(STUDENTS_COLLECTION)
                .document(studentId)
                .delete()
                .await()
            android.util.Log.d("FirestoreService", "Successfully deleted student: $studentId")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error deleting student $studentId: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAllStudents(): List<Student> {
        return try {
            android.util.Log.d("FirestoreService", "Fetching all students...")
            val snapshot = firestore.collection(STUDENTS_COLLECTION)
                .get()
                .await()
            
            android.util.Log.d("FirestoreService", "Successfully fetched ${snapshot.size()} student documents")
            val students = snapshot.toObjects(Student::class.java)
            android.util.Log.d("FirestoreService", "Successfully converted to ${students.size} Student objects")
            students
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching all students: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getStudentsFlow(): Flow<List<Student>> = callbackFlow {
        android.util.Log.d("FirestoreService", "Setting up students flow listener...")
        val listener = firestore.collection(STUDENTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Error in students flow listener: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        android.util.Log.d("FirestoreService", "Received ${snapshot.size()} student documents in flow")
                        val students = snapshot.toObjects(Student::class.java)
                        android.util.Log.d("FirestoreService", "Successfully converted to ${students.size} Student objects in flow")
                        trySend(students)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreService", "Error converting student documents in flow: ${e.message}", e)
                        e.printStackTrace()
                        trySend(emptyList())
                    }
                } else {
                    android.util.Log.w("FirestoreService", "Students snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            android.util.Log.d("FirestoreService", "Removing students flow listener")
            listener.remove()
        }
    }

    // Event operations
    override suspend fun createEvent(event: Event): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Creating event: ${event.id}")
            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(event)
                .await()
            android.util.Log.d("FirestoreService", "Successfully created event: ${event.id}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error creating event ${event.id}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getEvent(eventId: String): Event? {
        return try {
            android.util.Log.d("FirestoreService", "Fetching event with ID: '$eventId'")
            
            if (eventId.isBlank()) {
                android.util.Log.e("FirestoreService", "Invalid event ID: ID is blank")
                return null
            }
            
            // Ensure the ID is properly trimmed to avoid any whitespace issues
            val cleanId = eventId.trim()
            android.util.Log.d("FirestoreService", "Starting Firestore getEvent() call for ID: '$cleanId'")
            
            val document = firestore.collection(EVENTS_COLLECTION)
                .document(cleanId)
                .get()
                .await()
            
            android.util.Log.d("FirestoreService", "Firestore getEvent() returned document exists: ${document.exists()} for ID: '$cleanId'")
            if (document.exists()) {
                android.util.Log.d("FirestoreService", "Document data keys: ${document.data?.keys}")
            }
            
            if (document.exists()) {
                try {
                    // First try simple toObject conversion
                    val event = document.toObject<Event>()
                    if (event != null) {
                        // ALWAYS ensure event ID is set correctly from the document ID
                        val finalEvent = event.copy(id = cleanId)
                        android.util.Log.d("FirestoreService", "Successfully fetched and parsed event: '${finalEvent.name}' with ID: '$cleanId'")
                        return finalEvent
                    } else {
                        // If conversion fails, try manual conversion
                        android.util.Log.w("FirestoreService", "Failed to parse event automatically, trying manual conversion")
                        try {
                            val data = document.data
                            if (data != null) {
                                val event = Event(
                                    id = cleanId, // Use document ID directly
                                    name = data["name"] as? String ?: "Unknown Event",
                                    startTime = (data["startTime"] as? Long) ?: System.currentTimeMillis(),
                                    endTime = (data["endTime"] as? Long) ?: (System.currentTimeMillis() + 3600000),
                                    latitude = (data["latitude"] as? Double) ?: 0.0,
                                    longitude = (data["longitude"] as? Double) ?: 0.0,
                                    geofenceRadius = (data["geofenceRadius"] as? Number)?.toFloat() ?: 100f,
                                    isActive = (data["isActive"] as? Boolean) ?: true,
                                    signInStartOffset = (data["signInStartOffset"] as? Long) ?: 15,
                                    signInEndOffset = (data["signInEndOffset"] as? Long) ?: 15,
                                    signOutStartOffset = (data["signOutStartOffset"] as? Long) ?: 15,
                                    signOutEndOffset = (data["signOutEndOffset"] as? Long) ?: 15
                                )
                                android.util.Log.d("FirestoreService", "Successfully created event from manual conversion: '${event.name}' with ID: '$cleanId'")
                                return event
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreService", "Error in manual conversion: ${e.message}", e)
                        }
                        android.util.Log.e("FirestoreService", "Failed to parse event data for ID: '$cleanId'")
                        return null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreService", "Error converting document to Event object for ID '$cleanId': ${e.message}", e)
                    e.printStackTrace()
                    // Try one last desperate fallback attempt
                    try {
                        // Create a minimal event from just the document ID
                        val fallbackEvent = Event(
                            id = cleanId,
                            name = "Event $cleanId",
                            startTime = System.currentTimeMillis(),
                            endTime = System.currentTimeMillis() + 3600000,
                            latitude = 0.0,
                            longitude = 0.0,
                            geofenceRadius = 100f,
                            isActive = true,
                            signInStartOffset = 15,
                            signInEndOffset = 15,
                            signOutStartOffset = 15,
                            signOutEndOffset = 15
                        )
                        android.util.Log.d("FirestoreService", "Created fallback event as last resort: '$cleanId'")
                        return fallbackEvent
                    } catch (fallbackEx: Exception) {
                        android.util.Log.e("FirestoreService", "Even fallback event creation failed: ${fallbackEx.message}", fallbackEx)
                    }
                    return null
                }
            } else {
                // Try retrieving by querying the collection for matching ID
                android.util.Log.w("FirestoreService", "Event not found by direct ID lookup, trying query: '$cleanId'")
                try {
                    val snapshot = firestore.collection(EVENTS_COLLECTION)
                        .whereEqualTo("id", cleanId)
                        .get()
                        .await()
                    
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val event = doc.toObject<Event>()
                        if (event != null) {
                            android.util.Log.d("FirestoreService", "Found event via query: ${event.name}")
                            return event
                        }
                    }
                    
                    android.util.Log.w("FirestoreService", "Event not found via query either")
                } catch (queryEx: Exception) {
                    android.util.Log.e("FirestoreService", "Error in query attempt: ${queryEx.message}", queryEx)
                }
                
                return null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching event '$eventId': ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }

    override suspend fun updateEvent(event: Event): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Updating event: ${event.id}")
            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(event)
                .await()
            android.util.Log.d("FirestoreService", "Successfully updated event: ${event.id}")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error updating event ${event.id}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteEvent(eventId: String): Boolean {
        return try {
            android.util.Log.d("FirestoreService", "Deleting event: $eventId")
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()
            android.util.Log.d("FirestoreService", "Successfully deleted event: $eventId")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error deleting event $eventId: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAllEvents(): List<Event> {
        return try {
            android.util.Log.d("FirestoreService", "Starting to fetch all events from Firebase...")
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .get()
                .await()
            
            android.util.Log.d("FirestoreService", "Successfully fetched ${snapshot.size()} event documents")
            val events = snapshot.toObjects(Event::class.java)
            android.util.Log.d("FirestoreService", "Successfully converted to ${events.size} Event objects")
            events
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching all events: ${e.message}", e)
            e.printStackTrace()
            // Return empty list as fallback but log the error for debugging
            emptyList()
        }
    }

    override suspend fun getActiveEvents(): List<Event> {
        return try {
            android.util.Log.d("FirestoreService", "Starting to fetch active events from Firebase...")
            val currentTime = System.currentTimeMillis()
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .whereGreaterThanOrEqualTo("startTime", currentTime - (24 * 60 * 60 * 1000)) // Events from last 24 hours
                .whereLessThanOrEqualTo("endTime", currentTime + (7 * 24 * 60 * 60 * 1000)) // Up to next 7 days
                .get()
                .await()
            
            android.util.Log.d("FirestoreService", "Successfully fetched ${snapshot.size()} active event documents")
            val events = snapshot.toObjects(Event::class.java)
            android.util.Log.d("FirestoreService", "Successfully converted to ${events.size} active Event objects")
            events
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error fetching active events: ${e.message}", e)
            e.printStackTrace()
            // Return empty list as fallback but log the error for debugging
            emptyList()
        }
    }

    override fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        android.util.Log.d("FirestoreService", "Setting up events flow listener...")
        val listener = firestore.collection(EVENTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Error in events flow listener: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        android.util.Log.d("FirestoreService", "Received ${snapshot.size()} event documents in flow")
                        val events = snapshot.toObjects(Event::class.java)
                        android.util.Log.d("FirestoreService", "Successfully converted to ${events.size} Event objects in flow")
                        trySend(events)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreService", "Error converting event documents in flow: ${e.message}", e)
                        e.printStackTrace()
                        trySend(emptyList())
                    }
                } else {
                    android.util.Log.w("FirestoreService", "Events snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            android.util.Log.d("FirestoreService", "Removing events flow listener")
            listener.remove()
        }
    }

    // Attendance operations
    override suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean {
        return try {
            firestore.collection(ATTENDANCE_COLLECTION)
                .document(record.id)
                .set(record)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord? {
        return try {
            val snapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                snapshot.toObjects(AttendanceRecord::class.java).firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateAttendanceRecord(record: AttendanceRecord): Boolean {
        return try {
            firestore.collection(ATTENDANCE_COLLECTION)
                .document(record.id)
                .set(record)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAttendanceRecord(recordId: String): Boolean {
        return try {
            firestore.collection(ATTENDANCE_COLLECTION)
                .document(recordId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAttendanceByStudent(studentId: String): List<AttendanceRecord> {
        return try {
            val snapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            
            snapshot.toObjects(AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAttendanceByEvent(eventId: String): List<AttendanceRecord> {
        return try {
            val snapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
            
            snapshot.toObjects(AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllAttendanceRecords(): List<AttendanceRecord> {
        return try {
            val snapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .get()
                .await()
            
            snapshot.toObjects(AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getAttendanceFlow(): Flow<List<AttendanceRecord>> = callbackFlow {
        val listener = firestore.collection(ATTENDANCE_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val records = snapshot.toObjects(AttendanceRecord::class.java)
                    trySend(records)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }

    override suspend fun batchSyncData(): Boolean {
        // Implementation for batch data synchronization
        return true
    }
    
    override suspend fun deleteStudentWithCascade(studentId: String): Boolean {
        return try {
            // Start a batch operation
            val batch = firestore.batch()
            
            // Delete the student
            val studentRef = firestore.collection(STUDENTS_COLLECTION).document(studentId)
            batch.delete(studentRef)
            
            // Delete all attendance records for this student
            val attendanceSnapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            
            for (document in attendanceSnapshot.documents) {
                batch.delete(document.reference)
            }
            
            // Commit the batch
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deleteEventWithCascade(eventId: String): Boolean {
        return try {
            // Start a batch operation
            val batch = firestore.batch()
            
            // Delete the event
            val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
            batch.delete(eventRef)
            
            // Delete all attendance records for this event
            val attendanceSnapshot = firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
            
            for (document in attendanceSnapshot.documents) {
                batch.delete(document.reference)
            }
            
            // Commit the batch
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}