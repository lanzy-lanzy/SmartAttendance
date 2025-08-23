package dev.ml.smartattendance.data.dao

import androidx.room.*
import dev.ml.smartattendance.data.entity.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    
    @Query("SELECT * FROM students WHERE isActive = 1")
    fun getAllActiveStudents(): Flow<List<Student>>
    
    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): Student?
    
    @Query("SELECT * FROM students WHERE course = :course AND isActive = 1")
    suspend fun getStudentsByCourse(course: String): List<Student>
    
    @Query("SELECT DISTINCT course FROM students WHERE isActive = 1")
    suspend fun getAllCourses(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)
    
    @Update
    suspend fun updateStudent(student: Student)
    
    @Delete
    suspend fun deleteStudent(student: Student)
    
    @Query("UPDATE students SET isActive = 0 WHERE id = :studentId")
    suspend fun deactivateStudent(studentId: String)
    
    @Query("SELECT COUNT(*) FROM students WHERE isActive = 1")
    suspend fun getActiveStudentCount(): Int
}