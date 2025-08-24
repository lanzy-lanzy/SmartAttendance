package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.presentation.screen.auth.SplashUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthenticationStatus()
    }
    
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            try {
                // Check if user is signed in
                if (authService.isUserSignedIn()) {
                    // Get current user to determine role
                    val currentUser = authService.getCurrentUser()
                    if (currentUser != null) {
                        _uiState.value = SplashUiState.Authenticated(currentUser.role)
                    } else {
                        // User is signed in but profile not found, sign out and go to login
                        authService.signOut()
                        _uiState.value = SplashUiState.NotAuthenticated
                    }
                } else {
                    _uiState.value = SplashUiState.NotAuthenticated
                }
            } catch (e: Exception) {
                // Error occurred, assume not authenticated
                _uiState.value = SplashUiState.NotAuthenticated
            }
        }
    }
    
    fun retryAuthentication() {
        _uiState.value = SplashUiState.Loading
        checkAuthenticationStatus()
    }
}