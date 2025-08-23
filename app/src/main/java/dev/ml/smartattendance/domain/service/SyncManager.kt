package dev.ml.smartattendance.domain.service

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    data class PartialSuccess(val syncedCount: Int, val failedCount: Int) : SyncResult()
}

interface SyncManager {
    suspend fun syncAttendanceRecords(): SyncResult
    suspend fun syncStudentData(): SyncResult
    suspend fun syncEventData(): SyncResult
    fun schedulePeriodicSync()
    suspend fun syncAll(): SyncResult
}