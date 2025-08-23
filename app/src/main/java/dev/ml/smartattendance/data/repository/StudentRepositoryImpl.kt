package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.StudentDao
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao
) : StudentRepository {
    
    override fun getAllActiveStudents(): Flow<List<Student>> = studentDao.getAllActiveStudents()
    
    override suspend fun getStudentById(studentId: String): Student? = studentDao.getStudentById(studentId)
    
    override suspend fun getStudentsByCourse(course: String): List<Student> = studentDao.getStudentsByCourse(course)
    
    override suspend fun getAllCourses(): List<String> = studentDao.getAllCourses()
    
    override suspend fun insertStudent(student: Student) = studentDao.insertStudent(student)
    
    override suspend fun insertStudents(students: List<Student>) = studentDao.insertStudents(students)
    
    override suspend fun updateStudent(student: Student) = studentDao.updateStudent(student)
    
    override suspend fun deleteStudent(student: Student) = studentDao.deleteStudent(student)
    
    override suspend fun deactivateStudent(studentId: String) = studentDao.deactivateStudent(studentId)
    
    override suspend fun getActiveStudentCount(): Int = studentDao.getActiveStudentCount()
}