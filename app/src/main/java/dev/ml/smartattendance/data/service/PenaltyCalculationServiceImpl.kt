package dev.ml.smartattendance.data.service

import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.service.PenaltyCalculationService
import dev.ml.smartattendance.domain.service.PenaltyRule
import dev.ml.smartattendance.domain.service.StudentPenaltyInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PenaltyCalculationServiceImpl @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : PenaltyCalculationService {
    
    private val penaltyRules = listOf(
        PenaltyRule(
            status = AttendanceStatus.LATE,
            minutesLate = 5,
            penaltyType = PenaltyType.WARNING,
            description = "Late by 1-5 minutes"
        ),
        PenaltyRule(
            status = AttendanceStatus.LATE,
            minutesLate = 15,
            penaltyType = PenaltyType.MINOR,
            description = "Late by 6-15 minutes"
        ),
        PenaltyRule(
            status = AttendanceStatus.LATE,
            minutesLate = 30,
            penaltyType = PenaltyType.MAJOR,
            description = "Late by 16-30 minutes"
        ),
        PenaltyRule(
            status = AttendanceStatus.LATE,
            minutesLate = Long.MAX_VALUE,
            penaltyType = PenaltyType.CRITICAL,
            description = "Late by more than 30 minutes"
        ),
        PenaltyRule(
            status = AttendanceStatus.ABSENT,
            minutesLate = null,
            penaltyType = PenaltyType.CRITICAL,
            description = "Absent from class"
        )
    )
    
    override fun calculatePenalty(minutesLate: Long): PenaltyType? {
        return when {
            minutesLate <= 0 -> null // Not late
            minutesLate <= 5 -> PenaltyType.WARNING
            minutesLate <= 15 -> PenaltyType.MINOR
            minutesLate <= 30 -> PenaltyType.MAJOR
            else -> PenaltyType.CRITICAL
        }
    }
    
    override suspend fun evaluateStudentPenaltyStatus(studentId: String, eventId: String): StudentPenaltyInfo {
        val attendanceHistory = attendanceRepository.getAttendanceByStudentId(studentId)
        
        val lateCount = attendanceHistory.count { it.status == AttendanceStatus.LATE }
        val absentCount = attendanceHistory.count { it.status == AttendanceStatus.ABSENT }
        
        val severePenalties = attendanceHistory.count { 
            it.penalty == PenaltyType.MAJOR || it.penalty == PenaltyType.CRITICAL 
        }
        
        val currentPenaltyLevel = when {
            severePenalties >= 5 -> PenaltyType.CRITICAL
            severePenalties >= 3 -> PenaltyType.MAJOR
            lateCount >= 5 -> PenaltyType.MINOR
            lateCount >= 2 -> PenaltyType.WARNING
            else -> null
        }
        
        val recommendedAction = when {
            severePenalties >= 5 -> "Refer to Student Services Committee for disciplinary action"
            severePenalties >= 3 -> "Schedule meeting with academic advisor"
            lateCount >= 5 -> "Issue formal warning letter"
            lateCount >= 2 -> "Informal counseling session recommended"
            else -> null
        }
        
        return StudentPenaltyInfo(
            studentId = studentId,
            totalLateCount = lateCount,
            totalAbsentCount = absentCount,
            currentPenaltyLevel = currentPenaltyLevel,
            recommendedAction = recommendedAction
        )
    }
    
    override fun getPenaltyRules(): List<PenaltyRule> = penaltyRules
    
    override suspend fun shouldEscalatePenalty(studentId: String): Boolean {
        val attendanceHistory = attendanceRepository.getAttendanceByStudentId(studentId)
        
        // Check for pattern of repeated violations
        val recentRecords = attendanceHistory.take(10) // Last 10 records
        val criticalPenalties = recentRecords.count { it.penalty == PenaltyType.CRITICAL }
        val majorPenalties = recentRecords.count { it.penalty == PenaltyType.MAJOR }
        
        return criticalPenalties >= 3 || majorPenalties >= 5
    }
}