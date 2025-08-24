package dev.ml.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.usecase.EnrollStudentUseCase
import dev.ml.smartattendance.presentation.state.StudentManagementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentManagementViewModel @Inject constructor(
    private val enrollStudentUseCase: EnrollStudentUseCase,
    private val studentRepository: StudentRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(StudentManagementState())
    val state: StateFlow<StudentManagementState> = _state.asStateFlow()
    
    init {
        loadStudents()
    }
    
    fun loadStudents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val students = studentRepository.getAllActiveStudents().first()
                _state.value = _state.value.copy(
                    students = students,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load students: ${e.message}"
                )
            }
        }
    }
    
    fun enrollStudent(studentId: String, name: String, course: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isEnrolling = true,
                enrollmentMessage = null,
                error = null
            )
            
            when (val result = enrollStudentUseCase.execute(studentId, name, course)) {
                is EnrollStudentUseCase.EnrollmentResult.Success -> {
                    _state.value = _state.value.copy(
                        isEnrolling = false,
                        enrollmentMessage = "Student enrolled successfully!",
                        error = null
                    )
                    loadStudents() // Refresh the list
                }
                is EnrollStudentUseCase.EnrollmentResult.Error -> {
                    _state.value = _state.value.copy(
                        isEnrolling = false,
                        enrollmentMessage = null,
                        error = result.message
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _state.value = _state.value.copy(
            enrollmentMessage = null,
            error = null
        )
    }
}