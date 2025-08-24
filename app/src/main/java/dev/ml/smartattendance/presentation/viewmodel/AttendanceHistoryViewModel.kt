package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttendanceHistoryState(
    val attendanceRecords: List<DetailedAttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AttendanceHistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AttendanceHistoryState())
    val state: StateFlow<AttendanceHistoryState> = _state.asStateFlow()
    
    fun loadAttendanceHistory() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // TODO: Get current student ID from auth service
                val studentId = "STUDENT001" // Placeholder
                val records = attendanceRepository.getDetailedAttendanceByStudentId(studentId)
                
                _state.value = _state.value.copy(
                    attendanceRecords = records,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    attendanceRecords = emptyList(),
                    isLoading = false,
                    error = "Failed to load attendance history: ${e.message}"
                )
            }
        }
    }
    
    fun refreshAttendanceHistory() {
        loadAttendanceHistory()
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}