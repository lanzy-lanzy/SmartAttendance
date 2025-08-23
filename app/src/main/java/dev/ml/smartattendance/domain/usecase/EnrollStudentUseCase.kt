package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.repository.StudentRepository
import java.util.*
import javax.inject.Inject

class EnrollStudentUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) {
    
    sealed class EnrollmentResult {
        object Success : EnrollmentResult()
        data class Error(val message: String) : EnrollmentResult()
    }
    
    suspend fun execute(
        studentId: String,
        name: String,
        course: String
    ): EnrollmentResult {
        try {
            // Validate input
            if (studentId.isBlank()) {
                return EnrollmentResult.Error("Student ID cannot be empty")
            }
            
            if (name.isBlank()) {
                return EnrollmentResult.Error("Student name cannot be empty")
            }
            
            if (course.isBlank()) {
                return EnrollmentResult.Error("Course cannot be empty")
            }
            
            // Check if student already exists
            val existingStudent = studentRepository.getStudentById(studentId)
            if (existingStudent != null) {
                return EnrollmentResult.Error("Student with ID $studentId already exists")
            }
            
            // Create new student
            val student = Student(
                id = studentId,
                name = name.trim(),
                course = course.trim(),
                enrollmentDate = System.currentTimeMillis(),
                role = UserRole.STUDENT,
                isActive = true
            )
            
            // Save student
            studentRepository.insertStudent(student)
            
            return EnrollmentResult.Success
            
        } catch (e: Exception) {
            return EnrollmentResult.Error("Failed to enroll student: ${e.message}")
        }
    }
}