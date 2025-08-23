package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.usecase.GetCurrentEventsUseCase
import dev.ml.smartattendance.domain.usecase.MarkAttendanceUseCase
import dev.ml.smartattendance.presentation.state.AttendanceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val getCurrentEventsUseCase: GetCurrentEventsUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()
    
    init {
        loadCurrentEvents()
    }
    
    fun loadCurrentEvents() {
        viewModelScope.launch {
            try {
                val currentEvents = getCurrentEventsUseCase.getCurrentEvents()
                _state.value = _state.value.copy(
                    currentEvents = currentEvents,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load events: ${e.message}"
                )
            }
        }
    }
    
    fun markAttendance(studentId: String, eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isMarkingAttendance = true,
                attendanceMessage = null,
                error = null
            )
            
            when (val result = markAttendanceUseCase.execute(studentId, eventId)) {
                is MarkAttendanceUseCase.AttendanceResult.Success -> {
                    _state.value = _state.value.copy(
                        isMarkingAttendance = false,
                        attendanceMessage = "Attendance marked successfully!",
                        error = null
                    )
                }
                is MarkAttendanceUseCase.AttendanceResult.Error -> {
                    _state.value = _state.value.copy(
                        isMarkingAttendance = false,
                        attendanceMessage = null,
                        error = result.message
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _state.value = _state.value.copy(
            attendanceMessage = null,
            error = null
        )
    }
    
    fun setBiometricError(error: String) {
        _state.value = _state.value.copy(
            isMarkingAttendance = false,
            attendanceMessage = null,
            error = error
        )
    }
}