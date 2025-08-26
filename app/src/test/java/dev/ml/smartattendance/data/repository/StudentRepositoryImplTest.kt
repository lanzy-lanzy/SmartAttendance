package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.StudentDao
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudentRepositoryImplTest {
    
    @Mock
    private lateinit var studentDao: StudentDao
    
    @Mock
    private lateinit var firestoreService: FirestoreService
    
    private lateinit var studentRepository: StudentRepositoryImpl
    
    private val studentId = "STUDENT001"
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        studentRepository = StudentRepositoryImpl(studentDao, firestoreService)
    }
    
    @Test
    fun `getStudentById should return student from local cache when available`() = runTest {
        // Given
        val localStudent = createMockStudent()
        whenever(studentDao.getStudentById(studentId)).thenReturn(localStudent)
        
        // When
        val result = studentRepository.getStudentById(studentId)
        
        // Then
        assertEquals(localStudent, result)
        verify(studentDao).getStudentById(studentId)
        verify(firestoreService, never()).getStudent(studentId)
    }
    
    @Test
    fun `getStudentById should return student from Firebase when not in local cache`() = runTest {
        // Given
        val firebaseStudent = createMockStudent()
        whenever(studentDao.getStudentById(studentId)).thenReturn(null)
        whenever(firestoreService.getStudent(studentId)).thenReturn(firebaseStudent)
        
        // When
        val result = studentRepository.getStudentById(studentId)
        
        // Then
        assertEquals(firebaseStudent, result)
        verify(studentDao).getStudentById(studentId)
        verify(firestoreService).getStudent(studentId)
        verify(studentDao).insertStudent(firebaseStudent)
    }
    
    @Test
    fun `getStudentById should return null when student not found anywhere`() = runTest {
        // Given
        whenever(studentDao.getStudentById(studentId)).thenReturn(null)
        whenever(firestoreService.getStudent(studentId)).thenReturn(null)
        
        // When
        val result = studentRepository.getStudentById(studentId)
        
        // Then
        assertNull(result)
        verify(studentDao).getStudentById(studentId)
        verify(firestoreService).getStudent(studentId)
        verify(studentDao, never()).insertStudent(any())
    }
    
    private fun createMockStudent(): Student {
        return Student(
            id = studentId,
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            course = "CS101",
            yearLevel = 1,
            studentIdNumber = "2023-0001",
            isActive = true
        )
    }
    
    // Helper function for Mockito any() matcher
    private inline fun <reified T> any(): T = org.mockito.kotlin.any()
}