package dev.ml.smartattendance.data.service

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PenaltyCalculationServiceImplTest {
    
    @Mock
    private lateinit var attendanceRepository: AttendanceRepository
    
    private lateinit var penaltyCalculationService: PenaltyCalculationServiceImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        penaltyCalculationService = PenaltyCalculationServiceImpl(attendanceRepository)
    }
    
    @Test
    fun `calculatePenalty should return null for on-time attendance`() {
        // Given
        val minutesLate = 0L
        
        // When
        val penalty = penaltyCalculationService.calculatePenalty(minutesLate)
        
        // Then
        assertNull(penalty)
    }
    
    @Test
    fun `calculatePenalty should return WARNING for 1-5 minutes late`() {
        // Given
        val minutesLate = 3L
        
        // When
        val penalty = penaltyCalculationService.calculatePenalty(minutesLate)
        
        // Then
        assertEquals(PenaltyType.WARNING, penalty)
    }
    
    @Test
    fun `calculatePenalty should return MINOR for 6-15 minutes late`() {
        // Given
        val minutesLate = 10L
        
        // When
        val penalty = penaltyCalculationService.calculatePenalty(minutesLate)
        
        // Then
        assertEquals(PenaltyType.MINOR, penalty)
    }
    
    @Test
    fun `calculatePenalty should return MAJOR for 16-30 minutes late`() {
        // Given
        val minutesLate = 25L
        
        // When
        val penalty = penaltyCalculationService.calculatePenalty(minutesLate)
        
        // Then
        assertEquals(PenaltyType.MAJOR, penalty)
    }
    
    @Test
    fun `calculatePenalty should return CRITICAL for more than 30 minutes late`() {
        // Given
        val minutesLate = 45L
        
        // When
        val penalty = penaltyCalculationService.calculatePenalty(minutesLate)
        
        // Then
        assertEquals(PenaltyType.CRITICAL, penalty)
    }
    
    @Test
    fun `evaluateStudentPenaltyStatus should calculate correct summary`() = runTest {
        // Given
        val studentId = \"STUDENT001\"
        val eventId = \"EVENT001\"
        val attendanceHistory = createMockAttendanceHistory()
        
        whenever(attendanceRepository.getAttendanceByStudentId(studentId)).thenReturn(attendanceHistory)
        
        // When
        val penaltyInfo = penaltyCalculationService.evaluateStudentPenaltyStatus(studentId, eventId)
        
        // Then
        assertEquals(studentId, penaltyInfo.studentId)
        assertEquals(2, penaltyInfo.totalLateCount) // 2 late records in mock data
        assertEquals(1, penaltyInfo.totalAbsentCount) // 1 absent record in mock data
        assertEquals(PenaltyType.WARNING, penaltyInfo.currentPenaltyLevel) // 2 late = WARNING level
    }
    
    @Test
    fun `shouldEscalatePenalty should return true for repeated critical violations`() = runTest {
        // Given\n        val studentId = \"STUDENT001\"\n        val attendanceHistory = createCriticalViolationHistory()\n        \n        whenever(attendanceRepository.getAttendanceByStudentId(studentId)).thenReturn(attendanceHistory)\n        \n        // When\n        val shouldEscalate = penaltyCalculationService.shouldEscalatePenalty(studentId)\n        \n        // Then\n        assertTrue(shouldEscalate)\n    }\n    \n    @Test\n    fun `getPenaltyRules should return all defined rules`() {\n        // When\n        val rules = penaltyCalculationService.getPenaltyRules()\n        \n        // Then\n        assertEquals(5, rules.size) // 4 late rules + 1 absent rule\n        assertTrue(rules.any { it.penaltyType == PenaltyType.WARNING })\n        assertTrue(rules.any { it.penaltyType == PenaltyType.MINOR })\n        assertTrue(rules.any { it.penaltyType == PenaltyType.MAJOR })\n        assertTrue(rules.any { it.penaltyType == PenaltyType.CRITICAL })\n    }\n    \n    private fun createMockAttendanceHistory(): List<AttendanceRecord> {\n        val currentTime = System.currentTimeMillis()\n        return listOf(\n            AttendanceRecord(\n                id = \"REC001\",\n                studentId = \"STUDENT001\",\n                eventId = \"EVENT001\",\n                timestamp = currentTime - 86400000, // 1 day ago\n                status = AttendanceStatus.LATE,\n                penalty = PenaltyType.WARNING,\n                latitude = 14.5995,\n                longitude = 120.9842\n            ),\n            AttendanceRecord(\n                id = \"REC002\",\n                studentId = \"STUDENT001\",\n                eventId = \"EVENT002\",\n                timestamp = currentTime - 172800000, // 2 days ago\n                status = AttendanceStatus.PRESENT,\n                penalty = null,\n                latitude = 14.5995,\n                longitude = 120.9842\n            ),\n            AttendanceRecord(\n                id = \"REC003\",\n                studentId = \"STUDENT001\",\n                eventId = \"EVENT003\",\n                timestamp = currentTime - 259200000, // 3 days ago\n                status = AttendanceStatus.LATE,\n                penalty = PenaltyType.MINOR,\n                latitude = 14.5995,\n                longitude = 120.9842\n            ),\n            AttendanceRecord(\n                id = \"REC004\",\n                studentId = \"STUDENT001\",\n                eventId = \"EVENT004\",\n                timestamp = currentTime - 345600000, // 4 days ago\n                status = AttendanceStatus.ABSENT,\n                penalty = PenaltyType.CRITICAL,\n                latitude = null,\n                longitude = null\n            )\n        )\n    }\n    \n    private fun createCriticalViolationHistory(): List<AttendanceRecord> {\n        val currentTime = System.currentTimeMillis()\n        return (1..5).map { i ->\n            AttendanceRecord(\n                id = \"CRIT_REC$i\",\n                studentId = \"STUDENT001\",\n                eventId = \"EVENT$i\",\n                timestamp = currentTime - (i * 86400000L), // i days ago\n                status = AttendanceStatus.ABSENT,\n                penalty = PenaltyType.CRITICAL,\n                latitude = null,\n                longitude = null\n            )\n        }\n    }\n}"