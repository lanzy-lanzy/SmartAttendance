package dev.ml.smartattendance.presentation.state

import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.UserRole

data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUser: dev.ml.smartattendance.data.entity.User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AttendanceState(
    val currentEvents: List<Event> = emptyList(),
    val isMarkingAttendance: Boolean = false,
    val attendanceMessage: String? = null,
    val error: String? = null
)

data class StudentManagementState(
    val students: List<Student> = emptyList(),
    val isLoading: Boolean = false,
    val isEnrolling: Boolean = false,
    val enrollmentMessage: String? = null,
    val error: String? = null
)

data class EventManagementState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val creationMessage: String? = null,
    val error: String? = null
)

data class DashboardState(
    val userRole: UserRole = UserRole.STUDENT,
    val currentEvents: List<Event> = emptyList(),
    val totalStudents: Int = 0,
    val totalEvents: Int = 0,
    val isLoading: Boolean = false
)