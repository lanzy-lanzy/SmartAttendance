package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.domain.service.FirestoreService
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
    private val attendanceRepository: AttendanceRepository,
    private val firestoreService: FirestoreService,
    private val authService: AuthService
) : ViewModel() {
    
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()
    
    init {
        loadCurrentEvents()
    }
    
    fun loadCurrentEvents() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isMarkingAttendance = true, // Use isMarkingAttendance instead of isLoading
                    error = null
                )
                
                android.util.Log.d("AttendanceViewModel", "Starting event loading from Firebase...")
                
                // Get all active events directly from Firebase
                val activeEvents = try {
                    // Get events with timeout handling
                    val firebaseFetchTimeout = kotlinx.coroutines.withTimeoutOrNull(5000) {
                        val allEvents = firestoreService.getAllEvents()
                        android.util.Log.d("AttendanceViewModel", "getAllEvents returned ${allEvents.size} events")
                        allEvents.filter { it.isActive }
                    }
                    
                    if (firebaseFetchTimeout != null) {
                        android.util.Log.d("AttendanceViewModel", "Successfully loaded ${firebaseFetchTimeout.size} active events from Firebase")
                        firebaseFetchTimeout
                    } else {
                        android.util.Log.w("AttendanceViewModel", "Firebase fetch timed out, falling back to use case")
                        // Fallback to use case if Firebase times out
                        val fallbackEvents = getCurrentEventsUseCase.getAllEvents().first()
                        android.util.Log.d("AttendanceViewModel", "Fallback returned ${fallbackEvents.size} events")
                        fallbackEvents
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AttendanceViewModel", "Error fetching from Firebase: ${e.message}. Falling back to use case.", e)
                    // Only fallback to use case if Firebase direct call fails
                    try {
                        val fallbackEvents = getCurrentEventsUseCase.getAllEvents().first()
                        android.util.Log.d("AttendanceViewModel", "Use case fallback returned ${fallbackEvents.size} events")
                        fallbackEvents
                    } catch (useCaseException: Exception) {
                        android.util.Log.e("AttendanceViewModel", "Error in use case fallback: ${useCaseException.message}", useCaseException)
                        emptyList()
                    }
                }
                
                android.util.Log.d("AttendanceViewModel", "Got ${activeEvents.size} active events. Event IDs: ${activeEvents.map { it.id }}")
                
                // Load marked events for current student from authenticated user
                val currentUser = authService.getCurrentUser()
                val studentId = currentUser?.studentId
                
                android.util.Log.d("AttendanceViewModel", "Current user: ${currentUser?.uid}, role: ${currentUser?.role}, studentId: $studentId")
                
                // Use the student ID from auth service if available, otherwise use a default for testing
                val markedEventIds = if (!studentId.isNullOrBlank()) {
                    android.util.Log.d("AttendanceViewModel", "Loading marked events for authenticated student ID: $studentId")
                    loadMarkedEventIds(studentId)
                } else {
                    android.util.Log.w("AttendanceViewModel", "No authenticated student found, marked events won't be loaded correctly. User ID: ${currentUser?.uid}, role: ${currentUser?.role}")
                    emptySet()
                }
                
                _state.value = _state.value.copy(
                    currentEvents = activeEvents,
                    markedEventIds = markedEventIds,
                    isMarkingAttendance = false, // Use isMarkingAttendance instead of isLoading
                    error = null
                )
            } catch (e: Exception) {
                android.util.Log.e("AttendanceViewModel", "Failed to load events: ${e.message}", e)
                _state.value = _state.value.copy(
                    isMarkingAttendance = false, // Use isMarkingAttendance instead of isLoading
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
            
            android.util.Log.d("AttendanceViewModel", "Marking attendance for student: '$studentId', event: '$eventId'")
            
            // Validate studentId
            if (studentId.isBlank()) {
                android.util.Log.e("AttendanceViewModel", "Invalid student ID provided: '$studentId'")
                _state.value = _state.value.copy(
                    isMarkingAttendance = false,
                    error = "Invalid student ID. Please make sure you're logged in as a student."
                )
                return@launch
            }
            
            try {
                // First check if event exists in Firebase to handle the case properly
                android.util.Log.d("AttendanceViewModel", "Checking if event exists in Firebase")
                val eventExists = try {
                    val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                    val event = firestoreService.getEvent(eventId)
                    android.util.Log.d("AttendanceViewModel", "Event lookup result: ${event?.name ?: "null"}")
                    event != null
                } catch (e: Exception) {
                    android.util.Log.e("AttendanceViewModel", "Error checking event in Firebase: ${e.message}", e)
                    false
                }
                
                if (!eventExists) {
                    android.util.Log.e("AttendanceViewModel", "Event not found in Firebase: '$eventId'")
                    _state.value = _state.value.copy(
                        isMarkingAttendance = false,
                        error = "Event not found in Firebase. Please refresh and try again."
                    )
                    return@launch
                }
                
                // Check if attendance is already marked in Firebase
                android.util.Log.d("AttendanceViewModel", "Checking if attendance is already marked in Firebase")
                val attendanceExists = try {
                    val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                    val record = firestoreService.getAttendanceRecord(studentId, eventId)
                    android.util.Log.d("AttendanceViewModel", "Attendance record lookup result: ${record?.id ?: "null"}")
                    record != null
                } catch (e: Exception) {
                    android.util.Log.e("AttendanceViewModel", "Error checking attendance in Firebase: ${e.message}", e)
                    false
                }
                
                if (attendanceExists) {
                    android.util.Log.e("AttendanceViewModel", "Attendance already marked in Firebase")
                    _state.value = _state.value.copy(
                        isMarkingAttendance = false,
                        error = "Attendance already marked for this event in Firebase."
                    )
                    return@launch
                }
                
                android.util.Log.d("AttendanceViewModel", "Using MarkAttendanceUseCase to mark attendance")
                when (val result = markAttendanceUseCase.execute(studentId, eventId)) {
                    is MarkAttendanceUseCase.AttendanceResult.Success -> {
                        android.util.Log.d("AttendanceViewModel", "Attendance marked successfully in Firebase")
                        // Add eventId to marked events
                        val updatedMarkedEvents = _state.value.markedEventIds + eventId
                        
                        _state.value = _state.value.copy(
                            isMarkingAttendance = false,
                            markedEventIds = updatedMarkedEvents,
                            attendanceMessage = "Attendance marked successfully in Firebase!",
                            error = null
                        )
                        
                        // Clear message after 5 seconds
                        kotlinx.coroutines.delay(5000)
                        if (_state.value.attendanceMessage != null) {
                            _state.value = _state.value.copy(attendanceMessage = null)
                        }
                    }
                    is MarkAttendanceUseCase.AttendanceResult.Error -> {
                        android.util.Log.e("AttendanceViewModel", "Error marking attendance: ${result.message}")
                        _state.value = _state.value.copy(
                            isMarkingAttendance = false,
                            attendanceMessage = null,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AttendanceViewModel", "Unexpected error marking attendance: ${e.message}", e)
                _state.value = _state.value.copy(
                    isMarkingAttendance = false,
                    attendanceMessage = null,
                    error = "Unexpected error occurred while marking attendance in Firebase: ${e.message}"
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
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun clearAttendanceMessage() {
        _state.value = _state.value.copy(attendanceMessage = null)
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