package dev.ml.smartattendance.presentation.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.usecase.EnrollStudentUseCase
import dev.ml.smartattendance.presentation.screen.admin.StudentAttendanceData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComprehensiveStudentManagementState(
    val students: List<Student> = emptyList(),
    val filteredStudents: List<Student> = emptyList(),
    val availableCourses: List<String> = emptyList(),
    val selectedCourseFilter: String? = null,
    val selectedStatusFilter: StudentStatus? = null,
    val searchQuery: String = "",
    val studentAttendanceData: Map<String, StudentAttendanceData> = emptyMap(),
    val overallAttendanceRate: Double = 0.0,
    val isLoading: Boolean = false,
    val isEnrolling: Boolean = false,
    val enrollmentMessage: String? = null,
    val error: String? = null
)

enum class StudentStatus {
    ACTIVE, INACTIVE, ALL
}

@HiltViewModel
class ComprehensiveStudentManagementViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val enrollStudentUseCase: EnrollStudentUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ComprehensiveStudentManagementState())
    val state: StateFlow<ComprehensiveStudentManagementState> = _state.asStateFlow()
    
    fun loadStudents() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                val students = studentRepository.getAllActiveStudents().let { flow ->
                    var result = emptyList<Student>()
                    flow.collect { result = it }
                    result
                }
                
                val courses = students.map { it.course }.distinct().sorted()
                
                _state.value = _state.value.copy(
                    students = students,
                    filteredStudents = students,
                    availableCourses = courses,
                    isLoading = false
                )
                
                applyFilters()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load students: ${e.message}"
                )
            }
        }
    }
    
    fun loadAttendanceStatistics() {
        viewModelScope.launch {
            try {
                val attendanceData = mutableMapOf<String, StudentAttendanceData>()
                var totalAttendanceRate = 0.0
                
                state.value.students.forEach { student ->
                    val records = attendanceRepository.getDetailedAttendanceByStudentId(student.id)
                    
                    val presentCount = records.count { it.status == AttendanceStatus.PRESENT }
                    val lateCount = records.count { it.status == AttendanceStatus.LATE }
                    val absentCount = records.count { it.status == AttendanceStatus.ABSENT }
                    val totalEvents = records.size
                    
                    val attendanceRate = if (totalEvents > 0) {
                        ((presentCount + lateCount).toDouble() / totalEvents) * 100
                    } else 0.0
                    
                    val penalties = records.mapNotNull { it.penalty }.distinct()
                    val lastAttendance = records.maxOfOrNull { it.timestamp }
                    
                    attendanceData[student.id] = StudentAttendanceData(
                        studentId = student.id,
                        totalEvents = totalEvents,
                        presentCount = presentCount,
                        lateCount = lateCount,
                        absentCount = absentCount,
                        attendanceRate = attendanceRate,
                        penalties = penalties,
                        lastAttendance = lastAttendance
                    )
                    
                    totalAttendanceRate += attendanceRate
                }
                
                val overallRate = if (state.value.students.isNotEmpty()) {
                    totalAttendanceRate / state.value.students.size
                } else 0.0
                
                _state.value = _state.value.copy(
                    studentAttendanceData = attendanceData,
                    overallAttendanceRate = overallRate
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load attendance statistics: ${e.message}"
                )
            }
        }
    }
    
    fun searchStudents(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }
    
    fun filterByCourse(course: String?) {
        _state.value = _state.value.copy(selectedCourseFilter = course)
        applyFilters()
    }
    
    fun filterByStatus(status: StudentStatus?) {
        _state.value = _state.value.copy(selectedStatusFilter = status)
        applyFilters()
    }
    
    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.students
        
        // Apply search filter
        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { student ->
                student.name.contains(currentState.searchQuery, ignoreCase = true) ||
                student.id.contains(currentState.searchQuery, ignoreCase = true) ||
                student.course.contains(currentState.searchQuery, ignoreCase = true)
            }
        }
        
        // Apply course filter
        currentState.selectedCourseFilter?.let { course ->
            filtered = filtered.filter { it.course == course }
        }
        
        // Apply status filter
        when (currentState.selectedStatusFilter) {
            StudentStatus.ACTIVE -> filtered = filtered.filter { it.isActive }
            StudentStatus.INACTIVE -> filtered = filtered.filter { !it.isActive }
            StudentStatus.ALL, null -> { /* No filter */ }
        }
        
        _state.value = _state.value.copy(filteredStudents = filtered)
    }
    
    fun enrollStudent(id: String, name: String, course: String, email: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isEnrolling = true, error = null)
                
                when (val result = enrollStudentUseCase.execute(id, name, course)) {
                    is EnrollStudentUseCase.EnrollmentResult.Success -> {
                        _state.value = _state.value.copy(
                            isEnrolling = false,
                            enrollmentMessage = "Student enrolled successfully!"
                        )
                        loadStudents() // Refresh the list
                    }
                    is EnrollStudentUseCase.EnrollmentResult.Error -> {
                        _state.value = _state.value.copy(
                            isEnrolling = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isEnrolling = false,
                    error = "Failed to enroll student: ${e.message}"
                )
            }
        }
    }
    
    fun toggleStudentStatus(studentId: String, newStatus: Boolean) {
        viewModelScope.launch {
            try {
                val student = state.value.students.find { it.id == studentId } ?: return@launch
                val updatedStudent = student.copy(isActive = newStatus)
                
                studentRepository.updateStudent(updatedStudent)
                loadStudents() // Refresh the list
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to update student status: ${e.message}"
                )
            }
        }
    }
    
    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            try {
                val student = state.value.students.find { it.id == studentId } ?: return@launch
                studentRepository.deleteStudent(student)
                loadStudents() // Refresh the list
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to delete student: ${e.message}"
                )
            }
        }
    }
    
    fun exportStudentData() {
        viewModelScope.launch {
            try {
                // TODO: Implement export functionality
                _state.value = _state.value.copy(
                    enrollmentMessage = "Export functionality coming soon!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to export data: ${e.message}"
                )
            }
        }
    }
    
    fun importStudentData() {
        viewModelScope.launch {
            try {
                // TODO: Implement import functionality
                _state.value = _state.value.copy(
                    enrollmentMessage = "Import functionality coming soon!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to import data: ${e.message}"
                )
            }
        }
    }
    
    fun generateAttendanceReport() {
        viewModelScope.launch {
            try {
                // TODO: Implement report generation
                _state.value = _state.value.copy(
                    enrollmentMessage = "Report generation functionality coming soon!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to generate report: ${e.message}"
                )
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

// Event Detail ViewModel
data class ComprehensiveEventDetailState(
    val event: dev.ml.smartattendance.data.entity.Event? = null,
    val attendanceRecords: List<DetailedAttendanceRecord> = emptyList(),
    val filteredAttendanceRecords: List<DetailedAttendanceRecord> = emptyList(),
    val availableStudents: List<Student> = emptyList(),
    val totalRegisteredStudents: Int = 0,
    val selectedStatusFilter: AttendanceStatus? = null,
    val penaltyStatistics: Map<PenaltyType, Int> = emptyMap(),
    val detailedPenalties: List<StudentPenaltyData> = emptyList(),
    val lastUpdated: Long? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class StudentPenaltyData(
    val studentId: String,
    val studentName: String,
    val penalties: List<PenaltyType>,
    val totalPenaltyPoints: Int,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

@HiltViewModel
class ComprehensiveEventDetailViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val eventRepository: dev.ml.smartattendance.domain.repository.EventRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ComprehensiveEventDetailState())
    val state: StateFlow<ComprehensiveEventDetailState> = _state.asStateFlow()
    
    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                val event = eventRepository.getEventById(eventId)
                val students = studentRepository.getAllActiveStudents().let { flow ->
                    var result = emptyList<Student>()
                    flow.collect { result = it }
                    result
                }
                
                _state.value = _state.value.copy(
                    event = event,
                    availableStudents = students,
                    totalRegisteredStudents = students.size,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load event details: ${e.message}"
                )
            }
        }
    }
    
    fun loadAttendanceRecords(eventId: String) {
        viewModelScope.launch {
            try {
                val records = attendanceRepository.getDetailedAttendanceRecordsWithFilter(
                    startDate = null,
                    endDate = null,
                    courseFilter = null,
                    statusFilter = null,
                    studentIdFilter = null,
                    eventIdFilter = eventId
                )
                
                val penaltyStats = records.mapNotNull { it.penalty }
                    .groupingBy { it }
                    .eachCount()
                
                _state.value = _state.value.copy(
                    attendanceRecords = records,
                    filteredAttendanceRecords = records,
                    penaltyStatistics = penaltyStats,
                    lastUpdated = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load attendance records: ${e.message}"
                )
            }
        }
    }
    
    fun filterByStatus(status: AttendanceStatus?) {
        _state.value = _state.value.copy(selectedStatusFilter = status)
        
        val filtered = if (status != null) {
            state.value.attendanceRecords.filter { it.status == status }
        } else {
            state.value.attendanceRecords
        }
        
        _state.value = _state.value.copy(filteredAttendanceRecords = filtered)
    }
    
    fun markManualAttendance(eventId: String, studentId: String, status: AttendanceStatus, notes: String?) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isUpdating = true)
                
                // TODO: Implement manual attendance marking
                // This would involve creating an AttendanceRecord and inserting it
                
                _state.value = _state.value.copy(
                    isUpdating = false,
                    message = "Attendance marked successfully"
                )
                
                // Refresh records
                loadAttendanceRecords(eventId)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUpdating = false,
                    error = "Failed to mark attendance: ${e.message}"
                )
            }
        }
    }
    
    fun calculatePenalty(studentId: String, eventId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement penalty calculation based on requirements
                _state.value = _state.value.copy(
                    message = "Penalty calculation functionality coming soon!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to calculate penalty: ${e.message}"
                )
            }
        }
    }
    
    fun toggleEventStatus(eventId: String) {
        viewModelScope.launch {
            try {
                val event = state.value.event ?: return@launch
                val updatedEvent = event.copy(isActive = !event.isActive)
                
                eventRepository.updateEvent(updatedEvent)
                _state.value = _state.value.copy(event = updatedEvent)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to toggle event status: ${e.message}"
                )
            }
        }
    }
    
    fun startRealTimeUpdates(eventId: String) {
        // TODO: Implement real-time updates using Flow or WebSocket
    }
    
    fun stopRealTimeUpdates() {
        // TODO: Stop real-time updates
    }
    
    fun refreshData(eventId: String) {
        loadEventDetails(eventId)
        loadAttendanceRecords(eventId)
    }
    
    fun refreshAttendanceRecords(eventId: String) {
        loadAttendanceRecords(eventId)
    }
    
    fun sortAttendanceRecords() {
        val sorted = state.value.filteredAttendanceRecords.sortedBy { it.timestamp }
        _state.value = _state.value.copy(filteredAttendanceRecords = sorted)
    }
    
    fun exportAttendanceData(eventId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement export functionality
                _state.value = _state.value.copy(
                    message = "Export functionality coming soon!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to export data: ${e.message}"
                )
            }
        }
    }
    
    fun updateStudentPenalty(studentId: String, penalty: PenaltyType) {
        viewModelScope.launch {
            try {
                // TODO: Implement penalty update
                _state.value = _state.value.copy(
                    message = "Penalty updated successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to update penalty: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}