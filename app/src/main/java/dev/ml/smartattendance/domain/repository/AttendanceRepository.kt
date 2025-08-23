package dev.ml.smartattendance.domain.repository

import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>
    suspend fun getAttendanceByStudentId(studentId: String): List<AttendanceRecord>
    suspend fun getAttendanceByEventId(eventId: String): List<AttendanceRecord>
    suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord?
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>
    suspend fun getAttendanceInDateRange(startDate: Long, endDate: Long): List<AttendanceRecord>
    suspend fun getAttendanceByStatus(status: AttendanceStatus): List<AttendanceRecord>
    suspend fun insertAttendanceRecord(record: AttendanceRecord)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)
    suspend fun updateAttendanceRecord(record: AttendanceRecord)
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)
    suspend fun markRecordsAsSynced(recordIds: List<String>)
    suspend fun getAttendanceCountForEvent(eventId: String): Int
    suspend fun getDetailedAttendanceInDateRange(startDate: Long, endDate: Long): List<DetailedAttendanceRecord>
    suspend fun getDetailedAttendanceRecordsWithFilter(
        startDate: Long?,
        endDate: Long?,
        courseFilter: String?,
        statusFilter: AttendanceStatus?,
        studentIdFilter: String?,
        eventIdFilter: String?
    ): List<DetailedAttendanceRecord>
}