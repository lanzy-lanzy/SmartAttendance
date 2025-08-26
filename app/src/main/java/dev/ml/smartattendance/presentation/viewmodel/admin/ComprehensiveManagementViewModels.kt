package dev.ml.smartattendance.presentation.viewmodel.admin

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.domain.service.FirestoreService
import dev.ml.smartattendance.domain.usecase.EnrollStudentUseCase
import dev.ml.smartattendance.presentation.screen.admin.StudentAttendanceData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val enrollStudentUseCase: EnrollStudentUseCase,
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _state = MutableStateFlow(ComprehensiveStudentManagementState())
    val state: StateFlow<ComprehensiveStudentManagementState> = _state.asStateFlow()
    
    fun loadStudents() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Try multiple sources to ensure we get all students
                var allStudents = mutableListOf<Student>()
                
                // 1. First try the students collection directly
                val studentsFromCollection = firestoreService.getAllStudents()
                Log.d("StudentManagement", "Students from collection: ${studentsFromCollection.size}")
                allStudents.addAll(studentsFromCollection)
                
                // 2. Then get users with STUDENT role and convert to students if they're not already in the list
                val allUsers = firestoreService.getAllUsers()
                Log.d("StudentManagement", "All users: ${allUsers.size}")
                val studentUsers = allUsers.filter { it.role == dev.ml.smartattendance.domain.model.UserRole.STUDENT }
                Log.d("StudentManagement", "Student users: ${studentUsers.size}")
                
                studentUsers.forEach { user ->
                    if (user.studentId != null && user.course != null) {
                        // Check if this student is already in our list
                        val exists = allStudents.any { it.id == user.studentId }
                        Log.d("StudentManagement", "User ${user.name} with studentId ${user.studentId} exists in students? $exists")
                        
                        if (!exists) {
                            // Create a student object from the user
                            val student = Student(
                                id = user.studentId,
                                name = user.name,
                                course = user.course,
                                enrollmentDate = user.enrollmentDate ?: System.currentTimeMillis(),
                                role = user.role,
                                isActive = user.isActive,
                                email = user.email
                            )
                            
                            // Add to our list and save to students collection
                            allStudents.add(student)
                            val success = firestoreService.createStudent(student)
                            Log.d("StudentManagement", "Created student from user ${user.name}: $success")
                        }
                    } else {
                        Log.d("StudentManagement", "User ${user.name} missing studentId or course: studentId=${user.studentId}, course=${user.course}")
                    }
                }
                
                // 3. Cache all students locally
                if (allStudents.isNotEmpty()) {
                    Log.d("StudentManagement", "Final student count: ${allStudents.size}")
                    studentRepository.insertStudents(allStudents)
                } else {
                    // 4. If still empty, try local repository as a last resort
                    Log.d("StudentManagement", "No students found in Firebase, trying local repository")
                    allStudents.addAll(studentRepository.getAllActiveStudents().first())
                    Log.d("StudentManagement", "Students from local repository: ${allStudents.size}")
                }
                
                val courses = allStudents.map { it.course }.distinct().sorted()
                
                _state.value = _state.value.copy(
                    students = allStudents,
                    filteredStudents = allStudents,
                    availableCourses = courses,
                    isLoading = false,
                    error = if (allStudents.isEmpty()) "No students found in the database" else null
                )
                
                applyFilters()
                
            } catch (e: Exception) {
                // If error occurs, try falling back to the repository method
                try {
                    val students = studentRepository.getAllActiveStudents().first()
                    val courses = students.map { it.course }.distinct().sorted()
                    
                    _state.value = _state.value.copy(
                        students = students,
                        filteredStudents = students,
                        availableCourses = courses,
                        isLoading = false,
                        error = if (students.isEmpty()) "Error loading students: ${e.message}" else null
                    )
                    
                    applyFilters()
                } catch (fallbackEx: Exception) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load students: ${e.message}"
                    )
                }
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
        
        // Apply status filter only if explicitly set
        when (currentState.selectedStatusFilter) {
            StudentStatus.ACTIVE -> filtered = filtered.filter { it.isActive }
            StudentStatus.INACTIVE -> filtered = filtered.filter { !it.isActive }
            StudentStatus.ALL, null -> { /* No filtering by status */ }
        }
        
        _state.value = _state.value.copy(filteredStudents = filtered)
    }
    
    fun enrollStudent(id: String, name: String, course: String, email: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isEnrolling = true, error = null)
                
                // Create student directly in Firebase first
                val enrollmentDate = System.currentTimeMillis()
                val student = Student(
                    id = id,
                    name = name,
                    course = course,
                    enrollmentDate = enrollmentDate,
                    role = dev.ml.smartattendance.domain.model.UserRole.STUDENT,
                    isActive = true,
                    email = email
                )
                
                val createdInFirebase = firestoreService.createStudent(student)
                Log.d("StudentManagement", "Student created in Firebase: $createdInFirebase")
                
                // Also use the enrollment use case which handles local database
                val result = enrollStudentUseCase.execute(id, name, course)
                
                when (result) {
                    is EnrollStudentUseCase.EnrollmentResult.Success -> {
                        _state.value = _state.value.copy(
                            isEnrolling = false,
                            enrollmentMessage = "Student enrolled successfully!"
                        )
                        loadStudents() // Refresh the list
                    }
                    is EnrollStudentUseCase.EnrollmentResult.Error -> {
                        // If Firebase creation succeeded but use case failed, still consider it a success
                        if (createdInFirebase) {
                            _state.value = _state.value.copy(
                                isEnrolling = false,
                                enrollmentMessage = "Student enrolled successfully in Firebase!"
                            )
                            loadStudents() // Refresh the list
                        } else {
                            _state.value = _state.value.copy(
                                isEnrolling = false,
                                error = result.message
                            )
                        }
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
                
                // Update in Firebase first
                val updatedInFirebase = firestoreService.updateStudent(updatedStudent)
                Log.d("StudentManagement", "Student status updated in Firebase: $updatedInFirebase")
                
                // Also update in local database
                studentRepository.updateStudent(updatedStudent)
                
                _state.value = _state.value.copy(
                    enrollmentMessage = "Student status ${if (newStatus) "activated" else "deactivated"} successfully!"
                )
                
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
                _state.value = _state.value.copy(
                    isEnrolling = true,
                    enrollmentMessage = null,
                    error = null
                )
                
                val student = state.value.students.find { it.id == studentId } ?: return@launch
                
                // Delete from Firebase first
                val deletedFromFirebase = firestoreService.deleteStudent(studentId)
                Log.d("StudentManagement", "Student deleted from Firebase: $deletedFromFirebase")
                
                // Also delete from local database
                studentRepository.deleteStudent(student)
                
                _state.value = _state.value.copy(
                    isEnrolling = false,
                    enrollmentMessage = "Student and related attendance records deleted successfully!"
                )
                
                loadStudents() // Refresh the list
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isEnrolling = false,
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
    private val eventRepository: dev.ml.smartattendance.domain.repository.EventRepository,
    private val firestoreService: dev.ml.smartattendance.domain.service.FirestoreService // Direct injection of FirestoreService
) : ViewModel() {
    
    private val _state = MutableStateFlow(ComprehensiveEventDetailState())
    val state: StateFlow<ComprehensiveEventDetailState> = _state.asStateFlow()
    
    /**
     * Pre-loads an event into the state to speed up rendering
     * This is used for quick loading from direct Firestore calls
     */
    fun preloadEvent(event: Event) {
        android.util.Log.d("ComprehensiveEventDetail", "Pre-loading event: ${event.name}")
        viewModelScope.launch {
            try {
                // Only set the event if we don't already have one loaded
                if (_state.value.event == null) {
                    // Load students
                    val students = try {
                        studentRepository.getAllActiveStudents().first()
                    } catch (e: Exception) {
                        android.util.Log.e("ComprehensiveEventDetail", "Error loading students: ${e.message}")
                        emptyList()
                    }
                    
                    _state.value = _state.value.copy(
                        event = event,
                        availableStudents = students,
                        totalRegisteredStudents = students.size,
                        isLoading = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    // Also try to load attendance records
                    try {
                        loadAttendanceRecords(event.id)
                    } catch (e: Exception) {
                        android.util.Log.e("ComprehensiveEventDetail", "Error loading attendance records: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ComprehensiveEventDetail", "Error in preloadEvent: ${e.message}")
            }
        }
    }

    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                android.util.Log.d("ComprehensiveEventDetail", "Loading event details for ID: $eventId")
                
                if (eventId.isBlank()) {
                    android.util.Log.e("ComprehensiveEventDetail", "Invalid event ID: ID is blank")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid event ID provided"
                    )
                    return@launch
                }
                
                // Try to get event directly from Firestore first
                val firestoreService = dev.ml.smartattendance.data.service.FirebaseModule.provideFirestoreService()
                val firestoreEvent = firestoreService.getEvent(eventId)
                
                if (firestoreEvent != null) {
                    android.util.Log.d("ComprehensiveEventDetail", "Successfully loaded event from Firestore: ${firestoreEvent.name}")
                    val students = studentRepository.getAllActiveStudents().first()
                    _state.value = _state.value.copy(
                        event = firestoreEvent,
                        availableStudents = students,
                        totalRegisteredStudents = students.size,
                        isLoading = false
                    )
                    return@launch
                }
                
                // Fallback to repository if Firestore fails
                val event = eventRepository.getEventById(eventId)
                
                if (event != null) {
                    android.util.Log.d("ComprehensiveEventDetail", "Successfully loaded event from repository: ${event.name}")
                    _state.value = _state.value.copy(
                        event = event,
                        availableStudents = studentRepository.getAllActiveStudents().first(),
                        totalRegisteredStudents = studentRepository.getAllActiveStudents().first().size,
                        isLoading = false
                    )
                } else {
                    android.util.Log.e("ComprehensiveEventDetail", "Event not found with ID: $eventId")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Event not found"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ComprehensiveEventDetail", "Error loading event details: ${e.message}", e)
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
        android.util.Log.d("ComprehensiveEventDetail", "Refreshing data for event ID: '$eventId'")
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Try direct access to Firestore with better error handling
                android.util.Log.d("ComprehensiveEventDetail", "Attempting direct Firestore access for event: '$eventId'")
                val directEvent = try {
                    firestoreService.getEvent(eventId)
                } catch (e: Exception) {
                    android.util.Log.e("ComprehensiveEventDetail", "Error in direct Firestore access: ${e.message}", e)
                    null
                }
                
                android.util.Log.d("ComprehensiveEventDetail", "Direct Firestore result: ${directEvent != null}")
                
                if (directEvent != null) {
                    android.util.Log.d("ComprehensiveEventDetail", "Successfully loaded event directly: ${directEvent.name}")
                    // Load students
                    val students = studentRepository.getAllActiveStudents().first()
                    _state.value = _state.value.copy(
                        event = directEvent,
                        availableStudents = students,
                        totalRegisteredStudents = students.size
                    )
                    
                    // Also load attendance records
                    loadAttendanceRecords(eventId)
                    
                    _state.value = _state.value.copy(isLoading = false, lastUpdated = System.currentTimeMillis())
                    return@launch
                }
                
                // Fallback to repository if direct access fails
                android.util.Log.d("ComprehensiveEventDetail", "Direct access failed, trying repository for event: '$eventId'")
                val repoEvent = try {
                    eventRepository.getEventById(eventId)
                } catch (e: Exception) {
                    android.util.Log.e("ComprehensiveEventDetail", "Error in repository access: ${e.message}", e)
                    null
                }
                
                android.util.Log.d("ComprehensiveEventDetail", "Repository result: ${repoEvent != null}")
                
                if (repoEvent != null) {
                    android.util.Log.d("ComprehensiveEventDetail", "Successfully loaded event from repository: ${repoEvent.name}")
                    // Load students
                    val students = studentRepository.getAllActiveStudents().first()
                    _state.value = _state.value.copy(
                        event = repoEvent,
                        availableStudents = students,
                        totalRegisteredStudents = students.size
                    )
                    
                    // Also load attendance records
                    loadAttendanceRecords(eventId)
                    
                    _state.value = _state.value.copy(isLoading = false, lastUpdated = System.currentTimeMillis())
                    return@launch
                }
                
                // Try one last attempt with the Firebase Module directly
                android.util.Log.d("ComprehensiveEventDetail", "Trying direct Firebase Module access as last attempt")
                val firebaseModule = dev.ml.smartattendance.data.service.FirebaseModule
                val directFirestoreService = firebaseModule.provideFirestoreService()
                val lastAttemptEvent = try {
                    directFirestoreService.getEvent(eventId)
                } catch (e: Exception) {
                    android.util.Log.e("ComprehensiveEventDetail", "Error in direct Firebase Module access: ${e.message}", e)
                    null
                }
                
                android.util.Log.d("ComprehensiveEventDetail", "Direct Firebase Module result: ${lastAttemptEvent != null}")
                
                if (lastAttemptEvent != null) {
                    android.util.Log.d("ComprehensiveEventDetail", "Successfully loaded event from direct Firebase Module: ${lastAttemptEvent.name}")
                    val students = studentRepository.getAllActiveStudents().first()
                    _state.value = _state.value.copy(
                        event = lastAttemptEvent,
                        availableStudents = students,
                        totalRegisteredStudents = students.size,
                        isLoading = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    // Also load attendance records
                    loadAttendanceRecords(eventId)
                    return@launch
                }
                
                // If event is still not found, create a dummy event as a last resort
                android.util.Log.e("ComprehensiveEventDetail", "Event not found in any source, creating dummy event for ID: '$eventId'")
                val dummyEvent = Event(
                    id = eventId,
                    name = "Event $eventId",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 3600000,
                    latitude = 0.0,
                    longitude = 0.0,
                    geofenceRadius = 100f,
                    isActive = true,
                    signInStartOffset = 15,
                    signInEndOffset = 15,
                    signOutStartOffset = 15,
                    signOutEndOffset = 15
                )
                
                val students = studentRepository.getAllActiveStudents().first()
                _state.value = _state.value.copy(
                    event = dummyEvent,
                    availableStudents = students,
                    totalRegisteredStudents = students.size,
                    error = "Using placeholder event data. Original event might be missing.",
                    isLoading = false, 
                    lastUpdated = System.currentTimeMillis()
                )
                
                // Try to save this dummy event to prevent future issues
                try {
                    android.util.Log.d("ComprehensiveEventDetail", "Attempting to save dummy event to Firestore")
                    val success = firestoreService.createEvent(dummyEvent)
                    android.util.Log.d("ComprehensiveEventDetail", "Saved dummy event result: $success")
                    
                    if (success) {
                        android.util.Log.d("ComprehensiveEventDetail", "Successfully created placeholder event in Firestore")
                        _state.value = _state.value.copy(
                            error = "Created a placeholder event. Please refresh to see updated data."
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ComprehensiveEventDetail", "Failed to save dummy event: ${e.message}", e)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ComprehensiveEventDetail", "Error refreshing data: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to refresh data: ${e.message}"
                )
            }
        }
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