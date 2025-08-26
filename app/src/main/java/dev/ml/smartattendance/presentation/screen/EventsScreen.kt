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
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAttendanceHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAttendanceMarking: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToEventDetail: (String) -> Unit = {},
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Load events on composition
    LaunchedEffect(Unit) {
        viewModel.loadCurrentEvents()
    }
    
    // Refresh events when screen recomposes (e.g., when user returns after marking attendance)
    LaunchedEffect(Unit) {
        // Add a small delay to ensure we don't overwhelm the system
        kotlinx.coroutines.delay(300)
        viewModel.loadCurrentEvents()
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Events",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadCurrentEvents() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = "events",
                userRole = UserRole.STUDENT,
                onNavigate = { route -> 
                    when (route) {
                        "dashboard" -> onNavigateToDashboard()
                        "attendance_history" -> onNavigateToAttendanceHistory()
                        "events" -> { /* Already here */ }
                        "profile" -> onNavigateToProfile()
                        else -> {
                            // Handle any other routes if needed
                        }
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
                        text = "All Events",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "View and mark attendance for available events",
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
                // Summary Card
                if (state.currentEvents.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    val upcomingEvents = state.currentEvents.filter { it.startTime > currentTime }
                    val ongoingEvents = state.currentEvents.filter { 
                        it.startTime <= currentTime && it.endTime >= currentTime 
                    }
                    
                    ModernCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                title = "Total Events",
                                value = state.currentEvents.size.toString(),
                                icon = Icons.Default.Event
                            )
                            
                            StatItem(
                                title = "Ongoing",
                                value = ongoingEvents.size.toString(),
                                icon = Icons.Default.PlayArrow
                            )
                            
                            StatItem(
                                title = "Upcoming",
                                value = upcomingEvents.size.toString(),
                                icon = Icons.Default.Schedule
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Events List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (state.currentEvents.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.loadCurrentEvents() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Events List
                if (state.currentEvents.isEmpty()) {
                    EmptyEventsCard()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.currentEvents) { event ->
                            EventCard(
                                event = event,
                                onMarkAttendance = { onNavigateToAttendanceMarking(event.id) },
                                onViewDetails = { 
                                    android.util.Log.d("EventsScreen", "Navigating to event details for event ID: ${event.id}")
                                    onNavigateToEventDetail(event.id) 
                                },
                                isLoading = state.isMarkingAttendance,
                                isAttendanceMarked = state.markedEventIds.contains(event.id)
                            )
                        }
                    }
                }
                
                // Error handling
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = error,
                        type = AlertType.Error
                    ) {
                        viewModel.clearError()
                    }
                }
                
                // Success message
                state.attendanceMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = message,
                        type = AlertType.Success
                    ) {
                        viewModel.clearAttendanceMessage()
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEventsCard() {
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
                text = "No events are currently scheduled for attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onMarkAttendance: () -> Unit,
    onViewDetails: () -> Unit,
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
    val canMarkAttendance = isEventActive
    
    // Show timing information based on actual event configuration
    val attendanceWindowStart = signInStartTime
    val attendanceWindowEnd = signInEndTime
    
    val eventStatus = when {
        currentTime < event.startTime -> "Upcoming"
        currentTime >= event.startTime && currentTime <= event.endTime -> "Ongoing"
        else -> "Ended"
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
                    color = when (eventStatus) {
                        "Ongoing" -> MaterialTheme.colorScheme.primaryContainer
                        "Upcoming" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = eventStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (eventStatus) {
                            "Ongoing" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "Upcoming" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
            
            // Attendance Window Info
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Attendance: ${timeFormat.format(Date(attendanceWindowStart))} - ${timeFormat.format(Date(attendanceWindowEnd))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            // Action Buttons
            Spacer(modifier = Modifier.height(16.dp))

            // Row with two buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // View Details Button
                OutlinedButton(
                    onClick = { 
                        android.util.Log.d("EventsScreen", "View Details clicked for event: ${event.id}")
                        onViewDetails() 
                    },
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
                    enabled = canMarkAttendance && !isLoading && !isAttendanceMarked,
                    modifier = Modifier.weight(1f),
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
                    } else {
                        Icon(
                            imageVector = if (isAttendanceMarked)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isAttendanceMarked -> "Marked"
                                !isEventActive -> "Ended"
                                canMarkAttendance -> "Mark"
                                else -> "Mark"
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