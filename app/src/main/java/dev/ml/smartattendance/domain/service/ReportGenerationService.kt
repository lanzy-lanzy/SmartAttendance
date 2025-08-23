package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import java.io.File

data class ReportFilter(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val courseFilter: String? = null,
    val statusFilter: AttendanceStatus? = null,
    val studentIdFilter: String? = null,
    val eventIdFilter: String? = null
)

data class AttendanceReport(
    val title: String,
    val generatedAt: Long,
    val filter: ReportFilter,
    val records: List<DetailedAttendanceRecord>,
    val summary: AttendanceReportSummary
)

data class AttendanceReportSummary(
    val totalRecords: Int,
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val excusedCount: Int,
    val totalStudents: Int,
    val totalEvents: Int
)

enum class ReportFormat {
    CSV, JSON, PDF
}

sealed class ReportResult {
    data class Success(val file: File) : ReportResult()
    data class Error(val message: String) : ReportResult()
}

interface ReportGenerationService {
    suspend fun generateAttendanceReport(
        filter: ReportFilter,
        format: ReportFormat = ReportFormat.CSV
    ): ReportResult
    
    suspend fun getAttendanceData(filter: ReportFilter): AttendanceReport
    
    suspend fun exportToCSV(report: AttendanceReport): File
    suspend fun exportToJSON(report: AttendanceReport): File
    
    fun shareReport(file: File, context: android.content.Context)
}