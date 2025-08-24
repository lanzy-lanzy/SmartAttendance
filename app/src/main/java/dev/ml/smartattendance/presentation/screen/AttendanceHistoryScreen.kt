package dev.ml.smartattendance.presentation.screen

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
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.viewmodel.AttendanceHistoryViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceHistoryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAttendanceHistory: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    viewModel: AttendanceHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Load attendance history on composition
    LaunchedEffect(Unit) {
        viewModel.loadAttendanceHistory()
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Attendance History",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { /* TODO: Filter options */ }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = "attendance_history",
                userRole = UserRole.STUDENT,
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToDashboard()
                        "attendance_history" -> { /* Already here */ }
                        "events" -> onNavigateToEvents()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Clean Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Your Attendance Records",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Track your attendance across all events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Summary Stats
                if (state.attendanceRecords.isNotEmpty()) {
                    val totalRecords = state.attendanceRecords.size
                    val presentCount = state.attendanceRecords.count { it.status == AttendanceStatus.PRESENT }
                    val attendanceRate = (presentCount * 100) / totalRecords
                    
                    ModernCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                title = "Total Events",
                                value = totalRecords.toString(),
                                icon = Icons.Default.Event
                            )
                            
                            StatItem(
                                title = "Present",
                                value = presentCount.toString(),
                                icon = Icons.Default.CheckCircle
                            )
                            
                            StatItem(
                                title = "Attendance Rate",
                                value = "${attendanceRate}%",
                                icon = Icons.Default.TrendingUp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Records List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Records",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Records List
                if (state.attendanceRecords.isEmpty()) {
                    EmptyAttendanceCard()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.attendanceRecords) { record ->
                            AttendanceRecordCard(record = record)
                        }
                    }
                }
                
                // Error handling
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = error,
                        type = AlertType.Error
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyAttendanceCard() {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Attendance Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your attendance records will appear here once you start marking attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AttendanceRecordCard(record: DetailedAttendanceRecord) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (record.status) {
                    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
                    AttendanceStatus.LATE -> MaterialTheme.colorScheme.tertiaryContainer
                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    imageVector = when (record.status) {
                        AttendanceStatus.PRESENT -> Icons.Default.CheckCircle
                        AttendanceStatus.LATE -> Icons.Default.Schedule
                        AttendanceStatus.ABSENT -> Icons.Default.Cancel
                        else -> Icons.Default.HelpOutline
                    },
                    contentDescription = record.status.name,
                    tint = when (record.status) {
                        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.onPrimaryContainer
                        AttendanceStatus.LATE -> MaterialTheme.colorScheme.onTertiaryContainer
                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Record Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.eventName, // Now showing event name instead of ID
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${dateFormat.format(Date(record.timestamp))} at ${timeFormat.format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status Badge
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
        }
    }
}