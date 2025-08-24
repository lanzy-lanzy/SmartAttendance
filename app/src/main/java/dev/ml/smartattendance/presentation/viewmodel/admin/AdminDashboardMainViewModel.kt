package dev.ml.smartattendance.presentation.viewmodel.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.repository.StudentRepository
import dev.ml.smartattendance.presentation.screen.admin.AdminActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AdminDashboardMainState(
    val totalStudents: Int = 0,
    val activeStudents: Int = 0,
    val totalEvents: Int = 0,
    val activeEvents: Int = 0,
    val todayAttendanceRate: Double = 0.0,
    val todayPresentCount: Int = 0,
    val pendingIssues: Int = 0,
    val recentActivities: List<AdminActivity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminDashboardMainViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AdminDashboardMainState())
    val state: StateFlow<AdminDashboardMainState> = _state.asStateFlow()
    
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Load student statistics
                val allStudents = studentRepository.getAllActiveStudents().first()
                val totalStudents = allStudents.size
                val activeStudents = allStudents.count { it.isActive }
                
                // Load event statistics
                val allEvents = eventRepository.getAllEvents().first()
                val totalEvents = allEvents.size
                val activeEvents = allEvents.count { it.isActive }
                
                // Load today's attendance data (simplified for demo)
                val todayStart = LocalDate.now().atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                val todayEnd = LocalDate.now().atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                
                val todayAttendance = try {
                    attendanceRepository.getAttendanceInDateRange(todayStart, todayEnd)
                } catch (e: Exception) {
                    emptyList()
                }
                
                val todayPresentCount = todayAttendance.size
                val todayAttendanceRate = if (totalStudents > 0) {
                    (todayPresentCount.toDouble() / totalStudents) * 100
                } else 0.0
                
                // Generate recent activities (mock data for now)
                val recentActivities = generateRecentActivities()
                
                // Calculate pending issues (mock data)
                val pendingIssues = calculatePendingIssues()
                
                _state.value = _state.value.copy(
                    totalStudents = totalStudents,
                    activeStudents = activeStudents,
                    totalEvents = totalEvents,
                    activeEvents = activeEvents,
                    todayAttendanceRate = todayAttendanceRate,
                    todayPresentCount = todayPresentCount,
                    pendingIssues = pendingIssues,
                    recentActivities = recentActivities,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }
    
    private fun generateRecentActivities(): List<AdminActivity> {
        // For now, generate some mock recent activities
        // In a real implementation, this would come from an activity log or audit trail
        return listOf(
            AdminActivity(
                title = "New Student Enrolled",
                description = "John Doe was enrolled in Computer Science",
                timeAgo = "5 minutes ago",
                icon = Icons.Default.PersonAdd,
                color = Color(0xFF4CAF50)
            ),
            AdminActivity(
                title = "Event Created",
                description = "Morning Lecture event was created",
                timeAgo = "1 hour ago",
                icon = Icons.Default.Event,
                color = Color(0xFF2196F3)
            ),
            AdminActivity(
                title = "Attendance Marked",
                description = "25 students marked attendance for CS101",
                timeAgo = "2 hours ago",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            ),
            AdminActivity(
                title = "Report Generated",
                description = "Weekly attendance report was generated",
                timeAgo = "3 hours ago",
                icon = Icons.Default.Assessment,
                color = Color(0xFF9C27B0)
            ),
            AdminActivity(
                title = "System Settings Updated",
                description = "Attendance time window was modified",
                timeAgo = "5 hours ago",
                icon = Icons.Default.Settings,
                color = Color(0xFF607D8B)
            )
        )
    }
    
    private fun calculatePendingIssues(): Int {
        // Mock calculation of pending issues
        // In a real implementation, this would check for:
        // - Students with high penalty points
        // - Events with low attendance
        // - System alerts
        // - Failed attendance records
        return 3
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}