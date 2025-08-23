package dev.ml.smartattendance.domain.repository

import dev.ml.smartattendance.data.entity.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun getAllActiveStudents(): Flow<List<Student>>
    suspend fun getStudentById(studentId: String): Student?
    suspend fun getStudentsByCourse(course: String): List<Student>
    suspend fun getAllCourses(): List<String>
    suspend fun insertStudent(student: Student)
    suspend fun insertStudents(students: List<Student>)
    suspend fun updateStudent(student: Student)
    suspend fun deleteStudent(student: Student)
    suspend fun deactivateStudent(studentId: String)
    suspend fun getActiveStudentCount(): Int
}