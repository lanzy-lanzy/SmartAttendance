package dev.ml.smartattendance.presentation.screen.admin

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
import dev.ml.smartattendance.data.dao.DetailedAttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.presentation.viewmodel.admin.ComprehensiveEventDetailViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveEventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: ComprehensiveEventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<DetailedAttendanceRecord?>(null) }
    
    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
        viewModel.loadAttendanceRecords(eventId)
        viewModel.startRealTimeUpdates(eventId)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRealTimeUpdates()
        }
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Event Details",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Refresh
                    IconButton(onClick = { viewModel.refreshData(eventId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // Export
                    IconButton(onClick = { viewModel.exportAttendanceData(eventId) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    // Settings
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.event != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAttendanceDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Mark Attendance") }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Event Not Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The requested event could not be found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val event = state.event!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                // Event Information Card
                item {
                    ComprehensiveEventInfoCard(
                        event = event,
                        onToggleStatus = { viewModel.toggleEventStatus(eventId) }
                    )
                }
                
                // Real-time Attendance Statistics
                item {
                    RealTimeAttendanceStatistics(
                        totalRegistered = state.totalRegisteredStudents,
                        presentCount = state.attendanceRecords.count { it.status == AttendanceStatus.PRESENT },
                        lateCount = state.attendanceRecords.count { it.status == AttendanceStatus.LATE },
                        absentCount = state.totalRegisteredStudents - state.attendanceRecords.size,
                        lastUpdated = state.lastUpdated
                    )
                }
                
                // Penalty Overview
                item {
                    PenaltyOverviewCard(
                        penalties = state.penaltyStatistics,
                        onViewPenalties = { showPenaltyDialog = true }
                    )
                }
                
                // Attendance Status Filter
                item {
                    AttendanceStatusFilter(
                        selectedStatus = state.selectedStatusFilter,
                        onStatusChanged = { viewModel.filterByStatus(it) }
                    )
                }
                
                // Attendance Records Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance Records (${state.filteredAttendanceRecords.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Row {
                            IconButton(onClick = { viewModel.sortAttendanceRecords() }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            IconButton(onClick = { viewModel.refreshAttendanceRecords(eventId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                }
                
                // Attendance Records List
                if (state.filteredAttendanceRecords.isEmpty()) {
                    item {
                        EmptyAttendanceCard()
                    }
                } else {
                    items(state.filteredAttendanceRecords) { record ->
                        ComprehensiveAttendanceRecordCard(
                            record = record,
                            onEditAttendance = { 
                                selectedStudent = record
                                showAttendanceDialog = true 
                            },
                            onViewDetails = { /* Navigate to student detail */ },
                            onCalculatePenalty = { viewModel.calculatePenalty(record.studentId, eventId) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB space
            }
        }
    }
    
    // Manual Attendance Dialog
    if (showAttendanceDialog) {
        ManualAttendanceDialog(
            availableStudents = state.availableStudents,
            selectedStudent = selectedStudent,
            onDismiss = { 
                showAttendanceDialog = false
                selectedStudent = null
            },
            onMarkAttendance = { studentId, status, notes ->
                viewModel.markManualAttendance(eventId, studentId, status, notes)
                showAttendanceDialog = false
                selectedStudent = null
            }
        )
    }
    
    // Penalty Details Dialog
    if (showPenaltyDialog) {
        PenaltyDetailsDialog(
            penalties = state.detailedPenalties,
            onDismiss = { showPenaltyDialog = false },
            onUpdatePenalty = { studentId, penalty ->
                viewModel.updateStudentPenalty(studentId, penalty)
            }
        )
    }
}

@Composable
fun ComprehensiveEventInfoCard(
    event: Event,
    onToggleStatus: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Event ID: ${event.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (event.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (event.isActive) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = event.isActive,
                        onCheckedChange = { onToggleStatus() }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Event details grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EventDetailItem(
                        icon = Icons.Default.DateRange,
                        label = "Date",
                        value = dateFormat.format(Date(event.startTime))
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EventDetailItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = "${event.latitude}, ${event.longitude}"
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    EventDetailItem(
                        icon = Icons.Default.AccessTime,
                        label = "Time",
                        value = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EventDetailItem(
                        icon = Icons.Default.MyLocation,
                        label = "Radius",
                        value = "${event.geofenceRadius}m"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sign-in windows
            AttendanceWindowInfo(
                signInStart = event.signInStartOffset,
                signInEnd = event.signInEndOffset,
                signOutStart = event.signOutStartOffset,
                signOutEnd = event.signOutEndOffset
            )
        }
    }
}

@Composable
fun RealTimeAttendanceStatistics(
    totalRegistered: Int,
    presentCount: Int,
    lateCount: Int,
    absentCount: Int,
    lastUpdated: Long?
) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                lastUpdated?.let {
                    Text(
                        text = "Updated ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttendanceStatItem(
                    label = "Registered",
                    count = totalRegistered,
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                AttendanceStatItem(
                    label = "Present",
                    count = presentCount,
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
                
                AttendanceStatItem(
                    label = "Late",
                    count = lateCount,
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                AttendanceStatItem(
                    label = "Absent",
                    count = absentCount,
                    icon = Icons.Default.Cancel,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attendance rate progress
            val attendanceRate = if (totalRegistered > 0) {
                ((presentCount + lateCount).toFloat() / totalRegistered) * 100
            } else 0f
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attendance Rate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${String.format("%.1f", attendanceRate)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { attendanceRate / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ComprehensiveAttendanceRecordCard(
    record: DetailedAttendanceRecord,
    onEditAttendance: () -> Unit,
    onViewDetails: () -> Unit,
    onCalculatePenalty: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    var showActions by remember { mutableStateOf(false) }
    
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Student info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "ID: ${record.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Course: ${record.studentCourse}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status and actions
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (record.status) {
                            AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
                            AttendanceStatus.LATE -> MaterialTheme.colorScheme.tertiaryContainer
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = record.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (record.status) {
                                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                AttendanceStatus.LATE -> MaterialTheme.colorScheme.onTertiaryContainer
                                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    IconButton(
                        onClick = { showActions = !showActions },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Actions",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Attendance details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time: ${timeFormat.format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                record.penalty?.let { penalty ->
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
            
            // Location info if available
            if (record.latitude != null && record.longitude != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Location: ${String.format("%.6f", record.latitude)}, ${String.format("%.6f", record.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Notes if available
            record.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    TextButton(onClick = onEditAttendance) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    
                    TextButton(onClick = onViewDetails) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Details")
                    }
                    
                    TextButton(onClick = onCalculatePenalty) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Penalty")
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceStatItem(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = count.toString(),
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