package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.usecase.GetCurrentEventsUseCase
import dev.ml.smartattendance.domain.usecase.MarkAttendanceUseCase
import dev.ml.smartattendance.presentation.state.AttendanceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val getCurrentEventsUseCase: GetCurrentEventsUseCase,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()
    
    init {
        loadCurrentEvents()
    }
    
    fun loadCurrentEvents() {
        viewModelScope.launch {
            try {
                // Load all active events instead of just currently running ones
                // This allows students to see events they can mark attendance for
                val allEvents = getCurrentEventsUseCase.getAllEvents().first()
                val activeEvents = allEvents.filter { it.isActive }
                
                // Load marked events for current student
                val studentId = "STUDENT001" // TODO: Get from auth service
                val markedEventIds = loadMarkedEventIds(studentId)
                
                _state.value = _state.value.copy(
                    currentEvents = activeEvents,
                    markedEventIds = markedEventIds,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load events: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadMarkedEventIds(studentId: String): Set<String> {
        return try {
            val attendanceRecords = attendanceRepository.getAttendanceByStudentId(studentId)
            attendanceRecords.map { it.eventId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    fun markAttendance(studentId: String, eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isMarkingAttendance = true,
                attendanceMessage = null,
                error = null
            )
            
            try {
                when (val result = markAttendanceUseCase.execute(studentId, eventId)) {
                    is MarkAttendanceUseCase.AttendanceResult.Success -> {
                        // Add eventId to marked events
                        val updatedMarkedEvents = _state.value.markedEventIds + eventId
                        
                        _state.value = _state.value.copy(
                            isMarkingAttendance = false,
                            markedEventIds = updatedMarkedEvents,
                            attendanceMessage = "Attendance marked successfully! Thank you for being present.",
                            error = null
                        )
                        
                        // Clear message after 5 seconds
                        kotlinx.coroutines.delay(5000)
                        _state.value = _state.value.copy(attendanceMessage = null)
                    }
                    is MarkAttendanceUseCase.AttendanceResult.Error -> {
                        _state.value = _state.value.copy(
                            isMarkingAttendance = false,
                            attendanceMessage = null,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isMarkingAttendance = false,
                    attendanceMessage = null,
                    error = "Unexpected error occurred: ${e.message}"
                )
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
    
    fun setLocationError(error: String) {
        _state.value = _state.value.copy(
            isMarkingAttendance = false,
            attendanceMessage = null,
            error = error
        )
    }
    
    fun startLocationValidation() {
        _state.value = _state.value.copy(
            isMarkingAttendance = true,
            error = null
        )
    }
    
    fun completeLocationValidation(success: Boolean, message: String? = null) {
        _state.value = _state.value.copy(
            isMarkingAttendance = false,
            error = if (success) null else message,
            attendanceMessage = if (success && message != null) message else null
        )
    }
    
    fun getEventById(eventId: String) = state.value.currentEvents.find { it.id == eventId }
    
    fun isAttendanceMarked(eventId: String): Boolean {
        return state.value.markedEventIds.contains(eventId)
    }
}