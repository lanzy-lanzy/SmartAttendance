package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.service.FirestoreService
import dev.ml.smartattendance.domain.usecase.CreateEventUseCase
import dev.ml.smartattendance.domain.usecase.GetCurrentEventsUseCase
import dev.ml.smartattendance.presentation.state.EventManagementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val getCurrentEventsUseCase: GetCurrentEventsUseCase,
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _state = MutableStateFlow(EventManagementState())
    val state: StateFlow<EventManagementState> = _state.asStateFlow()
    
    init {
        loadEvents()
    }
    
    fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                getCurrentEventsUseCase.getAllEvents().collect { events ->
                    _state.value = _state.value.copy(
                        events = events,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load events: ${e.message}"
                )
            }
        }
    }
    
    fun toggleEventStatus(eventId: String, isActive: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val event = firestoreService.getEvent(eventId)
                if (event != null) {
                    val updatedEvent = event.copy(isActive = isActive)
                    val success = firestoreService.updateEvent(updatedEvent)
                    
                    if (success) {
                        _state.value = _state.value.copy(
                            creationMessage = if (isActive) "Event activated" else "Event deactivated",
                            isLoading = false
                        )
                        // Reload events to get updated list
                        loadEvents()
                    } else {
                        _state.value = _state.value.copy(
                            error = "Failed to update event status",
                            isLoading = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = "Event not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Error updating event status: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun createEvent(
        name: String,
        startTime: Long,
        endTime: Long,
        latitude: Double,
        longitude: Double,
        radius: Float = 50f
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isCreating = true,
                creationMessage = null,
                error = null
            )
            
            val location = LatLng(latitude, longitude)
            
            when (val result = createEventUseCase.execute(
                name = name,
                startTime = startTime,
                endTime = endTime,
                location = location,
                geofenceRadius = radius
            )) {
                is CreateEventUseCase.EventCreationResult.Success -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        creationMessage = "Event created successfully!",
                        error = null
                    )
                    loadEvents() // Refresh the list
                }
                is CreateEventUseCase.EventCreationResult.Error -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        creationMessage = null,
                        error = result.message
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _state.value = _state.value.copy(
            creationMessage = null,
            error = null
        )
    }
}