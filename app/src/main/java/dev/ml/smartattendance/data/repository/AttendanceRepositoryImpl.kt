package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.AttendanceRecordDao
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceRecordDao: AttendanceRecordDao,
    private val firestoreService: FirestoreService
) : AttendanceRepository {
    
    override fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>> = 
        firestoreService.getAttendanceFlow()
            .onStart { 
                // Start with local data for immediate display
                emit(attendanceRecordDao.getAllAttendanceRecords().first()) 
            }
            .map { firebaseRecords ->
                if (firebaseRecords.isNotEmpty()) {
                    // Cache Firebase data locally
                    attendanceRecordDao.insertAttendanceRecords(firebaseRecords)
                    firebaseRecords
                } else {
                    // Fallback to local data
                    attendanceRecordDao.getAllAttendanceRecords().first()
                }
            }
    
    override suspend fun getAttendanceByStudentId(studentId: String): List<AttendanceRecord> {
        return try {
            val firebaseRecords = firestoreService.getAttendanceByStudent(studentId)
            if (firebaseRecords.isNotEmpty()) {
                // Cache Firebase data locally
                attendanceRecordDao.insertAttendanceRecords(firebaseRecords)
                firebaseRecords
            } else {
                // Fallback to local data
                attendanceRecordDao.getAttendanceByStudentId(studentId)
            }
        } catch (e: Exception) {
            // Fallback to local data on error
            attendanceRecordDao.getAttendanceByStudentId(studentId)
        }
    }
    
    override suspend fun getDetailedAttendanceByStudentId(studentId: String): List<DetailedAttendanceRecord> = 
        attendanceRecordDao.getDetailedAttendanceByStudentId(studentId)
    
    override suspend fun getAttendanceByEventId(eventId: String): List<AttendanceRecord> {
        return try {
            val firebaseRecords = firestoreService.getAttendanceByEvent(eventId)
            if (firebaseRecords.isNotEmpty()) {
                // Cache Firebase data locally
                attendanceRecordDao.insertAttendanceRecords(firebaseRecords)
                firebaseRecords
            } else {
                // Fallback to local data
                attendanceRecordDao.getAttendanceByEventId(eventId)
            }
        } catch (e: Exception) {
            // Fallback to local data on error
            attendanceRecordDao.getAttendanceByEventId(eventId)
        }
    }
    
    override suspend fun getAttendanceRecord(studentId: String, eventId: String): AttendanceRecord? {
        return try {
            val firebaseRecord = firestoreService.getAttendanceRecord(studentId, eventId)
            if (firebaseRecord != null) {
                // Cache Firebase data locally
                attendanceRecordDao.insertAttendanceRecord(firebaseRecord)
                firebaseRecord
            } else {
                // Fallback to local data
                attendanceRecordDao.getAttendanceRecord(studentId, eventId)
            }
        } catch (e: Exception) {
            // Fallback to local data on error
            attendanceRecordDao.getAttendanceRecord(studentId, eventId)
        }
    }
    
    override suspend fun getUnsyncedRecords(): List<AttendanceRecord> = attendanceRecordDao.getUnsyncedRecords()
    
    override suspend fun getAttendanceInDateRange(startDate: Long, endDate: Long): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceInDateRange(startDate, endDate)
    
    override suspend fun getAttendanceByStatus(status: AttendanceStatus): List<AttendanceRecord> = 
        attendanceRecordDao.getAttendanceByStatus(status)
    
    override suspend fun insertAttendanceRecord(record: AttendanceRecord) {
        try {
            // Insert to Firebase first
            val success = firestoreService.createAttendanceRecord(record)
            if (success) {
                // Cache locally for offline access
                attendanceRecordDao.insertAttendanceRecord(record)
            } else {
                throw Exception("Failed to save attendance record to Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            attendanceRecordDao.insertAttendanceRecord(record)
            throw e
        }
    }
    
    override suspend fun insertAttendanceRecords(records: List<AttendanceRecord>) {
        // Insert records one by one to Firebase with local caching
        records.forEach { record ->
            try {
                val success = firestoreService.createAttendanceRecord(record)
                if (success) {
                    attendanceRecordDao.insertAttendanceRecord(record)
                }
            } catch (e: Exception) {
                // Continue with other records
            }
        }
    }
    
    override suspend fun updateAttendanceRecord(record: AttendanceRecord) {
        try {
            // Update in Firebase first
            val success = firestoreService.updateAttendanceRecord(record)
            if (success) {
                // Update local cache
                attendanceRecordDao.updateAttendanceRecord(record)
            } else {
                throw Exception("Failed to update attendance record in Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            attendanceRecordDao.updateAttendanceRecord(record)
            throw e
        }
    }
    
    override suspend fun deleteAttendanceRecord(record: AttendanceRecord) {
        try {
            // Delete from Firebase first
            val success = firestoreService.deleteAttendanceRecord(record.id)
            if (success) {
                // Delete from local cache
                attendanceRecordDao.deleteAttendanceRecord(record)
            } else {
                throw Exception("Failed to delete attendance record from Firebase")
            }
        } catch (e: Exception) {
            // Fallback to local only
            attendanceRecordDao.deleteAttendanceRecord(record)
            throw e
        }
    }
    
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