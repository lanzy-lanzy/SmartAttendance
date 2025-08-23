package dev.ml.smartattendance.data.service

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportGenerationServiceImpl @Inject constructor(
    private val context: Context,
    private val attendanceRepository: AttendanceRepository
) : ReportGenerationService {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    override suspend fun generateAttendanceReport(
        filter: ReportFilter,
        format: ReportFormat
    ): ReportResult {
        return try {
            val report = getAttendanceData(filter)
            val file = when (format) {
                ReportFormat.CSV -> exportToCSV(report)
                ReportFormat.JSON -> exportToJSON(report)
                ReportFormat.PDF -> throw UnsupportedOperationException("PDF export not implemented")
            }
            ReportResult.Success(file)
        } catch (e: Exception) {
            ReportResult.Error("Failed to generate report: ${e.message}")
        }
    }
    
    override suspend fun getAttendanceData(filter: ReportFilter): AttendanceReport {
        val records = attendanceRepository.getDetailedAttendanceRecordsWithFilter(
            startDate = filter.startDate,
            endDate = filter.endDate,
            courseFilter = filter.courseFilter,
            statusFilter = filter.statusFilter,
            studentIdFilter = filter.studentIdFilter,
            eventIdFilter = filter.eventIdFilter
        )
        
        val summary = calculateSummary(records)
        
        return AttendanceReport(
            title = "Attendance Report - ${dateFormat.format(Date())}",
            generatedAt = System.currentTimeMillis(),
            filter = filter,
            records = records,
            summary = summary
        )
    }
    
    override suspend fun exportToCSV(report: AttendanceReport): File = withContext(Dispatchers.IO) {
        val fileName = "attendance_report_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("Student ID,Student Name,Course,Event ID,Event Name,Timestamp,Status,Penalty,Latitude,Longitude,Synced,Notes\n")
            
            // Write data
            report.records.forEach { record ->
                writer.append("${record.studentId},")
                writer.append("\"${record.studentName}\",")
                writer.append("\"${record.studentCourse}\",")
                writer.append("${record.eventId},")
                writer.append("\"${record.eventName}\",")
                writer.append("\"${dateFormat.format(Date(record.timestamp))}\",")
                writer.append("${record.status.name},")
                writer.append("${record.penalty?.name ?: "None"},")
                writer.append("${record.latitude},")
                writer.append("${record.longitude},")
                writer.append("${record.synced},")
                writer.append("\"${record.notes ?: ""}\",")
                writer.append("\n")
            }
        }
        
        file
    }
    
    override suspend fun exportToJSON(report: AttendanceReport): File = withContext(Dispatchers.IO) {
        val fileName = "attendance_report_${System.currentTimeMillis()}.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val jsonObject = JSONObject().apply {
            put("title", report.title)
            put("generatedAt", report.generatedAt)
            put("generatedAtFormatted", dateFormat.format(Date(report.generatedAt)))
            
            // Add filter info
            put("filter", JSONObject().apply {
                report.filter.startDate?.let { put("startDate", it) }
                report.filter.endDate?.let { put("endDate", it) }
                report.filter.courseFilter?.let { put("courseFilter", it) }
                report.filter.statusFilter?.let { put("statusFilter", it.name) }
                report.filter.studentIdFilter?.let { put("studentIdFilter", it) }
                report.filter.eventIdFilter?.let { put("eventIdFilter", it) }
            })
            
            // Add summary
            put("summary", JSONObject().apply {
                put("totalRecords", report.summary.totalRecords)
                put("presentCount", report.summary.presentCount)
                put("lateCount", report.summary.lateCount)
                put("absentCount", report.summary.absentCount)
                put("excusedCount", report.summary.excusedCount)
                put("totalStudents", report.summary.totalStudents)
                put("totalEvents", report.summary.totalEvents)
            })
            
            // Add records
            put("records", JSONArray().apply {
                report.records.forEach { record ->
                    put(JSONObject().apply {
                        put("id", record.id)
                        put("studentId", record.studentId)
                        put("studentName", record.studentName)
                        put("studentCourse", record.studentCourse)
                        put("eventId", record.eventId)
                        put("eventName", record.eventName)
                        put("timestamp", record.timestamp)
                        put("timestampFormatted", dateFormat.format(Date(record.timestamp)))
                        put("status", record.status.name)
                        put("penalty", record.penalty?.name ?: "None")
                        put("latitude", record.latitude)
                        put("longitude", record.longitude)
                        put("synced", record.synced)
                        put("notes", record.notes ?: "")
                    })
                }
            })
        }
        
        FileWriter(file).use { writer ->
            writer.write(jsonObject.toString(2))
        }
        
        file
    }
    
    override fun shareReport(file: File, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension.lowercase()) {
                    "csv" -> "text/csv"
                    "json" -> "application/json"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Report")
                putExtra(Intent.EXTRA_TEXT, "Please find the attendance report attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Attendance Report"))
        } catch (e: Exception) {
            // Handle sharing error
        }
    }
    
    private fun calculateSummary(records: List<DetailedAttendanceRecord>): AttendanceReportSummary {
        val totalRecords = records.size
        val presentCount = records.count { it.status == AttendanceStatus.PRESENT }
        val lateCount = records.count { it.status == AttendanceStatus.LATE }
        val absentCount = records.count { it.status == AttendanceStatus.ABSENT }
        val excusedCount = records.count { it.status == AttendanceStatus.EXCUSED }
        
        val totalStudents = records.map { it.studentId }.distinct().size
        val totalEvents = records.map { it.eventId }.distinct().size
        
        return AttendanceReportSummary(
            totalRecords = totalRecords,
            presentCount = presentCount,
            lateCount = lateCount,
            absentCount = absentCount,
            excusedCount = excusedCount,
            totalStudents = totalStudents,
            totalEvents = totalEvents
        )
    }
}