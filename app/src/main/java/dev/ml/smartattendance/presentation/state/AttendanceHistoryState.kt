package dev.ml.smartattendance.presentation.state

import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord

data class AttendanceHistoryState(
    val attendanceRecords: List<DetailedAttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)