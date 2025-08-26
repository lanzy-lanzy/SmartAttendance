package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.presentation.state.AttendanceHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceHistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val authService: AuthService
) : ViewModel() {
    
    private val _state = MutableStateFlow(AttendanceHistoryState())
    val state: StateFlow<AttendanceHistoryState> = _state.asStateFlow()
    
    fun loadAttendanceHistory() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Get current student ID from auth service
                val currentUser = authService.getCurrentUser()
                val studentId = currentUser?.studentId
                
                android.util.Log.d("AttendanceHistoryViewModel", "Current user: ${currentUser?.uid}, role: ${currentUser?.role}, studentId: $studentId")
                
                if (studentId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        attendanceRecords = emptyList(),
                        isLoading = false,
                        error = "No student ID found. Please make sure you're logged in as a student."
                    )
                    return@launch
                }
                
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