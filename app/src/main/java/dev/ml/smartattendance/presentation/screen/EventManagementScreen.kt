package dev.ml.smartattendance.presentation.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.presentation.viewmodel.EventManagementViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EventManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEventDetail: (String) -> Unit = {},
    viewModel: EventManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var filterActiveOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }
    
    // Clear messages after showing them
    LaunchedEffect(state.creationMessage, state.error) {
        if (state.creationMessage != null || state.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Events",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Filter toggle
                    IconButton(onClick = { filterActiveOnly = !filterActiveOnly }) {
                        Icon(
                            imageVector = if (filterActiveOnly) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = if (filterActiveOnly) "Show All" else "Filter Active",
                            tint = if (filterActiveOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Add button
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add Event",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            // Clean Header with Search and Stats
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Search Bar
                CleanSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search events...",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Compact Statistics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactStatItem(
                        title = "Total",
                        value = state.events.size.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    CompactStatItem(
                        title = "Active",
                        value = state.events.count { it.isActive }.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    CompactStatItem(
                        title = "Inactive",
                        value = state.events.count { !it.isActive }.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Success/Error Messages
            state.creationMessage?.let { message ->
                CleanAlert(
                    message = message,
                    isError = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            state.error?.let { error ->
                CleanAlert(
                    message = error,
                    isError = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Events List Header
            if (filterActiveOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { filterActiveOnly = false },
                        label = { Text("Active Only", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            
            // Events List
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                val filteredEvents = state.events
                    .filter { event ->
                        val matchesSearch = if (searchQuery.isEmpty()) {
                            true
                        } else {
                            event.name.contains(searchQuery, ignoreCase = true) ||
                            event.id.contains(searchQuery, ignoreCase = true)
                        }
                        val matchesFilter = if (filterActiveOnly) {
                            event.isActive
                        } else {
                            true
                        }
                        matchesSearch && matchesFilter
                    }
                
                if (filteredEvents.isEmpty()) {
                    CleanEmptyState(
                        isFiltered = searchQuery.isNotEmpty() || filterActiveOnly,
                        onCreateEvent = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEvents) { event ->
                            CleanEventCard(
                                event = event,
                                onEventClick = { onNavigateToEventDetail(event.id) },
                                onEditClick = {
                                    selectedEvent = event
                                    showEditDialog = true
                                },
                                onDeleteClick = {
                                    selectedEvent = event
                                    showDeleteDialog = true
                                },
                                onToggleActive = {
                                    viewModel.toggleEventStatus(event.id, !event.isActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog Handlers
    if (showAddDialog) {
        CreateEventDialog(
            isLoading = state.isCreating,
            onDismiss = { showAddDialog = false },
            onCreateEvent = { event ->
                viewModel.createEvent(
                    name = event.name,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    radius = event.geofenceRadius,
                    signInStartOffset = event.signInStartOffset,
                    signInEndOffset = event.signInEndOffset,
                    signOutStartOffset = event.signOutStartOffset,
                    signOutEndOffset = event.signOutEndOffset
                )
                showAddDialog = false
            }
        )
    }
    
    if (showEditDialog && selectedEvent != null) {
        EditEventDialog(
            event = selectedEvent!!,
            isLoading = state.isCreating,
            onDismiss = {
                showEditDialog = false
                selectedEvent = null
            },
            onUpdateEvent = { updatedEvent ->
                viewModel.updateEvent(updatedEvent)
                showEditDialog = false
                selectedEvent = null
            }
        )
    }
    
    if (showDeleteDialog && selectedEvent != null) {
        CleanDeleteDialog(
            eventName = selectedEvent!!.name,
            onConfirm = {
                viewModel.deleteEvent(selectedEvent!!.id)
                showDeleteDialog = false
                selectedEvent = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedEvent = null
            }
        )
    }
}

@Composable
fun ModernEventCard(
    event: Event,
    onEventClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick() }
    ) {
        Column {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Event status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (event.isActive) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (event.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (event.isActive) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Event ID: ${event.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action menu
                Row {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (event.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (event.isActive) "Deactivate" else "Activate",
                            tint = if (event.isActive) ErrorRed else SuccessGreen
                        )
                    }
                    
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Event details grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EventDetailItem(
                        icon = Icons.Default.Schedule,
                        label = "Start",
                        value = "${dateFormat.format(Date(event.startTime))}\\n${timeFormat.format(Date(event.startTime))}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    EventDetailItem(
                        icon = Icons.Default.ScheduleSend,
                        label = "End",
                        value = "${dateFormat.format(Date(event.endTime))}\\n${timeFormat.format(Date(event.endTime))}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EventDetailItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = "${String.format("%.4f", event.latitude)}, ${String.format("%.4f", event.longitude)}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    EventDetailItem(
                        icon = Icons.Default.MyLocation,
                        label = "Radius",
                        value = "${event.geofenceRadius.toInt()}m",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EventDetailItem(
                        icon = Icons.Default.Login,
                        label = "Sign-in Window",
                        value = "${event.signInStartOffset} to ${event.signInEndOffset} min",
                        modifier = Modifier.weight(1f)
                    )
                    
                    EventDetailItem(
                        icon = Icons.Default.Logout,
                        label = "Sign-out Window",
                        value = "${event.signOutStartOffset} to ${event.signOutEndOffset} min",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action button
            GradientButton(
                onClick = onEventClick,
                text = "View Details",
                icon = Icons.Default.Visibility,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EventDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EmptyEventsCard(
    isFiltered: Boolean,
    onCreateEvent: () -> Unit
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFiltered) Icons.Default.SearchOff else Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isFiltered) "No events found" else "No events created yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isFiltered) {
                    "Try adjusting your search or filter criteria"
                } else {
                    "Create your first event to get started with attendance tracking"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isFiltered) {
                Spacer(modifier = Modifier.height(20.dp))
                
                GradientButton(
                    onClick = onCreateEvent,
                    text = "Create Event",
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class EventFormData(
    val name: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours later
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geofenceRadius: Float = 50f,
    val signInStartOffset: Long = -15, // 15 minutes before
    val signInEndOffset: Long = 30,    // 30 minutes after start
    val signOutStartOffset: Long = -30, // 30 minutes before end
    val signOutEndOffset: Long = 15     // 15 minutes after end
)

@Composable
fun CreateEventDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreateEvent: (EventFormData) -> Unit
) {
    var formData by remember { mutableStateOf(EventFormData()) }
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Basic Info", "Schedule", "Location", "Settings")
    
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        ModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Event",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { if (!isLoading) onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Progress indicator
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = (currentStep + 1) / steps.size.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Step ${currentStep + 1} of ${steps.size}: ${steps[currentStep]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Step content
                when (currentStep) {
                    0 -> BasicInfoStep(
                        formData = formData,
                        onUpdate = { formData = it }
                    )
                    1 -> ScheduleStep(
                        formData = formData,
                        onUpdate = { formData = it }
                    )
                    2 -> LocationStep(
                        formData = formData,
                        onUpdate = { formData = it }
                    )
                    3 -> SettingsStep(
                        formData = formData,
                        onUpdate = { formData = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            enabled = !isLoading
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = { currentStep++ },
                            enabled = !isLoading && isStepValid(currentStep, formData)
                        ) {
                            Text("Next")
                        }
                    } else {
                        GradientButton(
                            onClick = { onCreateEvent(formData) },
                            enabled = !isLoading && isFormValid(formData),
                            isLoading = isLoading,
                            text = "Create Event",
                            icon = Icons.Default.Check
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicInfoStep(
    formData: EventFormData,
    onUpdate: (EventFormData) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(
            value = formData.name,
            onValueChange = { onUpdate(formData.copy(name = it)) },
            label = "Event Name",
            leadingIcon = Icons.Default.Event,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Give your event a descriptive name that attendees will easily recognize.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScheduleStep(
    formData: EventFormData,
    onUpdate: (EventFormData) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Event Schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Start Date and Time
        Text(
            text = "Start Date & Time",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartDatePicker = true }
            ) {
                OutlinedTextField(
                    value = dateFormat.format(Date(formData.startTime)),
                    onValueChange = { },
                    label = { Text("Start Date") },
                    readOnly = true,
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartTimePicker = true }
            ) {
                OutlinedTextField(
                    value = timeFormat.format(Date(formData.startTime)),
                    onValueChange = { },
                    label = { Text("Start Time") },
                    readOnly = true,
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        
        // End Date and Time
        Text(
            text = "End Date & Time",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndDatePicker = true }
            ) {
                OutlinedTextField(
                    value = dateFormat.format(Date(formData.endTime)),
                    onValueChange = { },
                    label = { Text("End Date") },
                    readOnly = true,
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndTimePicker = true }
            ) {
                OutlinedTextField(
                    value = timeFormat.format(Date(formData.endTime)),
                    onValueChange = { },
                    label = { Text("End Time") },
                    readOnly = true,
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        
        // Duration display
        val duration = formData.endTime - formData.startTime
        if (duration > 0) {
            Text(
                text = "Duration: ${formatDuration(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "⚠️ End time must be after start time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    
    // Date and Time Pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.startTime
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.timeInMillis = selectedDate
                
                calendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                
                onUpdate(formData.copy(startTime = calendar.timeInMillis))
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showStartTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.startTime
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                
                onUpdate(formData.copy(startTime = calendar.timeInMillis))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.endTime
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.timeInMillis = selectedDate
                
                calendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                
                onUpdate(formData.copy(endTime = calendar.timeInMillis))
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.endTime
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                
                onUpdate(formData.copy(endTime = calendar.timeInMillis))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
fun LocationStep(
    formData: EventFormData,
    onUpdate: (EventFormData) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
    // Create location provider instance
    val locationProvider = remember {
        dev.ml.smartattendance.data.service.LocationProviderImpl(context)
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Event Location",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Current Location Button
        OutlinedButton(
            onClick = {
                isLoadingLocation = true
                locationError = null
                
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val location = locationProvider.getCurrentLocation()
                        if (location != null) {
                            onUpdate(formData.copy(
                                latitude = location.latitude,
                                longitude = location.longitude
                            ))
                        } else {
                            locationError = "Unable to get current location. Please check location permissions and GPS settings."
                        }
                    } catch (e: Exception) {
                        locationError = "Error getting location: ${e.message}"
                    } finally {
                        isLoadingLocation = false
                    }
                }
            },
            enabled = !isLoadingLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Getting Location...")
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Current Location")
            }
        }
        
        // Location error display
        locationError?.let { error ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        Text(
            text = "or enter coordinates manually:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernTextField(
                value = if (formData.latitude == 0.0) "" else formData.latitude.toString(),
                onValueChange = { input ->
                    val value = input.toDoubleOrNull()
                    if (value != null) {
                        onUpdate(formData.copy(latitude = value))
                    } else if (input.isEmpty()) {
                        onUpdate(formData.copy(latitude = 0.0))
                    }
                },
                label = "Latitude",
                leadingIcon = Icons.Default.LocationOn,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            
            ModernTextField(
                value = if (formData.longitude == 0.0) "" else formData.longitude.toString(),
                onValueChange = { input ->
                    val value = input.toDoubleOrNull()
                    if (value != null) {
                        onUpdate(formData.copy(longitude = value))
                    } else if (input.isEmpty()) {
                        onUpdate(formData.copy(longitude = 0.0))
                    }
                },
                label = "Longitude",
                leadingIcon = Icons.Default.LocationOn,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }
        
        ModernTextField(
            value = formData.geofenceRadius.toString(),
            onValueChange = { 
                it.toFloatOrNull()?.let { radius ->
                    onUpdate(formData.copy(geofenceRadius = radius))
                }
            },
            label = "Geofence Radius (meters)",
            leadingIcon = Icons.Default.MyLocation,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (formData.latitude != 0.0 && formData.longitude != 0.0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "📍 Location Set",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lat: ${String.format("%.6f", formData.latitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lng: ${String.format("%.6f", formData.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Radius: ${formData.geofenceRadius.toInt()}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Text(
            text = "Students must be within ${formData.geofenceRadius.toInt()}m of this location to mark attendance. Recommended: 50-100m for classroom environments.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsStep(
    formData: EventFormData,
    onUpdate: (EventFormData) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Attendance Windows",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Configure when students can sign in and sign out relative to event times.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Sign-in window
        Text(
            text = "Sign-in Window",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernTextField(
                value = formData.signInStartOffset.toString(),
                onValueChange = { 
                    it.toLongOrNull()?.let { offset ->
                        onUpdate(formData.copy(signInStartOffset = offset))
                    }
                },
                label = "Start (min before)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            
            ModernTextField(
                value = formData.signInEndOffset.toString(),
                onValueChange = { 
                    it.toLongOrNull()?.let { offset ->
                        onUpdate(formData.copy(signInEndOffset = offset))
                    }
                },
                label = "End (min after start)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Sign-out window
        Text(
            text = "Sign-out Window",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernTextField(
                value = formData.signOutStartOffset.toString(),
                onValueChange = { 
                    it.toLongOrNull()?.let { offset ->
                        onUpdate(formData.copy(signOutStartOffset = offset))
                    }
                },
                label = "Start (min before end)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            
            ModernTextField(
                value = formData.signOutEndOffset.toString(),
                onValueChange = { 
                    it.toLongOrNull()?.let { offset ->
                        onUpdate(formData.copy(signOutEndOffset = offset))
                    }
                },
                label = "End (min after end)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EditEventDialog(
    event: Event,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUpdateEvent: (Event) -> Unit
) {
    var formData by remember { 
        mutableStateOf(
            EventFormData(
                name = event.name,
                startTime = event.startTime,
                endTime = event.endTime,
                latitude = event.latitude,
                longitude = event.longitude,
                geofenceRadius = event.geofenceRadius,
                signInStartOffset = event.signInStartOffset,
                signInEndOffset = event.signInEndOffset,
                signOutStartOffset = event.signOutStartOffset,
                signOutEndOffset = event.signOutEndOffset
            )
        )
    }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        ModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Event",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { if (!isLoading) onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Form content in scrollable column
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    item {
                        ModernTextField(
                            value = formData.name,
                            onValueChange = { formData = formData.copy(name = it) },
                            label = "Event Name",
                            leadingIcon = Icons.Default.Event,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Start Date and Time
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = dateFormat.format(Date(formData.startTime)),
                                    onValueChange = { },
                                    label = { Text("Start Date") },
                                    readOnly = true,
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartTimePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = timeFormat.format(Date(formData.startTime)),
                                    onValueChange = { },
                                    label = { Text("Start Time") },
                                    readOnly = true,
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // End Date and Time
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = dateFormat.format(Date(formData.endTime)),
                                    onValueChange = { },
                                    label = { Text("End Date") },
                                    readOnly = true,
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndTimePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = timeFormat.format(Date(formData.endTime)),
                                    onValueChange = { },
                                    label = { Text("End Time") },
                                    readOnly = true,
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                    
                    item {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = formData.latitude.toString(),
                                onValueChange = { 
                                    it.toDoubleOrNull()?.let { lat ->
                                        formData = formData.copy(latitude = lat)
                                    }
                                },
                                label = "Latitude",
                                leadingIcon = Icons.Default.LocationOn,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            
                            ModernTextField(
                                value = formData.longitude.toString(),
                                onValueChange = { 
                                    it.toDoubleOrNull()?.let { lng ->
                                        formData = formData.copy(longitude = lng)
                                    }
                                },
                                label = "Longitude",
                                leadingIcon = Icons.Default.LocationOn,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ModernTextField(
                            value = formData.geofenceRadius.toString(),
                            onValueChange = { 
                                it.toFloatOrNull()?.let { radius ->
                                    formData = formData.copy(geofenceRadius = radius)
                                }
                            },
                            label = "Geofence Radius (meters)",
                            leadingIcon = Icons.Default.MyLocation,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    
                    GradientButton(
                        onClick = {
                            val updatedEvent = event.copy(
                                name = formData.name,
                                startTime = formData.startTime,
                                endTime = formData.endTime,
                                latitude = formData.latitude,
                                longitude = formData.longitude,
                                geofenceRadius = formData.geofenceRadius,
                                signInStartOffset = formData.signInStartOffset,
                                signInEndOffset = formData.signInEndOffset,
                                signOutStartOffset = formData.signOutStartOffset,
                                signOutEndOffset = formData.signOutEndOffset
                            )
                            onUpdateEvent(updatedEvent)
                        },
                        enabled = !isLoading && formData.name.isNotBlank(),
                        isLoading = isLoading,
                        text = "Update",
                        icon = Icons.Default.Save
                    )
                }
            }
        }
    }
    
    // Date and Time Pickers for Edit Dialog
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.startTime
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.timeInMillis = selectedDate
                
                calendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                
                formData = formData.copy(startTime = calendar.timeInMillis)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showStartTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.startTime
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                
                formData = formData.copy(startTime = calendar.timeInMillis)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.endTime
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.timeInMillis = selectedDate
                
                calendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                
                formData = formData.copy(endTime = calendar.timeInMillis)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = formData.endTime
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                
                formData = formData.copy(endTime = calendar.timeInMillis)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    eventName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Event",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete the event:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "\"$eventName\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This action cannot be undone and all related attendance records will be affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun updateDateTime(
    date: String, 
    time: String, 
    isStart: Boolean, 
    formData: EventFormData, 
    onUpdate: (EventFormData) -> Unit
) {
    try {
        val dateTimeString = "$date $time"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timestamp = format.parse(dateTimeString)?.time ?: return
        
        if (isStart) {
            onUpdate(formData.copy(startTime = timestamp))
        } else {
            onUpdate(formData.copy(endTime = timestamp))
        }
    } catch (e: Exception) {
        // Handle parsing error
    }
}

fun formatDuration(milliseconds: Long): String {
    val hours = milliseconds / (1000 * 60 * 60)
    val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
    return "${hours}h ${minutes}m"
}

fun isStepValid(step: Int, formData: EventFormData): Boolean {
    return when (step) {
        0 -> formData.name.isNotBlank()
        1 -> formData.startTime < formData.endTime
        2 -> formData.latitude != 0.0 && formData.longitude != 0.0 && formData.geofenceRadius > 0
        3 -> true
        else -> false
    }
}

fun isFormValid(formData: EventFormData): Boolean {
    return formData.name.isNotBlank() &&
           formData.startTime < formData.endTime &&
           formData.latitude != 0.0 &&
           formData.longitude != 0.0 &&
           formData.geofenceRadius > 0
}

// Clean UI Components for Professional Design

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun CleanSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else null,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun CompactStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CleanAlert(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    
    val textColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun CleanEventCard(
    event: Event,
    onEventClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (event.isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${dateFormat.format(Date(event.startTime))} - ${dateFormat.format(Date(event.endTime))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "${event.geofenceRadius.toInt()}m radius",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action buttons
                Row {
                    IconButton(
                        onClick = onToggleActive,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (event.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (event.isActive) "Pause" else "Activate",
                            tint = if (event.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanEmptyState(
    isFiltered: Boolean,
    onCreateEvent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isFiltered) Icons.Default.SearchOff else Icons.Default.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isFiltered) "No events found" else "No events yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isFiltered) {
                "Try adjusting your search or filters"
            } else {
                "Create your first event to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        if (!isFiltered) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onCreateEvent,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Event")
            }
        }
    }
}

@Composable
fun CleanDeleteDialog(
    eventName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = "Delete Event",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$eventName\"? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Date and Time Picker Components

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}