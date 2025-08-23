package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.model.auth.AdminLevel
import dev.ml.smartattendance.domain.model.auth.RegisterRequest
import dev.ml.smartattendance.domain.service.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authService.signIn(email, password)
            
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(result.errorMessage ?: "Sign in failed")
            }
        }
    }
    
    fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        studentId: String? = null,
        course: String? = null,
        adminLevel: AdminLevel = AdminLevel.BASIC
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val request = RegisterRequest(
                email = email,
                password = password,
                name = name,
                role = role,
                studentId = studentId,
                course = course,
                adminLevel = adminLevel
            )
            
            val result = authService.signUp(request)
            
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(result.errorMessage ?: "Sign up failed")
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            _uiState.value = AuthUiState.Initial
        }
    }
    
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val success = authService.sendPasswordReset(email)
            
            if (success) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error("Failed to send password reset email")
            }
        }
    }
    
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Initial
        }
    }
}