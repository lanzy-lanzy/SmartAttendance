package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import javax.inject.Inject

/**
 * Use case for calculating penalties based on attendance patterns and SSC rules
 * as specified in Requirement 5 of the requirements document.
 */
class PenaltyCalculationUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val eventRepository: EventRepository,
    private val studentRepository: StudentRepository
) {
    
    sealed class PenaltyResult {
        data class Success(val penalty: PenaltyType?, val points: Int, val riskLevel: RiskLevel) : PenaltyResult()
        data class Error(val message: String) : PenaltyResult()
    }
    
    enum class RiskLevel {
        LOW,      // 0-20 penalty points
        MEDIUM,   // 21-40 penalty points  
        HIGH,     // 41-60 penalty points
        CRITICAL  // 61+ penalty points (requires administrative review)
    }
    
    /**
     * Calculate penalty for a specific attendance record
     */
    suspend fun calculatePenaltyForAttendance(
        studentId: String, 
        eventId: String, 
        attendanceStatus: AttendanceStatus,
        timestamp: Long
    ): PenaltyResult {
        try {
            val event = eventRepository.getEventById(eventId) 
                ?: return PenaltyResult.Error("Event not found")
            
            val penalty = when (attendanceStatus) {
                AttendanceStatus.PRESENT -> null // No penalty for being present
                AttendanceStatus.LATE -> calculateLatePenalty(event, timestamp)
                AttendanceStatus.ABSENT -> PenaltyType.MAJOR // Automatic major penalty for absence
                AttendanceStatus.EXCUSED -> null // No penalty for excused absence
            }
            
            // Calculate total penalty points for the student
            val studentRecords = attendanceRepository.getAttendanceByStudentId(studentId)
            val totalPoints = calculateTotalPenaltyPoints(studentRecords, penalty)
            val riskLevel = determineRiskLevel(totalPoints)
            
            return PenaltyResult.Success(penalty, totalPoints, riskLevel)
            
        } catch (e: Exception) {
            return PenaltyResult.Error("Failed to calculate penalty: ${e.message}")
        }
    }
    
    /**
     * Calculate penalty for late attendance based on how late the student is
     */
    private fun calculateLatePenalty(event: Event, timestamp: Long): PenaltyType {
        val eventStartTime = event.startTime
        val minutesLate = (timestamp - eventStartTime) / (60 * 1000)
        
        return when {
            minutesLate <= 5 -> PenaltyType.WARNING    // 1-5 minutes late
            minutesLate <= 15 -> PenaltyType.MINOR     // 6-15 minutes late
            minutesLate <= 30 -> PenaltyType.MAJOR     // 16-30 minutes late
            else -> PenaltyType.CRITICAL               // 30+ minutes late
        }
    }
    
    /**
     * Calculate total penalty points for a student based on SSC rules
     */
    private fun calculateTotalPenaltyPoints(
        records: List<AttendanceRecord>, 
        newPenalty: PenaltyType?
    ): Int {
        var totalPoints = 0
        
        // Count existing penalties
        records.forEach { record ->
            totalPoints += when (record.penalty) {
                PenaltyType.WARNING -> 1
                PenaltyType.MINOR -> 3
                PenaltyType.MAJOR -> 8
                PenaltyType.CRITICAL -> 15
                null -> 0
            }
        }
        
        // Add new penalty points
        totalPoints += when (newPenalty) {
            PenaltyType.WARNING -> 1
            PenaltyType.MINOR -> 3
            PenaltyType.MAJOR -> 8
            PenaltyType.CRITICAL -> 15
            null -> 0
        }
        
        return totalPoints
    }
    
    /**
     * Determine risk level based on total penalty points
     */
    private fun determineRiskLevel(totalPoints: Int): RiskLevel {
        return when {
            totalPoints <= 20 -> RiskLevel.LOW
            totalPoints <= 40 -> RiskLevel.MEDIUM
            totalPoints <= 60 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }
    
    /**
     * Get comprehensive penalty analysis for a student
     */
    suspend fun getStudentPenaltyAnalysis(studentId: String): StudentPenaltyAnalysis {
        try {
            val student = studentRepository.getStudentById(studentId)
                ?: return StudentPenaltyAnalysis.Error("Student not found")
            
            val records = attendanceRepository.getAttendanceByStudentId(studentId)
            val totalPoints = calculateTotalPenaltyPoints(records, null)
            val riskLevel = determineRiskLevel(totalPoints)
            
            val penaltyBreakdown = records.mapNotNull { it.penalty }
                .groupingBy { it }
                .eachCount()
            
            val attendanceRate = if (records.isNotEmpty()) {
                val presentCount = records.count { 
                    it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE 
                }
                (presentCount.toDouble() / records.size) * 100
            } else 100.0
            
            return StudentPenaltyAnalysis.Success(
                StudentPenaltyData(
                    studentId = studentId,
                    studentName = student.name,
                    totalPenaltyPoints = totalPoints,
                    riskLevel = riskLevel,
                    penaltyBreakdown = penaltyBreakdown,
                    attendanceRate = attendanceRate,
                    totalEvents = records.size,
                    flaggedForReview = riskLevel == RiskLevel.CRITICAL,
                    recommendations = generateRecommendations(riskLevel, attendanceRate)
                )
            )
            
        } catch (e: Exception) {
            return StudentPenaltyAnalysis.Error("Failed to analyze penalties: ${e.message}")
        }
    }
    
    /**
     * Generate recommendations based on student's risk level and attendance pattern
     */
    private fun generateRecommendations(riskLevel: RiskLevel, attendanceRate: Double): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (riskLevel) {
            RiskLevel.LOW -> {
                recommendations.add("Maintain current attendance pattern")
                if (attendanceRate < 95) {
                    recommendations.add("Aim for better punctuality to avoid warnings")
                }
            }
            RiskLevel.MEDIUM -> {
                recommendations.add("Improve attendance to avoid further penalties")
                recommendations.add("Contact academic advisor for support")
                recommendations.add("Review course schedule for conflicts")
            }
            RiskLevel.HIGH -> {
                recommendations.add("Immediate improvement required")
                recommendations.add("Mandatory meeting with academic advisor")
                recommendations.add("Consider academic counseling")
                recommendations.add("Review academic load and time management")
            }
            RiskLevel.CRITICAL -> {
                recommendations.add("URGENT: Administrative review required")
                recommendations.add("Risk of academic probation")
                recommendations.add("Mandatory counseling session")
                recommendations.add("Possible course withdrawal consideration")
                recommendations.add("Contact student services immediately")
            }
        }
        
        return recommendations
    }
    
    /**
     * Check if student should be flagged for administrative review
     */
    suspend fun shouldFlagStudentForReview(studentId: String): Boolean {
        val analysis = getStudentPenaltyAnalysis(studentId)
        return when (analysis) {
            is StudentPenaltyAnalysis.Success -> analysis.data.flaggedForReview
            is StudentPenaltyAnalysis.Error -> false
        }
    }
    
    /**
     * Get all students requiring administrative review
     */
    suspend fun getStudentsRequiringReview(): List<String> {
        return try {
            val allStudents = mutableListOf<dev.ml.smartattendance.data.entity.Student>()
            studentRepository.getAllActiveStudents().collect { students ->
                allStudents.addAll(students)
            }
            
            allStudents.mapNotNull { student ->
                if (shouldFlagStudentForReview(student.id)) {
                    student.id
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Data classes for penalty analysis results
 */
sealed class StudentPenaltyAnalysis {
    data class Success(val data: StudentPenaltyData) : StudentPenaltyAnalysis()
    data class Error(val message: String) : StudentPenaltyAnalysis()
}

data class StudentPenaltyData(
    val studentId: String,
    val studentName: String,
    val totalPenaltyPoints: Int,
    val riskLevel: PenaltyCalculationUseCase.RiskLevel,
    val penaltyBreakdown: Map<PenaltyType, Int>,
    val attendanceRate: Double,
    val totalEvents: Int,
    val flaggedForReview: Boolean,
    val recommendations: List<String>
)

/**
 * Configuration for penalty calculation rules (SSC rules)
 */
data class PenaltyConfiguration(
    val warningPoints: Int = 1,
    val minorPoints: Int = 3,
    val majorPoints: Int = 8,
    val criticalPoints: Int = 15,
    val lowRiskThreshold: Int = 20,
    val mediumRiskThreshold: Int = 40,
    val highRiskThreshold: Int = 60,
    val lateGracePeriod: Long = 5, // minutes
    val minorLateThreshold: Long = 15, // minutes
    val majorLateThreshold: Long = 30 // minutes
)