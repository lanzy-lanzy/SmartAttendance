package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.auth.User
import kotlinx.coroutines.flow.Flow

interface FirestoreService {
    // User operations
    suspend fun createUser(user: User): Boolean
    suspend fun getUser(uid: String): User?
    suspend fun updateUser(user: User): Boolean
    suspend fun getAllUsers(): List<User>
    fun getUsersFlow(): Flow<List<User>>
    
    // Student operations
    suspend fun createStudent(student: Student): Boolean
    suspend fun getStudent(studentId: String): Student?
    suspend fun updateStudent(student: Student): Boolean
    suspend fun deleteStudent(studentId: String): Boolean
    suspend fun getAllStudents(): List<Student>
    fun getStudentsFlow(): Flow<List<Student>>
    
    // Event operations
    suspend fun createEvent(event: Event): Boolean
    suspend fun getEvent(eventId: String): Event?
    suspend fun updateEvent(event: Event): Boolean
    suspend fun deleteEvent(eventId: String): Boolean
    suspend fun getAllEvents(): List<Event>
    suspend fun getActiveEvents(): List<Event>
    fun getEventsFlow(): Flow<List<Event>>
    
    // Attendance operations
    suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean
    suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord?
    suspend fun updateAttendanceRecord(record: AttendanceRecord): Boolean
    suspend fun deleteAttendanceRecord(recordId: String): Boolean
    suspend fun getAttendanceByStudent(studentId: String): List<AttendanceRecord>
    suspend fun getAttendanceByEvent(eventId: String): List<AttendanceRecord>
    suspend fun getAllAttendanceRecords(): List<AttendanceRecord>
    fun getAttendanceFlow(): Flow<List<AttendanceRecord>>
    
    // Batch operations
    suspend fun batchSyncData(): Boolean
    
    // Cascade delete operations
    suspend fun deleteStudentWithCascade(studentId: String): Boolean
    suspend fun deleteEventWithCascade(eventId: String): Boolean
}