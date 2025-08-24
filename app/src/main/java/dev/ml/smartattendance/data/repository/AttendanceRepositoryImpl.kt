package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.AttendanceRecordDao
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceRecordDao: AttendanceRecordDao
) : AttendanceRepository {
    
    override fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>> = attendanceRecordDao.getAllAttendanceRecords()
    
    override suspend fun getAttendanceByStudentId(studentId: String): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceByStudentId(studentId)
    
    override suspend fun getDetailedAttendanceByStudentId(studentId: String): List<DetailedAttendanceRecord> = 
        attendanceRecordDao.getDetailedAttendanceByStudentId(studentId)
    
    override suspend fun getAttendanceByEventId(eventId: String): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceByEventId(eventId)
    
    override suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord? = 
        attendanceRecordDao.getAttendanceRecord(studentId, eventId)
    
    override suspend fun getUnsyncedRecords(): List<AttendanceRecord> = attendanceRecordDao.getUnsyncedRecords()
    
    override suspend fun getAttendanceInDateRange(startDate: Long, endDate: Long): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceInDateRange(startDate, endDate)
    
    override suspend fun getAttendanceByStatus(status: AttendanceStatus): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceByStatus(status)
    
    override suspend fun insertAttendanceRecord(record: AttendanceRecord) = 
        attendanceRecordDao.insertAttendanceRecord(record)
    
    override suspend fun insertAttendanceRecords(records: List<AttendanceRecord>) = 
        attendanceRecordDao.insertAttendanceRecords(records)
    
    override suspend fun updateAttendanceRecord(record: AttendanceRecord) = 
        attendanceRecordDao.updateAttendanceRecord(record)
    
    override suspend fun deleteAttendanceRecord(record: AttendanceRecord) = 
        attendanceRecordDao.deleteAttendanceRecord(record)
    
    override suspend fun markRecordsAsSynced(recordIds: List<String>) = 
        attendanceRecordDao.markRecordsAsSynced(recordIds)
    
    override suspend fun getAttendanceCountForEvent(eventId: String): Int = 
        attendanceRecordDao.getAttendanceCountForEvent(eventId)
    
    override suspend fun getDetailedAttendanceInDateRange(startDate: Long, endDate: Long): List<DetailedAttendanceRecord> = 
        attendanceRecordDao.getDetailedAttendanceInDateRange(startDate, endDate)
    
    override suspend fun getDetailedAttendanceRecordsWithFilter(
        startDate: Long?,
        endDate: Long?,
        courseFilter: String?,
        statusFilter: AttendanceStatus?,
        studentIdFilter: String?,
        eventIdFilter: String?
    ): List<DetailedAttendanceRecord> = 
        attendanceRecordDao.getDetailedAttendanceRecordsWithFilter(
            startDate, endDate, courseFilter, statusFilter, studentIdFilter, eventIdFilter
        )
}