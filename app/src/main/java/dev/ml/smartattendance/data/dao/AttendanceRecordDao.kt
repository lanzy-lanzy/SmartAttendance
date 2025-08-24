package dev.ml.smartattendance.data.dao

import androidx.room.*
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {
    
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>
    
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY timestamp DESC")
    suspend fun getAttendanceByStudentId(studentId: String): List<AttendanceRecord>
    
    @Query("""
        SELECT ar.*, s.name as studentName, s.course as studentCourse, e.name as eventName 
        FROM attendance_records ar 
        JOIN students s ON ar.studentId = s.id 
        JOIN events e ON ar.eventId = e.id 
        WHERE ar.studentId = :studentId 
        ORDER BY ar.timestamp DESC
    """)
    suspend fun getDetailedAttendanceByStudentId(studentId: String): List<DetailedAttendanceRecord>
    
    @Query("SELECT * FROM attendance_records WHERE eventId = :eventId ORDER BY timestamp ASC")
    suspend fun getAttendanceByEventId(eventId: String): List<AttendanceRecord>
    
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND eventId = :eventId")
    suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord?
    
    @Query("SELECT * FROM attendance_records WHERE synced = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>
    
    @Query("SELECT * FROM attendance_records WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getAttendanceInDateRange(startDate: Long, endDate: Long): List<AttendanceRecord>
    
    @Query("SELECT * FROM attendance_records WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getAttendanceByStatus(status: AttendanceStatus): List<AttendanceRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)
    
    @Update
    suspend fun updateAttendanceRecord(record: AttendanceRecord)
    
    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)
    
    @Query("UPDATE attendance_records SET synced = 1 WHERE id IN (:recordIds)")
    suspend fun markRecordsAsSynced(recordIds: List<String>)
    
    @Query("SELECT COUNT(*) FROM attendance_records WHERE eventId = :eventId")
    suspend fun getAttendanceCountForEvent(eventId: String): Int
    
    @Query("""
        SELECT ar.*, s.name as studentName, s.course as studentCourse, e.name as eventName 
        FROM attendance_records ar 
        JOIN students s ON ar.studentId = s.id 
        JOIN events e ON ar.eventId = e.id 
        WHERE ar.timestamp BETWEEN :startDate AND :endDate 
        ORDER BY ar.timestamp ASC
    """)
    suspend fun getDetailedAttendanceInDateRange(startDate: Long, endDate: Long): List<DetailedAttendanceRecord>
    
    @Query("""
        SELECT ar.*, s.name as studentName, s.course as studentCourse, e.name as eventName 
        FROM attendance_records ar 
        JOIN students s ON ar.studentId = s.id 
        JOIN events e ON ar.eventId = e.id 
        WHERE (:startDate IS NULL OR ar.timestamp >= :startDate) 
        AND (:endDate IS NULL OR ar.timestamp <= :endDate)
        AND (:courseFilter IS NULL OR s.course = :courseFilter)
        AND (:status IS NULL OR ar.status = :status)
        AND (:studentIdFilter IS NULL OR ar.studentId = :studentIdFilter)
        AND (:eventIdFilter IS NULL OR ar.eventId = :eventIdFilter)
        ORDER BY ar.timestamp ASC
    """)
    suspend fun getDetailedAttendanceRecordsWithFilter(
        startDate: Long?,
        endDate: Long?,
        courseFilter: String?,
        status: AttendanceStatus?,
        studentIdFilter: String?,
        eventIdFilter: String?
    ): List<DetailedAttendanceRecord>
}

data class DetailedAttendanceRecord(
    val id: String,
    val studentId: String,
    val eventId: String,
    val timestamp: Long,
    val status: AttendanceStatus,
    val penalty: dev.ml.smartattendance.domain.model.PenaltyType?,
    val latitude: Double?,
    val longitude: Double?,
    val synced: Boolean,
    val notes: String?,
    val studentName: String,
    val studentCourse: String,
    val eventName: String
)