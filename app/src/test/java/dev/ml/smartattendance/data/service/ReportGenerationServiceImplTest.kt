package dev.ml.smartattendance.data.service

import android.content.Context
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.service.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ReportGenerationServiceImplTest {

    private lateinit var reportService: ReportGenerationServiceImpl
    private val mockAttendanceRepository = mockk<AttendanceRepository>()
    private val mockStudentRepository = mockk<StudentRepository>()
    private val mockEventRepository = mockk<EventRepository>()
    private val mockContext = mockk<Context>()

    private val sampleDetailedRecords = listOf(
        DetailedAttendanceRecord(
            id = 1L,
            studentId = "S001",
            studentName = "John Doe",
            studentCourse = "Computer Science",
            eventId = 1L,
            eventName = "Morning Assembly",
            timestamp = 1698739200000L, // 2023-10-31 08:00:00
            status = AttendanceStatus.PRESENT,
            penalty = null,
            latitude = 40.7128,
            longitude = -74.0060,
            synced = true,
            notes = "On time"
        ),
        DetailedAttendanceRecord(
            id = 2L,
            studentId = "S002",
            studentName = "Jane Smith",
            studentCourse = "Computer Science",
            eventId = 1L,
            eventName = "Morning Assembly",
            timestamp = 1698739800000L, // 2023-10-31 08:10:00
            status = AttendanceStatus.LATE,
            penalty = PenaltyType.WARNING,
            latitude = 40.7128,
            longitude = -74.0060,
            synced = true,
            notes = "10 minutes late"
        ),
        DetailedAttendanceRecord(
            id = 3L,
            studentId = "S003",
            studentName = "Bob Johnson",
            studentCourse = "Mathematics",
            eventId = 2L,
            eventName = "Math Lecture",
            timestamp = 1698742800000L, // 2023-10-31 09:00:00
            status = AttendanceStatus.ABSENT,
            penalty = PenaltyType.DEDUCTION,
            latitude = null,
            longitude = null,
            synced = false,
            notes = "No show"
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock external files directory
        val mockExternalDir = mockk<File>()
        every { mockContext.getExternalFilesDir(null) } returns mockExternalDir
        every { mockContext.packageName } returns "dev.ml.smartattendance"
        
        reportService = ReportGenerationServiceImpl(
            attendanceRepository = mockAttendanceRepository,
            studentRepository = mockStudentRepository,
            eventRepository = mockEventRepository,
            context = mockContext
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getAttendanceData should return complete report with all records when no filter applied`() = runTest {
        // Given
        val filter = ReportFilter()
        val startDate = 0L
        val endDate = System.currentTimeMillis()
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(startDate, endDate)
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(3, result.records.size)
        assertEquals("Attendance Report", result.title.substring(0, "Attendance Report".length))
        assertTrue(result.generatedAt > 0)
        
        // Verify summary calculations
        assertEquals(3, result.summary.totalRecords)
        assertEquals(1, result.summary.presentCount)
        assertEquals(1, result.summary.lateCount)
        assertEquals(1, result.summary.absentCount)
        assertEquals(0, result.summary.excusedCount)
        assertEquals(3, result.summary.totalStudents)
        assertEquals(2, result.summary.totalEvents)
    }

    @Test
    fun `getAttendanceData should filter by course correctly`() = runTest {
        // Given
        val filter = ReportFilter(courseFilter = "Computer Science")
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(2, result.records.size)
        assertTrue(result.records.all { it.studentCourse.contains("Computer Science", ignoreCase = true) })
        
        // Verify filtered summary
        assertEquals(2, result.summary.totalRecords)
        assertEquals(1, result.summary.presentCount)
        assertEquals(1, result.summary.lateCount)
        assertEquals(0, result.summary.absentCount)
    }

    @Test
    fun `getAttendanceData should filter by status correctly`() = runTest {
        // Given
        val filter = ReportFilter(statusFilter = AttendanceStatus.LATE)
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(1, result.records.size)
        assertEquals(AttendanceStatus.LATE, result.records[0].status)
        assertEquals("Jane Smith", result.records[0].studentName)
    }

    @Test
    fun `getAttendanceData should filter by student ID correctly`() = runTest {
        // Given
        val filter = ReportFilter(studentIdFilter = "S001")
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(1, result.records.size)
        assertEquals("S001", result.records[0].studentId)
        assertEquals("John Doe", result.records[0].studentName)
    }

    @Test
    fun `getAttendanceData should filter by event ID correctly`() = runTest {
        // Given
        val filter = ReportFilter(eventIdFilter = 1L)
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(2, result.records.size)
        assertTrue(result.records.all { it.eventId == 1L })
        assertTrue(result.records.all { it.eventName == "Morning Assembly" })
    }

    @Test
    fun `getAttendanceData should apply multiple filters correctly`() = runTest {
        // Given
        val filter = ReportFilter(
            courseFilter = "Computer Science",
            statusFilter = AttendanceStatus.LATE
        )
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords

        // When
        val result = reportService.getAttendanceData(filter)

        // Then
        assertEquals(1, result.records.size)
        assertEquals("Jane Smith", result.records[0].studentName)
        assertEquals(AttendanceStatus.LATE, result.records[0].status)
        assertEquals("Computer Science", result.records[0].studentCourse)
    }

    @Test
    fun `exportToCSV should create file with correct content`() = runTest {
        // Given
        val mockFile = mockk<File>()
        val mockFileWriter = mockk<java.io.FileWriter>(relaxed = true)
        
        every { File(any<File>(), any<String>()) } returns mockFile
        mockkConstructor(java.io.FileWriter::class)
        every { anyConstructed<java.io.FileWriter>().use(any()) } answers {
            val block = firstArg<(java.io.FileWriter) -> Unit>()
            block(mockFileWriter)
        }
        
        val report = createSampleReport()

        // When
        val result = reportService.exportToCSV(report)

        // Then
        assertEquals(mockFile, result)
        
        // Verify CSV header was written
        verify { mockFileWriter.append(match { it.contains("Student ID,Student Name,Course") }) }
        
        // Verify data rows were written
        verify { mockFileWriter.append(match { it.contains("S001") && it.contains("John Doe") }) }
        verify { mockFileWriter.append(match { it.contains("S002") && it.contains("Jane Smith") }) }
        
        // Verify summary was written
        verify { mockFileWriter.append(match { it.contains("SUMMARY") }) }
        verify { mockFileWriter.append(match { it.contains("Total Records,3") }) }
    }

    @Test
    fun `exportToJSON should create file with correct JSON structure`() = runTest {
        // Given
        val mockFile = mockk<File>()
        val mockFileWriter = mockk<java.io.FileWriter>(relaxed = true)
        
        every { File(any<File>(), any<String>()) } returns mockFile
        mockkConstructor(java.io.FileWriter::class)
        every { anyConstructed<java.io.FileWriter>().use(any()) } answers {
            val block = firstArg<(java.io.FileWriter) -> Unit>()
            block(mockFileWriter)
        }
        
        val report = createSampleReport()

        // When
        val result = reportService.exportToJSON(report)

        // Then
        assertEquals(mockFile, result)
        
        // Verify JSON structure was written
        verify { mockFileWriter.write(match { json ->
            json.contains("\"title\"") && 
            json.contains("\"generatedAt\"") &&
            json.contains("\"summary\"") &&
            json.contains("\"records\"") &&
            json.contains("\"filter\"")
        }) }
    }

    @Test
    fun `generateAttendanceReport should return success for CSV format`() = runTest {
        // Given
        val filter = ReportFilter()
        val format = ReportFormat.CSV
        val mockFile = mockk<File>()
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords
        
        every { File(any<File>(), any<String>()) } returns mockFile
        mockkConstructor(java.io.FileWriter::class)
        every { anyConstructed<java.io.FileWriter>().use(any()) } just Runs

        // When
        val result = reportService.generateAttendanceReport(filter, format)

        // Then
        assertTrue(result is ReportResult.Success)
        assertEquals(mockFile, (result as ReportResult.Success).file)
    }

    @Test
    fun `generateAttendanceReport should return success for JSON format`() = runTest {
        // Given
        val filter = ReportFilter()
        val format = ReportFormat.JSON
        val mockFile = mockk<File>()
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords
        
        every { File(any<File>(), any<String>()) } returns mockFile
        mockkConstructor(java.io.FileWriter::class)
        every { anyConstructed<java.io.FileWriter>().use(any()) } just Runs

        // When
        val result = reportService.generateAttendanceReport(filter, format)

        // Then
        assertTrue(result is ReportResult.Success)
        assertEquals(mockFile, (result as ReportResult.Success).file)
    }

    @Test
    fun `generateAttendanceReport should fallback to CSV for PDF format`() = runTest {
        // Given
        val filter = ReportFilter()
        val format = ReportFormat.PDF // Should fallback to CSV
        val mockFile = mockk<File>()
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } returns sampleDetailedRecords
        
        every { File(any<File>(), any<String>()) } returns mockFile
        mockkConstructor(java.io.FileWriter::class)
        every { anyConstructed<java.io.FileWriter>().use(any()) } just Runs

        // When
        val result = reportService.generateAttendanceReport(filter, format)

        // Then
        assertTrue(result is ReportResult.Success)
        assertEquals(mockFile, (result as ReportResult.Success).file)
    }

    @Test
    fun `generateAttendanceReport should return error when exception occurs`() = runTest {
        // Given
        val filter = ReportFilter()
        val format = ReportFormat.CSV
        
        coEvery { 
            mockAttendanceRepository.getDetailedAttendanceInDateRange(any(), any())
        } throws RuntimeException("Database error")

        // When
        val result = reportService.generateAttendanceReport(filter, format)

        // Then
        assertTrue(result is ReportResult.Error)
        assertTrue((result as ReportResult.Error).message.contains("Failed to generate report"))
        assertTrue(result.message.contains("Database error"))
    }

    @Test
    fun `shareReport should create proper sharing intent`() {
        // Given
        val mockFile = mockk<File>()
        val mockContext = mockk<Context>(relaxed = true)
        
        every { mockFile.extension } returns "csv"
        
        mockkStatic("androidx.core.content.FileProvider")
        every { 
            androidx.core.content.FileProvider.getUriForFile(any(), any(), any()) 
        } returns mockk()

        // When
        reportService.shareReport(mockFile, mockContext)

        // Then
        verify { mockContext.startActivity(any()) }
    }

    private fun createSampleReport(): AttendanceReport {
        return AttendanceReport(
            title = "Test Report",
            generatedAt = System.currentTimeMillis(),
            filter = ReportFilter(),
            records = sampleDetailedRecords,
            summary = AttendanceReportSummary(
                totalRecords = 3,
                presentCount = 1,
                lateCount = 1,
                absentCount = 1,
                excusedCount = 0,
                totalStudents = 3,
                totalEvents = 2
            )
        )
    }
}