package dev.ml.smartattendance.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.navigation.Screen
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: UserRole,
    onNavigateToStudentManagement: () -> Unit = {},
    onNavigateToEventManagement: () -> Unit = {},
    onNavigateToAttendance: (String) -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // If user is admin, redirect to admin dashboard
    // This is critical to ensure admins see the bottom navigation
    LaunchedEffect(Unit) {
        if (userRole == UserRole.ADMIN) {
            onNavigateToAdminDashboard()
        }
        viewModel.loadCurrentEvents()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("SmartAttendance ${if (userRole == UserRole.ADMIN) "- Admin" else ""}") 
                },
                actions = {
                    if (userRole == UserRole.ADMIN) {
                        IconButton(onClick = onNavigateToAdminDashboard) {
                            Icon(Icons.Default.Dashboard, contentDescription = "Admin Dashboard")
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
                .padding(16.dp)
        ) {
            // Welcome Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (userRole == UserRole.ADMIN) "Admin Dashboard" else "Student Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (userRole == UserRole.ADMIN) 
                            "Manage events and students" 
                        else 
                            "Mark your attendance for available events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Events Section
            Text(
                text = "Current Events",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.currentEvents.isEmpty()) {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No current events",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.currentEvents) { event ->
                        EventCard(
                            event = event,
                            userRole = userRole,
                            onMarkAttendance = { onNavigateToAttendance(event.id) }
                        )
                    }
                }
            }
            
            // Error handling
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    userRole: UserRole,
    onMarkAttendance: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start: ${dateFormat.format(Date(event.startTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "End: ${dateFormat.format(Date(event.endTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (userRole == UserRole.STUDENT) {
                    Button(
                        onClick = onMarkAttendance,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Mark Attendance")
                    }
                }
            }
        }
    }
}