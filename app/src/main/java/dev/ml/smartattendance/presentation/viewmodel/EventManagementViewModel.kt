package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import dev.ml.smartattendance.domain.usecase.CreateEventUseCase
import dev.ml.smartattendance.domain.usecase.GetCurrentEventsUseCase
import dev.ml.smartattendance.presentation.state.EventManagementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val getCurrentEventsUseCase: GetCurrentEventsUseCase,
    private val firestoreService: FirestoreService,
    private val eventRepository: EventRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(EventManagementState())
    val state: StateFlow<EventManagementState> = _state.asStateFlow()
    
    init {
        loadEvents()
        // Set up real-time event updates
        setupEventListener()
    }
    
    private fun setupEventListener() {
        viewModelScope.launch {
            try {
                android.util.Log.d("EventManagementViewModel", "Setting up event listener...")
                firestoreService.getEventsFlow().collect { events ->
                    android.util.Log.d("EventManagementViewModel", "Received ${events.size} events from Firebase flow")
                    _state.value = _state.value.copy(
                        events = events,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EventManagementViewModel", "Error in event listener: ${e.message}", e)
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to set up event updates: ${e.message}"
                )
            }
        }
    }
    
    fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            android.util.Log.d("EventManagementViewModel", "Starting to load events...")
            
            try {
                // Directly fetch from Firebase for immediate response
                android.util.Log.d("EventManagementViewModel", "Fetching events from Firebase...")
                val firebaseEvents = firestoreService.getAllEvents()
                android.util.Log.d("EventManagementViewModel", "Successfully loaded ${firebaseEvents.size} events from Firebase")
                
                // Save events to local database
                try {
                    if (firebaseEvents.isNotEmpty()) {
                        android.util.Log.d("EventManagementViewModel", "Attempting to cache ${firebaseEvents.size} events locally...")
                        eventRepository.insertEvents(firebaseEvents)
                        android.util.Log.d("EventManagementViewModel", "Successfully cached events locally")
                    } else {
                        android.util.Log.d("EventManagementViewModel", "No events to cache locally")
                    }
                } catch (cacheEx: Exception) {
                    android.util.Log.w("EventManagementViewModel", "Failed to cache events locally: ${cacheEx.message}", cacheEx)
                }
                
                _state.value = _state.value.copy(
                    events = firebaseEvents,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                android.util.Log.e("EventManagementViewModel", "Failed to load events from Firebase: ${e.message}", e)
                e.printStackTrace()
                
                // Fallback to repository flow if direct Firebase call fails
                try {
                    android.util.Log.d("EventManagementViewModel", "Trying fallback to local repository...")
                    val events = getCurrentEventsUseCase.getAllEvents().first()
                    android.util.Log.d("EventManagementViewModel", "Successfully loaded ${events.size} events from local repository")
                    
                    _state.value = _state.value.copy(
                        events = events,
                        isLoading = false,
                        error = "Using cached events. Could not connect to server: ${e.message}"
                    )
                } catch (fallbackEx: Exception) {
                    android.util.Log.e("EventManagementViewModel", "Fallback also failed: ${fallbackEx.message}", fallbackEx)
                    fallbackEx.printStackTrace()
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load events. Please check your connection and try again."
                    )
                }
            }
        }
    }
    
    fun refreshEvents() {
        viewModelScope.launch {
            try {
                // Directly fetch from Firebase to ensure we have the latest data
                val firebaseEvents = firestoreService.getAllEvents()
                
                // Update the local cache for offline access
                if (firebaseEvents.isNotEmpty()) {
                    eventRepository.insertEvents(firebaseEvents)
                }
                
                // The real-time listener will automatically update the UI
            } catch (e: Exception) {
                // If direct Firebase fetch fails, don't update the error state
                // since we still have the real-time listener
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
        radius: Float = 50f,
        signInStartOffset: Long = -15,
        signInEndOffset: Long = 30,
        signOutStartOffset: Long = -30,
        signOutEndOffset: Long = 15
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
                geofenceRadius = radius,
                signInStartOffset = signInStartOffset,
                signInEndOffset = signInEndOffset,
                signOutStartOffset = signOutStartOffset,
                signOutEndOffset = signOutEndOffset
            )) {
                is CreateEventUseCase.EventCreationResult.Success -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        creationMessage = "Event created successfully!",
                        error = null
                    )
                    // We don't need to manually refresh since we have a real-time listener
                    // But we'll still call loadEvents() as a fallback
                    loadEvents()
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
    
    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isCreating = true,
                creationMessage = null,
                error = null
            )
            
            try {
                // Directly use Firebase service for immediate update
                val success = firestoreService.updateEvent(event)
                
                if (success) {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        creationMessage = "Event updated successfully!",
                        error = null
                    )
                    // Real-time listener will update the UI automatically
                    // But still call loadEvents as a fallback
                    loadEvents()
                } else {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        creationMessage = null,
                        error = "Failed to update event"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isCreating = false,
                    creationMessage = null,
                    error = "Error updating event: ${e.message}"
                )
            }
        }
    }
    
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                creationMessage = null,
                error = null
            )
            
            try {
                // Directly use Firebase service for immediate deletion with cascade
                val success = firestoreService.deleteEventWithCascade(eventId)
                
                if (success) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        creationMessage = "Event and related attendance records deleted successfully!",
                        error = null
                    )
                    // Real-time listener will update the UI automatically
                    // But still call loadEvents as a fallback
                    loadEvents()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        creationMessage = null,
                        error = "Failed to delete event"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    creationMessage = null,
                    error = "Error deleting event: ${e.message}"
                )
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