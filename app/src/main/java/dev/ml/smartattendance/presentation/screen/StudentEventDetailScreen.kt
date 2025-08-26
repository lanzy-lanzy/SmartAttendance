package dev.ml.smartattendance.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import dev.ml.smartattendance.presentation.viewmodel.EventDetailViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAttendanceMarking: (String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Event Details",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading event details...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, 
                       modifier = Modifier.padding(horizontal = 24.dp)) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Event Not Found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "The requested event could not be found.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            android.util.Log.d("StudentEventDetailScreen", "Retry button clicked for event ID: $eventId")
                            viewModel.loadEventDetails(eventId) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Events")
                    }
                }
            }
        } else if (state.event == null) {
            // Handle the case where there's no error but event is still null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, 
                       modifier = Modifier.padding(horizontal = 24.dp)) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Event Found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to retrieve the event details. The event may have been deleted or you may not have permission to view it.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Return to Events")
                    }
                }
            }
        } else {
            state.event?.let { event ->
                StudentEventDetailContent(
                    event = event,
                    onNavigateToAttendanceMarking = onNavigateToAttendanceMarking,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun StudentEventDetailContent(
    event: Event,
    onNavigateToAttendanceMarking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    
    // Calculate attendance window
    val signInStartTime = event.startTime - (event.signInStartOffset * 60 * 1000)
    val signInEndTime = event.startTime + (event.signInEndOffset * 60 * 1000)
    val signOutStartTime = event.endTime - (event.signOutStartOffset * 60 * 1000)
    val signOutEndTime = event.endTime + (event.signOutEndOffset * 60 * 1000)
    
    val isEventActive = event.isActive && currentTime < event.endTime
    val canMarkAttendance = isEventActive && currentTime >= signInStartTime && currentTime <= signInEndTime
    
    val eventStatus = when {
        currentTime < event.startTime -> "Upcoming"
        currentTime >= event.startTime && currentTime <= event.endTime -> "Ongoing"
        else -> "Ended"
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Event Header Card
        ModernCard {
            Column {
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
                        style = MaterialTheme.typography.labelMedium,
                        color = when (eventStatus) {
                            "Ongoing" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "Upcoming" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Event Name
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Event Date
                Text(
                    text = dateFormat.format(Date(event.startTime)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Event Schedule Card
        ModernCard {
            Column {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EventDetailItem(
                    icon = Icons.Default.Schedule,
                    label = "Event Time",
                    value = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                EventDetailItem(
                    icon = Icons.Default.Login,
                    label = "Sign-in Window",
                    value = "${timeFormat.format(Date(signInStartTime))} - ${timeFormat.format(Date(signInEndTime))}"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                EventDetailItem(
                    icon = Icons.Default.Logout,
                    label = "Sign-out Window",
                    value = "${timeFormat.format(Date(signOutStartTime))} - ${timeFormat.format(Date(signOutEndTime))}"
                )
            }
        }
        
        // Location Card
        ModernCard {
            Column {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EventDetailItem(
                    icon = Icons.Default.LocationOn,
                    label = "Coordinates",
                    value = "${String.format("%.6f", event.latitude)}, ${String.format("%.6f", event.longitude)}"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                EventDetailItem(
                    icon = Icons.Default.MyLocation,
                    label = "Geofence Radius",
                    value = "${event.geofenceRadius.toInt()} meters"
                )
            }
        }
        
        // Attendance Action Card
        ModernCard {
            Column {
                Text(
                    text = "Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val attendanceStatusText = when {
                    !event.isActive -> "Event is inactive"
                    currentTime < signInStartTime -> {
                        val minutesUntil = (signInStartTime - currentTime) / (60 * 1000)
                        "Sign-in opens in ${minutesUntil} minutes"
                    }
                    canMarkAttendance -> "You can mark attendance now"
                    currentTime > signInEndTime && currentTime < signOutStartTime -> "Sign-in window closed, sign-out opens later"
                    currentTime >= signOutStartTime && currentTime <= signOutEndTime -> "Sign-out window is open"
                    currentTime > signOutEndTime -> "Attendance window closed"
                    else -> "Attendance not available"
                }
                
                Text(
                    text = attendanceStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canMarkAttendance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { onNavigateToAttendanceMarking(event.id) },
                    enabled = canMarkAttendance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Attendance")
                }
            }
        }
    }
}
