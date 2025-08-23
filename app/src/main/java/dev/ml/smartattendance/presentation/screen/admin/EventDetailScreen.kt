package dev.ml.smartattendance.presentation.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.presentation.viewmodel.EventDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
        viewModel.loadAttendanceRecords(eventId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                    }
                    
                    state.event?.let { event ->
                        IconButton(
                            onClick = { viewModel.toggleEventStatus(eventId, !event.isActive) }
                        ) {
                            if (event.isActive) {
                                Icon(
                                    Icons.Default.PauseCircle,
                                    contentDescription = "Deactivate Event",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Activate Event",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
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
                CircularProgressIndicator()
            }
        } else if (state.event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Event not found")
            }
        } else {
            val event = state.event!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Event Info Card
                item {
                    EventInfoCard(event)
                }
                
                // Status Banner
                item {
                    StatusBanner(event.isActive)
                }
                
                // Attendance Statistics
                item {
                    AttendanceStatisticsCard(state.attendanceRecords)
                }
                
                // Attendance Records Title
                item {
                    Text(
                        text = "Attendance Records",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Attendance Records
                if (state.attendanceRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No attendance records yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.attendanceRecords) { record ->
                        AttendanceRecordCard(record)
                    }
                }
            }
        }
    }
    
    // Edit Event Dialog
    if (showEditDialog && state.event != null) {
        EditEventDialog(
            event = state.event!!,
            isLoading = state.isUpdating,
            onDismiss = { showEditDialog = false },
            onUpdateEvent = { name, startTime, endTime, latitude, longitude, radius, isActive ->
                viewModel.updateEvent(
                    eventId = eventId,
                    name = name,
                    startTime = startTime,
                    endTime = endTime,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    isActive = isActive
                )
                showEditDialog = false
            }
        )
    }
    
    // Show messages
    state.message?.let { message ->
        LaunchedEffect(message) {
            // Auto-dismiss message after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(message)
        }
    }
}

@Composable
fun EventInfoCard(event: Event) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Starts: ${dateFormat.format(Date(event.startTime))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ends: ${dateFormat.format(Date(event.endTime))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Location: ${event.latitude}, ${event.longitude}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Geofence Radius: ${event.geofenceRadius} meters",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sign-in Window: ${event.signInStartOffset} mins before to ${event.signInEndOffset} mins after start",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Sign-out Window: ${event.signOutStartOffset} mins before to ${event.signOutEndOffset} mins after end",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBanner(isActive: Boolean) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "Event is Active" else "Event is Inactive",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AttendanceStatisticsCard(records: List<AttendanceRecord>) {
    val total = records.size
    val present = records.count { it.status == AttendanceStatus.PRESENT }
    val late = records.count { it.status == AttendanceStatus.LATE }
    val absent = records.count { it.status == AttendanceStatus.ABSENT }
    val excused = records.count { it.status == AttendanceStatus.EXCUSED }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Attendance Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttendanceStatBox(
                    label = "Total",
                    count = total,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                AttendanceStatBox(
                    label = "Present",
                    count = present,
                    color = Color(0xFF4CAF50)  // Green
                )
                AttendanceStatBox(
                    label = "Late",
                    count = late,
                    color = Color(0xFFFFC107)  // Amber
                )
                AttendanceStatBox(
                    label = "Absent",
                    count = absent,
                    color = Color(0xFFF44336)  // Red
                )
                AttendanceStatBox(
                    label = "Excused",
                    count = excused,
                    color = Color(0xFF2196F3)  // Blue
                )
            }
            
            if (total > 0) {
                LinearProgressIndicator(
                    progress = present.toFloat() / total,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF4CAF50),  // Green
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Text(
                    text = "Attendance Rate: ${(present.toFloat() / total * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AttendanceStatBox(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AttendanceRecordCard(record: AttendanceRecord) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        when (record.status) {
                            AttendanceStatus.PRESENT -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            AttendanceStatus.LATE -> Color(0xFFFFC107).copy(alpha = 0.2f)
                            AttendanceStatus.ABSENT -> Color(0xFFF44336).copy(alpha = 0.2f)
                            AttendanceStatus.EXCUSED -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        },
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.status) {
                        AttendanceStatus.PRESENT -> Icons.Default.CheckCircle
                        AttendanceStatus.LATE -> Icons.Default.Timer
                        AttendanceStatus.ABSENT -> Icons.Default.Cancel
                        AttendanceStatus.EXCUSED -> Icons.Default.Assignment
                    },
                    contentDescription = null,
                    tint = when (record.status) {
                        AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
                        AttendanceStatus.LATE -> Color(0xFFFFC107)
                        AttendanceStatus.ABSENT -> Color(0xFFF44336)
                        AttendanceStatus.EXCUSED -> Color(0xFF2196F3)
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Student and timestamp info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Student ID: ${record.studentId}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Time: ${dateFormat.format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                record.latitude?.let { latitude ->
                    record.longitude?.let { longitude ->
                        Text(
                            text = "Location: $latitude, $longitude",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                record.notes?.let { notes ->
                    if (notes.isNotEmpty()) {
                        Text(
                            text = "Notes: $notes",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Status Badge
            Box(
                modifier = Modifier
                    .background(
                        when (record.status) {
                            AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
                            AttendanceStatus.LATE -> Color(0xFFFFC107)
                            AttendanceStatus.ABSENT -> Color(0xFFF44336)
                            AttendanceStatus.EXCUSED -> Color(0xFF2196F3)
                        },
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = record.status.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: Event,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUpdateEvent: (String, Long, Long, Double, Double, Float, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(event.name) }
    var startDate by remember { mutableStateOf(Date(event.startTime)) }
    var endDate by remember { mutableStateOf(Date(event.endTime)) }
    var latitude by remember { mutableStateOf(event.latitude.toString()) }
    var longitude by remember { mutableStateOf(event.longitude.toString()) }
    var radius by remember { mutableStateOf(event.geofenceRadius.toString()) }
    var isActive by remember { mutableStateOf(event.isActive) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Time: ${SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(startDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "End Time: ${SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(endDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
                
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Geofence Radius (meters)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    
                    Text("Event is Active")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateEvent(
                        name,
                        startDate.time,
                        endDate.time,
                        latitude.toDoubleOrNull() ?: event.latitude,
                        longitude.toDoubleOrNull() ?: event.longitude,
                        radius.toFloatOrNull() ?: event.geofenceRadius,
                        isActive
                    )
                },
                enabled = !isLoading && name.isNotBlank() && 
                          latitude.toDoubleOrNull() != null && 
                          longitude.toDoubleOrNull() != null &&
                          radius.toFloatOrNull() != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Date pickers would go here (simplified for this implementation)
    // In a full implementation, you would use DatePickerDialog from Material3
}