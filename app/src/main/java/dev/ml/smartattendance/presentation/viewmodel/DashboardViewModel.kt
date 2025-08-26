package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.model.auth.User
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.domain.service.FirestoreService
import dev.ml.smartattendance.presentation.state.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authService: AuthService,
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState(userRole = UserRole.STUDENT))
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                // Get current user
                val currentUser = authService.getCurrentUser()
                
                // Update state
                _state.value = _state.value.copy(
                    userRole = UserRole.STUDENT, // This should be determined from the user
                    totalEvents = 0,
                    totalStudents = 0,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false
                )
            }
        }
    }
}