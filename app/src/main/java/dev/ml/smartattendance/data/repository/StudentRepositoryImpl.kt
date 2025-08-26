package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.StudentDao
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val firestoreService: FirestoreService
) : StudentRepository {
    
    override fun getAllActiveStudents(): Flow<List<Student>> {
        // Firebase-first approach: Get active students from Firebase with local fallback
        return firestoreService.getStudentsFlow()
            .map { firebaseStudents ->
                val activeStudents = firebaseStudents.filter { it.isActive }
                if (activeStudents.isNotEmpty()) {
                    // Cache Firebase data locally
                    studentDao.insertStudents(firebaseStudents)
                    activeStudents
                } else {
                    // Fallback to local data only if Firebase returns empty list
                    studentDao.getAllActiveStudents().first()
                }
            }
            .catch { e ->
                // If Firebase throws an error, fallback to local
                emit(studentDao.getAllActiveStudents().first())
            }
    }
    
    override suspend fun getStudentById(studentId: String): Student? {
        // Try Firebase first
        try {
            val firebaseStudent = firestoreService.getStudent(studentId)
            if (firebaseStudent != null) {
                // Cache the student locally for future use
                studentDao.insertStudent(firebaseStudent)
                return firebaseStudent
            }
        } catch (e: Exception) {
            // If Firebase fails, fall back to local cache
        }
        
        // Only try local if Firebase fails or returns null
        return studentDao.getStudentById(studentId)
    }
    
    override suspend fun getStudentsByCourse(course: String): List<Student> {
        // Get all students from Firebase and filter by course
        return try {
            val allStudents = firestoreService.getAllStudents()
            allStudents.filter { it.course == course && it.isActive }
        } catch (e: Exception) {
            studentDao.getStudentsByCourse(course)
        }
    }
    
    override suspend fun getAllCourses(): List<String> {
        // Get distinct courses from Firebase
        return try {
            val allStudents = firestoreService.getAllStudents()
            allStudents.filter { it.isActive }.map { it.course }.distinct().sorted()
        } catch (e: Exception) {
            studentDao.getAllCourses()
        }
    }
    
    override suspend fun insertStudent(student: Student) {
        try {
            // Save to Firebase first
            val success = firestoreService.createStudent(student)
            if (success) {
                // Cache locally for offline access
                studentDao.insertStudent(student)
            } else {
                throw Exception("Failed to save student to Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            studentDao.insertStudent(student)
            throw e
        }
    }
    
    override suspend fun insertStudents(students: List<Student>) {
        // Insert students one by one to Firebase with local caching
        students.forEach { student ->
            try {
                val success = firestoreService.createStudent(student)
                if (success) {
                    studentDao.insertStudent(student)
                }
            } catch (e: Exception) {
                // Continue with other students
            }
        }
    }
    
    override suspend fun updateStudent(student: Student) {
        try {
            // Update in Firebase first
            val success = firestoreService.updateStudent(student)
            if (success) {
                // Update local cache
                studentDao.updateStudent(student)
            } else {
                throw Exception("Failed to update student in Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            studentDao.updateStudent(student)
            throw e
        }
    }
    
    override suspend fun deleteStudent(student: Student) {
        try {
            // Delete from Firebase with cascade (includes attendance records)
            val success = firestoreService.deleteStudentWithCascade(student.id)
            if (success) {
                // Delete from local cache
                studentDao.deleteStudent(student)
            } else {
                throw Exception("Failed to delete student from Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            studentDao.deleteStudent(student)
            throw e
        }
    }
    
    override suspend fun deactivateStudent(studentId: String) {
        try {
            // Get student, update status, and save
            val student = getStudentById(studentId)
            if (student != null) {
                val updatedStudent = student.copy(isActive = false)
                updateStudent(updatedStudent)
            }
        } catch (e: Exception) {
            // Fallback to local only
            studentDao.deactivateStudent(studentId)
            throw e
        }
    }
    
    override suspend fun getActiveStudentCount(): Int {
        return try {
            val allStudents = firestoreService.getAllStudents()
            allStudents.count { it.isActive }
        } catch (e: Exception) {
            studentDao.getActiveStudentCount()
        }
    }
}