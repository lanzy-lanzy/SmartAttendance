package dev.ml.smartattendance.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import dev.ml.smartattendance.presentation.viewmodel.DashboardViewModel
import dev.ml.smartattendance.presentation.state.DashboardState
import dev.ml.smartattendance.ui.components.AlertCard
import dev.ml.smartattendance.ui.components.AlertType
import dev.ml.smartattendance.ui.components.EventDetailChip
import dev.ml.smartattendance.ui.components.ModernBottomNavigation
import dev.ml.smartattendance.ui.components.ModernCard
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

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
    onNavigateToEventDetail: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val attendanceViewModel: AttendanceViewModel = hiltViewModel()
    val attendanceState by attendanceViewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Initial launch effect to load data
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        attendanceViewModel.loadCurrentEvents()
    }
    
    // Refresh data when screen recomposes (e.g., when user returns after marking attendance)
    LaunchedEffect(Unit) {
        // Add a small delay to ensure we don't overwhelm the system
        kotlinx.coroutines.delay(300)
        viewModel.loadDashboardData()
        attendanceViewModel.loadCurrentEvents()
    }
    
    Scaffold(
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = "dashboard",
                userRole = state.userRole,
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> { /* Already on dashboard */ }
                        "attendance_history" -> onNavigateToAttendanceHistory()
                        "events" -> onNavigateToEvents()
                        "profile" -> onNavigateToProfile()
                        else -> {
                            // Handle other routes as needed
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Header(
                userName = "Student", // This should come from the user data
                isAdmin = state.userRole == UserRole.ADMIN,
                onAdminDashboard = onNavigateToAdminDashboard,
                scrollState = scrollState
            )
            
            // Dashboard Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Quick Stats Card
                QuickStatsCard(
                    totalEvents = state.totalEvents,
                    todayEvents = 0, // This needs to be calculated
                    markedEvents = 0, // This needs to be calculated
                    alpha = if (scrollState.value > 0) 1f else 0f
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                if (attendanceState.currentEvents.isEmpty()) {
                    CleanEmptyEventsCard(userRole = userRole)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(attendanceState.currentEvents) { event ->
                            CleanEventCard(
                                event = event,
                                userRole = userRole,
                                onMarkAttendance = { onNavigateToAttendance(event.id) },
                                onViewDetails = { onNavigateToEventDetail(event.id) },
                                isLoading = attendanceState.isMarkingAttendance,
                                isAttendanceMarked = attendanceState.markedEventIds.contains(event.id)
                            )
                        }
                    }
                }
                
                // Success Message
                attendanceState.attendanceMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = message,
                        type = AlertType.Success
                    )
                }
                
                // Error handling
                attendanceState.error?.let { error ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Header(
    userName: String,
    isAdmin: Boolean,
    onAdminDashboard: () -> Unit = {},
    scrollState: ScrollState
) {
    val scrollOffset = (scrollState.value / 2).coerceIn(0, 70)
    val dynamicPadding = 30.dp - with(LocalDensity.current) { scrollOffset.toDp() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dynamicPadding, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row {
                // Admin Dashboard button for admin users
                if (isAdmin) {
                    IconButton(onClick = onAdminDashboard) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Dashboard"
                        )
                    }
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
    onViewDetails: () -> Unit = {},
    isLoading: Boolean = false,
    isAttendanceMarked: Boolean = false
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    
    // Check if event is active and within attendance window
    val isEventActive = event.isActive && currentTime < event.endTime
    
    // Use the event's configured sign-in offsets for proper timing window
    val signInStartTime = event.startTime - (event.signInStartOffset * 60 * 1000) // Convert minutes to milliseconds
    val signInEndTime = event.startTime + (event.signInEndOffset * 60 * 1000) // Convert minutes to milliseconds
    
    // Check if current time is within the sign-in window
    val isWithinSignInWindow = currentTime >= signInStartTime && currentTime <= signInEndTime
    
    // Determine if attendance can be marked
    val canMarkAttendance = userRole == UserRole.STUDENT && isEventActive && isWithinSignInWindow && !isAttendanceMarked
    
    // Determine the attendance button state
    val attendanceButtonState = when {
        isAttendanceMarked -> AttendanceButtonState.ALREADY_MARKED
        !isEventActive -> AttendanceButtonState.EVENT_ENDED
        !isWithinSignInWindow && currentTime < signInStartTime -> {
            // Calculate minutes until sign-in window opens
            val minutesUntil = (signInStartTime - currentTime) / (60 * 1000)
            AttendanceButtonState.NOT_STARTED(minutesUntil)
        }
        !isWithinSignInWindow && currentTime > signInEndTime -> AttendanceButtonState.WINDOW_CLOSED
        isWithinSignInWindow -> AttendanceButtonState.AVAILABLE
        else -> AttendanceButtonState.WINDOW_CLOSED
    }
    
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (attendanceButtonState) {
                            is AttendanceButtonState.AVAILABLE -> Icons.Default.Timer
                            is AttendanceButtonState.ALREADY_MARKED -> Icons.Default.CheckCircle
                            is AttendanceButtonState.EVENT_ENDED -> Icons.Default.EventBusy
                            is AttendanceButtonState.WINDOW_CLOSED -> Icons.Default.DoNotDisturb
                            is AttendanceButtonState.NOT_STARTED -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (attendanceButtonState) {
                            is AttendanceButtonState.AVAILABLE -> MaterialTheme.colorScheme.primary
                            is AttendanceButtonState.ALREADY_MARKED -> Color.Green
                            is AttendanceButtonState.EVENT_ENDED -> MaterialTheme.colorScheme.error
                            is AttendanceButtonState.WINDOW_CLOSED -> MaterialTheme.colorScheme.error
                            is AttendanceButtonState.NOT_STARTED -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = when (attendanceButtonState) {
                            is AttendanceButtonState.AVAILABLE -> "Sign-in window: ${timeFormat.format(Date(signInStartTime))} - ${timeFormat.format(Date(signInEndTime))}"
                            is AttendanceButtonState.ALREADY_MARKED -> "Attendance already marked"
                            is AttendanceButtonState.EVENT_ENDED -> "Event has ended"
                            is AttendanceButtonState.WINDOW_CLOSED -> "Sign-in window closed"
                            is AttendanceButtonState.NOT_STARTED -> {
                                val minutes = (attendanceButtonState as AttendanceButtonState.NOT_STARTED).minutesUntil
                                "Sign-in available in $minutes minutes"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (attendanceButtonState) {
                            is AttendanceButtonState.AVAILABLE -> MaterialTheme.colorScheme.primary
                            is AttendanceButtonState.ALREADY_MARKED -> Color.Green
                            is AttendanceButtonState.EVENT_ENDED -> MaterialTheme.colorScheme.error
                            is AttendanceButtonState.WINDOW_CLOSED -> MaterialTheme.colorScheme.error
                            is AttendanceButtonState.NOT_STARTED -> MaterialTheme.colorScheme.tertiary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Action Buttons for Students
            if (userRole == UserRole.STUDENT) {
                Spacer(modifier = Modifier.height(16.dp))

                // Row with two buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // View Details Button
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Details")
                    }

                    // Mark Attendance Button
                    Button(
                        onClick = onMarkAttendance,
                        enabled = canMarkAttendance && !isLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (attendanceButtonState) {
                                is AttendanceButtonState.AVAILABLE -> MaterialTheme.colorScheme.primary
                                is AttendanceButtonState.ALREADY_MARKED -> Color.Green.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            disabledContainerColor = when (attendanceButtonState) {
                                is AttendanceButtonState.ALREADY_MARKED -> Color.Green.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marking...")
                        } else {
                            Icon(
                                imageVector = when (attendanceButtonState) {
                                    is AttendanceButtonState.AVAILABLE -> Icons.Default.Fingerprint
                                    is AttendanceButtonState.ALREADY_MARKED -> Icons.Default.CheckCircle
                                    is AttendanceButtonState.EVENT_ENDED -> Icons.Default.EventBusy
                                    is AttendanceButtonState.WINDOW_CLOSED -> Icons.Default.DoNotDisturb
                                    is AttendanceButtonState.NOT_STARTED -> Icons.Default.Schedule
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (attendanceButtonState) {
                                    is AttendanceButtonState.AVAILABLE -> "Mark Attendance"
                                    is AttendanceButtonState.ALREADY_MARKED -> "Already Marked"
                                    is AttendanceButtonState.EVENT_ENDED -> "Event Ended"
                                    is AttendanceButtonState.WINDOW_CLOSED -> "Window Closed"
                                    is AttendanceButtonState.NOT_STARTED -> {
                                        val minutes = (attendanceButtonState as AttendanceButtonState.NOT_STARTED).minutesUntil
                                        "Available in ${minutes}m"
                                    }
                                }
                            )
                        }
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
            .graphicsLayer(alpha = alpha)
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

// Define attendance button states for better UI management
sealed class AttendanceButtonState {
    object AVAILABLE : AttendanceButtonState()
    object ALREADY_MARKED : AttendanceButtonState()
    object EVENT_ENDED : AttendanceButtonState()
    object WINDOW_CLOSED : AttendanceButtonState()
    data class NOT_STARTED(val minutesUntil: Long) : AttendanceButtonState()
}
