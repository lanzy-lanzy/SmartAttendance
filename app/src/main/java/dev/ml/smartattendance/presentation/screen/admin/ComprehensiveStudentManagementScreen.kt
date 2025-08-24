package dev.ml.smartattendance.presentation.screen.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.presentation.viewmodel.admin.ComprehensiveStudentManagementViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveStudentManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudentDetail: (String) -> Unit,
    viewModel: ComprehensiveStudentManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkActionsSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadStudents()
        viewModel.loadAttendanceStatistics()
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Student Management",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Search
                    IconButton(onClick = { /* Toggle search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    // Filter
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    // More actions
                    IconButton(onClick = { showBulkActionsSheet = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Student") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with statistics
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StudentStatisticsCard(
                    totalStudents = state.students.size,
                    activeStudents = state.students.count { it.isActive },
                    coursesCount = state.students.map { it.course }.distinct().size,
                    attendanceRate = state.overallAttendanceRate
                )
            }
            
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchStudents(it)
                    },
                    label = { Text("Search students...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                viewModel.searchStudents("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            // Course filter chips
            item {
                CourseFilterChips(
                    courses = state.availableCourses,
                    selectedCourse = state.selectedCourseFilter,
                    onCourseSelected = { viewModel.filterByCourse(it) }
                )
            }
            
            // Students list
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.filteredStudents.isEmpty()) {
                item {
                    EmptyStudentsCard()
                }
            } else {
                items(state.filteredStudents) { student ->
                    ComprehensiveStudentCard(
                        student = student,
                        attendanceData = state.studentAttendanceData[student.id],
                        onStudentClick = { onNavigateToStudentDetail(student.id) },
                        onToggleActiveStatus = { viewModel.toggleStudentStatus(student.id, !student.isActive) },
                        onDeleteStudent = { viewModel.deleteStudent(student.id) }
                    )
                }
            }
        }
    }
    
    // Add Student Dialog
    if (showAddDialog) {
        EnhancedAddStudentDialog(
            isLoading = state.isEnrolling,
            courses = state.availableCourses,
            onDismiss = { showAddDialog = false },
            onAddStudent = { id, name, course, email ->
                viewModel.enrollStudent(id, name, course, email)
                showAddDialog = false
            }
        )
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            courses = state.availableCourses,
            selectedCourse = state.selectedCourseFilter,
            selectedStatus = state.selectedStatusFilter,
            onCourseFilterChanged = { viewModel.filterByCourse(it) },
            onStatusFilterChanged = { viewModel.filterByStatus(it) },
            onDismiss = { showFilterSheet = false }
        )
    }
    
    // Bulk Actions Bottom Sheet
    if (showBulkActionsSheet) {
        BulkActionsBottomSheet(
            onExportData = { viewModel.exportStudentData() },
            onImportData = { viewModel.importStudentData() },
            onGenerateReport = { viewModel.generateAttendanceReport() },
            onDismiss = { showBulkActionsSheet = false }
        )
    }
}

@Composable
fun StudentStatisticsCard(
    totalStudents: Int,
    activeStudents: Int,
    coursesCount: Int,
    attendanceRate: Double
) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    title = "Total",
                    value = totalStudents.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatisticItem(
                    title = "Active",
                    value = activeStudents.toString(),
                    icon = Icons.Default.PersonOutline,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatisticItem(
                    title = "Courses",
                    value = coursesCount.toString(),
                    icon = Icons.Default.School,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatisticItem(
                    title = "Attendance",
                    value = "${String.format("%.1f", attendanceRate)}%",
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ComprehensiveStudentCard(
    student: Student,
    attendanceData: StudentAttendanceData?,
    onStudentClick: () -> Unit,
    onToggleActiveStatus: () -> Unit,
    onDeleteStudent: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStudentClick() }
    ) {
        Column {
            // Main student info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (student.isActive) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ID: ${student.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Course: ${student.course}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Enrolled: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(student.enrollmentDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Actions button
                IconButton(onClick = { showActions = !showActions }) {
                    Icon(
                        imageVector = if (showActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Actions"
                    )
                }
            }
            
            // Attendance summary
            attendanceData?.let { data ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttendanceMetric(
                        label = "Present",
                        value = data.presentCount.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    AttendanceMetric(
                        label = "Late",
                        value = data.lateCount.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    AttendanceMetric(
                        label = "Absent",
                        value = data.absentCount.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                    AttendanceMetric(
                        label = "Rate",
                        value = "${String.format("%.1f", data.attendanceRate)}%",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // Penalty indicators
                if (data.penalties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PenaltyIndicators(penalties = data.penalties)
                }
            }
            
            // Expanded actions
            if (showActions) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = onToggleActiveStatus,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (student.isActive) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (student.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (student.isActive) "Deactivate" else "Activate")
                    }
                    
                    TextButton(onClick = onDeleteStudent) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// Data classes for student analytics
data class StudentAttendanceData(
    val studentId: String,
    val totalEvents: Int,
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val attendanceRate: Double,
    val penalties: List<PenaltyType>,
    val lastAttendance: Long?
)

@Composable
fun AttendanceMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PenaltyIndicators(penalties: List<PenaltyType>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        penalties.distinct().forEach { penalty ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (penalty) {
                    PenaltyType.WARNING -> MaterialTheme.colorScheme.secondaryContainer
                    PenaltyType.MINOR -> MaterialTheme.colorScheme.tertiaryContainer
                    PenaltyType.MAJOR -> MaterialTheme.colorScheme.errorContainer
                    PenaltyType.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                }
            ) {
                Text(
                    text = penalty.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (penalty) {
                        PenaltyType.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
                        PenaltyType.MINOR -> MaterialTheme.colorScheme.onTertiaryContainer
                        PenaltyType.MAJOR -> MaterialTheme.colorScheme.onErrorContainer
                        PenaltyType.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}