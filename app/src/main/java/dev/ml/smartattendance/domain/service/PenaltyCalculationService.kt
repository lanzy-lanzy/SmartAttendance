package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType

data class PenaltyRule(
    val status: AttendanceStatus,
    val minutesLate: Long?,
    val penaltyType: PenaltyType,
    val description: String
)

data class StudentPenaltyInfo(
    val studentId: String,
    val totalLateCount: Int,
    val totalAbsentCount: Int,
    val currentPenaltyLevel: PenaltyType?,
    val recommendedAction: String?
)

interface PenaltyCalculationService {
    fun calculatePenalty(minutesLate: Long): PenaltyType?
    suspend fun evaluateStudentPenaltyStatus(studentId: String, eventId: String): StudentPenaltyInfo
    fun getPenaltyRules(): List<PenaltyRule>
    suspend fun shouldEscalatePenalty(studentId: String): Boolean
}