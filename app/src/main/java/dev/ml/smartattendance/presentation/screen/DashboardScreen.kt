package dev.ml.smartattendance.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    userRole: UserRole,
    onNavigateToStudentManagement: () -> Unit = {},
    onNavigateToEventManagement: () -> Unit = {},
    onNavigateToAttendance: (String) -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToAttendanceHistory: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Load events on composition
    LaunchedEffect(Unit) {
        if (userRole == UserRole.ADMIN) {
            onNavigateToAdminDashboard()
        }
        viewModel.loadCurrentEvents()
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = if (userRole == UserRole.STUDENT) "Dashboard" else "SmartAttendance",
                onNavigateBack = { /* Dashboard doesn't need back navigation */ },
                actions = {
                    IconButton(onClick = { /* TODO: Profile/Settings */ }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (userRole == UserRole.STUDENT) {
                ModernBottomNavigation(
                    currentRoute = "dashboard",
                    userRole = UserRole.STUDENT,
                    onNavigate = { route ->
                        when (route) {
                            "dashboard" -> {
                                // Already on dashboard
                            }
                            "attendance_history" -> {
                                onNavigateToAttendanceHistory()
                            }
                            "events" -> {
                                onNavigateToEvents()
                            }
                            "profile" -> {
                                onNavigateToProfile()
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Clean Welcome Header
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
                        text = if (userRole == UserRole.ADMIN) "Admin Dashboard" else "Welcome Back!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (userRole == UserRole.ADMIN) 
                            "Manage events and monitor attendance" 
                        else 
                            "Select an event to mark your attendance",
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
                // Today's Summary for Students
                if (userRole == UserRole.STUDENT) {
                    val currentTime = System.currentTimeMillis()
                    val todayEvents = state.currentEvents.filter { event ->
                        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.startTime))
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTime))
                        eventDate == today
                    }
                    
                    SummaryCard(
                        title = "Today's Events",
                        count = todayEvents.size,
                        subtitle = if (todayEvents.isEmpty()) "No events scheduled" else "events available",
                        icon = Icons.Default.Event
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Available Events Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (userRole == UserRole.STUDENT) "Available Events" else "Current Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (userRole == UserRole.ADMIN) {
                        TextButton(
                            onClick = onNavigateToEventManagement
                        ) {
                            Text("Manage")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Events List
                if (state.currentEvents.isEmpty()) {
                    CleanEmptyEventsCard(userRole = userRole)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.currentEvents) { event ->
                            CleanEventCard(
                                event = event,
                                userRole = userRole,
                                onMarkAttendance = { onNavigateToAttendance(event.id) },
                                isLoading = state.isMarkingAttendance,
                                isAttendanceMarked = state.markedEventIds.contains(event.id)
                            )
                        }
                    }
                }
                
                // Success Message
                state.attendanceMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = message,
                        type = AlertType.Success
                    )
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
fun SummaryCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CleanEmptyEventsCard(userRole: UserRole) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Events Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (userRole == UserRole.ADMIN) 
                    "Create your first event to get started" 
                else 
                    "No events are currently scheduled for attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CleanEventCard(
    event: Event,
    userRole: UserRole,
    onMarkAttendance: () -> Unit,
    isLoading: Boolean = false,
    isAttendanceMarked: Boolean = false
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    
    // Check if event is active and within attendance window
    val isEventActive = event.isActive && currentTime < event.endTime
    
    // Use the event's configured sign-in offsets for more accurate timing
    // For testing purposes, make the window more generous
    val signInStartTime = event.startTime - (event.signInStartOffset * 60 * 1000) // Convert minutes to milliseconds
    val signInEndTime = event.startTime + (event.signInEndOffset * 60 * 1000) // Convert minutes to milliseconds
    
    // For demo/testing: Allow attendance marking for any active event
    // In production, you would use: currentTime >= signInStartTime && currentTime <= signInEndTime
    val canMarkAttendance = userRole == UserRole.STUDENT && isEventActive
    
    // Show timing information based on actual event configuration
    val attendanceWindowStart = signInStartTime
    val attendanceWindowEnd = signInEndTime
    
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Event Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = dateFormat.format(Date(event.startTime)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Event Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEventActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isEventActive) "Active" else "Ended",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEventActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Event Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EventDetailChip(
                    icon = Icons.Default.AccessTime,
                    text = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
                )
                
                EventDetailChip(
                    icon = Icons.Default.LocationOn,
                    text = "${event.geofenceRadius.toInt()}m radius"
                )
            }
            
            // Attendance Window Info for Students
            if (userRole == UserRole.STUDENT) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Attendance: ${timeFormat.format(Date(attendanceWindowStart))} - ${timeFormat.format(Date(attendanceWindowEnd))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Action Button for Students
            if (userRole == UserRole.STUDENT) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onMarkAttendance,
                    enabled = canMarkAttendance && !isLoading && !isAttendanceMarked,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAttendanceMarked) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(
                            imageVector = if (isAttendanceMarked) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isAttendanceMarked -> "Already Marked"
                                !isEventActive -> "Event Ended"
                                canMarkAttendance -> "Mark Attendance"
                                currentTime < attendanceWindowStart -> {
                                    val minutesUntil = (attendanceWindowStart - currentTime) / (60 * 1000)
                                    "Available in ${minutesUntil}min"
                                }
                                currentTime > attendanceWindowEnd -> "Sign-in Window Closed"
                                else -> "Mark Attendance" // Fallback to allow marking
                            },
                            color = if (isAttendanceMarked) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(
    totalEvents: Int,
    todayEvents: Int,
    markedEvents: Int,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
        Column {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem(
                    title = "Total Events",
                    value = totalEvents.toString(),
                    icon = Icons.Default.Event,
                    color = MaterialTheme.colorScheme.primary
                )
                
                QuickStatItem(
                    title = "Today's Events",
                    value = todayEvents.toString(),
                    icon = Icons.Default.Today,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                QuickStatItem(
                    title = "Marked",
                    value = markedEvents.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun QuickStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ModernLoadingCard(
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Loading events...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please wait while we fetch your events",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EventDetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function to count today's events
fun getTodayEventsCount(events: List<Event>): Int {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    val startOfDay = today.timeInMillis
    
    today.add(Calendar.DAY_OF_MONTH, 1)
    val startOfNextDay = today.timeInMillis
    
    return events.count { event ->
        event.startTime >= startOfDay && event.startTime < startOfNextDay
    }
}
