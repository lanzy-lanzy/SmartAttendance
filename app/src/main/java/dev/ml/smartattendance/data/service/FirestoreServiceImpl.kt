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
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUser(uid: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject<User>()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(users)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }

    // Student operations
    override suspend fun createStudent(student: Student): Boolean {
        return try {
            firestore.collection(STUDENTS_COLLECTION)
                .document(student.id)
                .set(student)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getStudent(studentId: String): Student? {
        return try {
            val document = firestore.collection(STUDENTS_COLLECTION)
                .document(studentId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject<Student>()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateStudent(student: Student): Boolean {
        return try {
            firestore.collection(STUDENTS_COLLECTION)
                .document(student.id)
                .set(student)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteStudent(studentId: String): Boolean {
        return try {
            firestore.collection(STUDENTS_COLLECTION)
                .document(studentId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllStudents(): List<Student> {
        return try {
            val snapshot = firestore.collection(STUDENTS_COLLECTION)
                .get()
                .await()
            
            snapshot.toObjects(Student::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getStudentsFlow(): Flow<List<Student>> = callbackFlow {
        val listener = firestore.collection(STUDENTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val students = snapshot.toObjects(Student::class.java)
                    trySend(students)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }

    // Event operations
    override suspend fun createEvent(event: Event): Boolean {
        return try {
            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(event)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getEvent(eventId: String): Event? {
        return try {
            val document = firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject<Event>()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateEvent(event: Event): Boolean {
        return try {
            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(event)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteEvent(eventId: String): Boolean {
        return try {
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllEvents(): List<Event> {
        return try {
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .get()
                .await()
            
            snapshot.toObjects(Event::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getActiveEvents(): List<Event> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .whereGreaterThanOrEqualTo("startTime", currentTime - (24 * 60 * 60 * 1000)) // Events from last 24 hours
                .whereLessThanOrEqualTo("endTime", currentTime + (7 * 24 * 60 * 60 * 1000)) // Up to next 7 days
                .get()
                .await()
            
            snapshot.toObjects(Event::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        val listener = firestore.collection(EVENTS_COLLECTION)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(events)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
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
}