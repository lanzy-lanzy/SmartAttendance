package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.repository.StudentRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnrollStudentUseCaseTest {

    private lateinit var enrollStudentUseCase: EnrollStudentUseCase
    private val mockStudentRepository = mockk<StudentRepository>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        enrollStudentUseCase = EnrollStudentUseCase(mockStudentRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute should return Success when student is enrolled successfully`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "Computer Science"
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } just Runs

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Success)
        
        coVerify { mockStudentRepository.getStudentById(studentId) }
        coVerify { 
            mockStudentRepository.insertStudent(match { student ->
                student.id == studentId &&
                student.name == name &&
                student.course == course &&
                student.role == UserRole.STUDENT &&
                student.isActive == true
            })
        }
    }

    @Test
    fun `execute should return Error when student ID is blank`() = runTest {
        // Given
        val studentId = ""
        val name = "John Doe"
        val course = "Computer Science"

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Student ID cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
        
        coVerify(exactly = 0) { mockStudentRepository.getStudentById(any()) }
        coVerify(exactly = 0) { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should return Error when student ID is whitespace only`() = runTest {
        // Given
        val studentId = "   "
        val name = "John Doe"
        val course = "Computer Science"

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Student ID cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
    }

    @Test
    fun `execute should return Error when student name is blank`() = runTest {
        // Given
        val studentId = "S001"
        val name = ""
        val course = "Computer Science"

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Student name cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
        
        coVerify(exactly = 0) { mockStudentRepository.getStudentById(any()) }
        coVerify(exactly = 0) { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should return Error when student name is whitespace only`() = runTest {
        // Given
        val studentId = "S001"
        val name = "   "
        val course = "Computer Science"

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Student name cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
    }

    @Test
    fun `execute should return Error when course is blank`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = ""

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Course cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
        
        coVerify(exactly = 0) { mockStudentRepository.getStudentById(any()) }
        coVerify(exactly = 0) { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should return Error when course is whitespace only`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "   "

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Course cannot be empty", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
    }

    @Test
    fun `execute should return Error when student already exists`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "Computer Science"
        val existingStudent = Student(
            id = studentId,
            name = "Existing Student",
            course = "Math",
            enrollmentDate = System.currentTimeMillis(),
            role = UserRole.STUDENT,
            isActive = true
        )
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns existingStudent

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertEquals("Student with ID S001 already exists", (result as EnrollStudentUseCase.EnrollmentResult.Error).message)
        
        coVerify { mockStudentRepository.getStudentById(studentId) }
        coVerify(exactly = 0) { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should trim whitespace from name and course`() = runTest {
        // Given
        val studentId = "S001"
        val name = "  John Doe  "
        val course = "  Computer Science  "
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } just Runs

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Success)
        
        coVerify { 
            mockStudentRepository.insertStudent(match { student ->
                student.name == "John Doe" &&
                student.course == "Computer Science"
            })
        }
    }

    @Test
    fun `execute should set correct default values for new student`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "Computer Science"
        val currentTime = System.currentTimeMillis()
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } just Runs

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Success)
        
        coVerify { 
            mockStudentRepository.insertStudent(match { student ->
                student.id == studentId &&
                student.name == name &&
                student.course == course &&
                student.role == UserRole.STUDENT &&
                student.isActive == true &&
                student.enrollmentDate >= currentTime &&
                student.enrollmentDate <= System.currentTimeMillis()
            })
        }
    }

    @Test
    fun `execute should return Error when repository throws exception during getStudentById`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "Computer Science"
        
        coEvery { mockStudentRepository.getStudentById(studentId) } throws RuntimeException("Database error")

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertTrue((result as EnrollStudentUseCase.EnrollmentResult.Error).message.contains("Failed to enroll student"))
        assertTrue(result.message.contains("Database error"))
        
        coVerify { mockStudentRepository.getStudentById(studentId) }
        coVerify(exactly = 0) { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should return Error when repository throws exception during insertStudent`() = runTest {
        // Given
        val studentId = "S001"
        val name = "John Doe"
        val course = "Computer Science"
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } throws RuntimeException("Insert failed")

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Error)
        assertTrue((result as EnrollStudentUseCase.EnrollmentResult.Error).message.contains("Failed to enroll student"))
        assertTrue(result.message.contains("Insert failed"))
        
        coVerify { mockStudentRepository.getStudentById(studentId) }
        coVerify { mockStudentRepository.insertStudent(any()) }
    }

    @Test
    fun `execute should handle special characters in student data`() = runTest {
        // Given
        val studentId = "S-001/2023"
        val name = "José María O'Connor"
        val course = "Computer Science & Engineering"
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } just Runs

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Success)
        
        coVerify { 
            mockStudentRepository.insertStudent(match { student ->
                student.id == studentId &&
                student.name == name &&
                student.course == course
            })
        }
    }

    @Test
    fun `execute should handle very long valid inputs`() = runTest {
        // Given
        val studentId = "VERY_LONG_STUDENT_ID_WITH_MANY_CHARACTERS_123456789"
        val name = "A Very Long Student Name With Many Words And Characters That Exceeds Normal Length"
        val course = "A Very Long Course Name Including Multiple Specializations And Concentrations"
        
        coEvery { mockStudentRepository.getStudentById(studentId) } returns null
        coEvery { mockStudentRepository.insertStudent(any()) } just Runs

        // When
        val result = enrollStudentUseCase.execute(studentId, name, course)

        // Then
        assertTrue(result is EnrollStudentUseCase.EnrollmentResult.Success)
        
        coVerify { 
            mockStudentRepository.insertStudent(match { student ->
                student.id == studentId &&
                student.name == name &&
                student.course == course
            })
        }
    }
}