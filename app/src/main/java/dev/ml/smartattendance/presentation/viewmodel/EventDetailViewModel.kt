package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.service.FirestoreService
import dev.ml.smartattendance.domain.usecase.CreateEventUseCase
import dev.ml.smartattendance.domain.usecase.GetCurrentEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EventDetailState(
    val event: Event? = null,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val getCurrentEventsUseCase: GetCurrentEventsUseCase,
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state.asStateFlow()
    
    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val event = firestoreService.getEvent(eventId)
                _state.value = _state.value.copy(
                    event = event,
                    isLoading = false,
                    error = if (event == null) "Event not found" else null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load event: ${e.message}"
                )
            }
        }
    }
    
    fun loadAttendanceRecords(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val records = firestoreService.getAttendanceByEvent(eventId)
                _state.value = _state.value.copy(
                    attendanceRecords = records,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load attendance records: ${e.message}"
                )
            }
        }
    }
    
    fun updateEvent(
        eventId: String,
        name: String,
        startTime: Long,
        endTime: Long,
        latitude: Double,
        longitude: Double,
        radius: Float,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUpdating = true,
                message = null,
                error = null
            )
            
            try {
                val currentEvent = _state.value.event
                if (currentEvent != null) {
                    val updatedEvent = currentEvent.copy(
                        name = name,
                        startTime = startTime,
                        endTime = endTime,
                        latitude = latitude,
                        longitude = longitude,
                        geofenceRadius = radius,
                        isActive = isActive
                    )
                    
                    val success = firestoreService.updateEvent(updatedEvent)
                    
                    if (success) {
                        _state.value = _state.value.copy(
                            event = updatedEvent,
                            isUpdating = false,
                            message = "Event updated successfully"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isUpdating = false,
                            error = "Failed to update event"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = "No event to update"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUpdating = false,
                    error = "Error updating event: ${e.message}"
                )
            }
        }
    }
    
    fun toggleEventStatus(eventId: String, isActive: Boolean) {
        viewModelScope.launch {
            val currentEvent = _state.value.event ?: return@launch
            
            _state.value = _state.value.copy(
                isUpdating = true,
                message = null,
                error = null
            )
            
            try {
                val updatedEvent = currentEvent.copy(isActive = isActive)
                val success = firestoreService.updateEvent(updatedEvent)
                
                if (success) {
                    _state.value = _state.value.copy(
                        event = updatedEvent,
                        isUpdating = false,
                        message = if (isActive) "Event activated" else "Event deactivated"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = "Failed to update event status"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUpdating = false,
                    error = "Error updating event status: ${e.message}"
                )
            }
        }
    }
    
    // Method to manually mark attendance for a student (for admin override)
    fun markAttendanceForStudent(
        eventId: String,
        studentId: String,
        status: dev.ml.smartattendance.domain.model.AttendanceStatus,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUpdating = true,
                message = null,
                error = null
            )
            
            try {
                // Check if record already exists
                val existingRecord = firestoreService.getAttendanceRecord(studentId, eventId)
                
                val record = if (existingRecord != null) {
                    existingRecord.copy(
                        status = status,
                        timestamp = System.currentTimeMillis(),
                        notes = notes ?: existingRecord.notes
                    )
                } else {
                    AttendanceRecord(
                        id = UUID.randomUUID().toString(),
                        studentId = studentId,
                        eventId = eventId,
                        timestamp = System.currentTimeMillis(),
                        status = status,
                        penalty = null, // Penalty will be calculated by a separate process
                        latitude = null, // No location for manual entries
                        longitude = null,
                        synced = true, // Mark as synced since it's created by admin
                        notes = notes
                    )
                }
                
                val success = if (existingRecord != null) {
                    firestoreService.updateAttendanceRecord(record)
                } else {
                    firestoreService.createAttendanceRecord(record)
                }
                
                if (success) {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        message = "Attendance marked for student"
                    )
                    
                    // Reload attendance records
                    loadAttendanceRecords(eventId)
                } else {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = "Failed to mark attendance"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUpdating = false,
                    error = "Error marking attendance: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _state.value = _state.value.copy(
            message = null,
            error = null
        )
    }
}