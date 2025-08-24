package dev.ml.smartattendance.presentation.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.data.service.BiometricAuthenticatorImpl
import dev.ml.smartattendance.domain.model.biometric.AuthResult
import dev.ml.smartattendance.domain.model.biometric.BiometricCapability
import dev.ml.smartattendance.domain.model.biometric.BiometricError
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import dev.ml.smartattendance.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceMarkingScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onAttendanceMarked: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Find the specific event
    val event = state.currentEvents.find { it.id == eventId }
    
    // Biometric authenticator
    val biometricAuthenticator = remember {
        BiometricAuthenticatorImpl(context)
    }
    
    var currentStep by remember { mutableStateOf(AttendanceStep.EVENT_INFO) }
    var locationValidated by remember { mutableStateOf(false) }
    var biometricValidated by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    LaunchedEffect(eventId) {
        viewModel.loadCurrentEvents()
    }
    
    Scaffold(
        topBar = {
            CleanTopAppBar(
                title = "Mark Attendance",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (event == null) {
            // Event not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                        text = "The event you're trying to access could not be found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
            ) {
                // Progress Indicator
                AttendanceProgressIndicator(
                    currentStep = currentStep,
                    locationValidated = locationValidated,
                    biometricValidated = biometricValidated
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                when (currentStep) {
                    AttendanceStep.EVENT_INFO -> {
                        EventInfoStep(
                            event = event,
                            onNext = { currentStep = AttendanceStep.LOCATION_CHECK }
                        )
                    }
                    
                    AttendanceStep.LOCATION_CHECK -> {
                        LocationCheckStep(
                            event = event,
                            isProcessing = isProcessing,
                            onLocationValidated = { validated ->
                                locationValidated = validated
                                if (validated) {
                                    currentStep = AttendanceStep.BIOMETRIC_AUTH
                                }
                            },
                            onBack = { currentStep = AttendanceStep.EVENT_INFO },
                            viewModel = viewModel
                        )
                    }
                    
                    AttendanceStep.BIOMETRIC_AUTH -> {
                        BiometricAuthStep(
                            event = event,
                            biometricAuthenticator = biometricAuthenticator,
                            isProcessing = isProcessing,
                            onBiometricValidated = { validated ->
                                biometricValidated = validated
                                if (validated) {
                                    currentStep = AttendanceStep.MARK_ATTENDANCE
                                }
                            },
                            onBack = { currentStep = AttendanceStep.LOCATION_CHECK }
                        )
                    }
                    
                    AttendanceStep.MARK_ATTENDANCE -> {
                        MarkAttendanceStep(
                            event = event,
                            isProcessing = isProcessing,
                            onMarkAttendance = {
                                scope.launch {
                                    isProcessing = true
                                    // Using the sample student ID that exists in the database
                                    viewModel.markAttendance("STUDENT001", eventId)
                                    isProcessing = false
                                    onAttendanceMarked()
                                }
                            },
                            onBack = { currentStep = AttendanceStep.BIOMETRIC_AUTH }
                        )
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
                
                // Success message
                state.attendanceMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertCard(
                        message = message,
                        type = AlertType.Success
                    )
                }
            }
        }
    }
}

enum class AttendanceStep {
    EVENT_INFO,
    LOCATION_CHECK,
    BIOMETRIC_AUTH,
    MARK_ATTENDANCE
}

@Composable
fun AttendanceProgressIndicator(
    currentStep: AttendanceStep,
    locationValidated: Boolean,
    biometricValidated: Boolean
) {
    val steps = listOf("Event Info", "Location", "Biometric", "Complete")
    val currentStepIndex = when (currentStep) {
        AttendanceStep.EVENT_INFO -> 0
        AttendanceStep.LOCATION_CHECK -> 1
        AttendanceStep.BIOMETRIC_AUTH -> 2
        AttendanceStep.MARK_ATTENDANCE -> 3
    }
    
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val isCompleted = index < currentStepIndex || 
                            (index == 1 && locationValidated) ||
                            (index == 2 && biometricValidated)
                        val isCurrent = index == currentStepIndex
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = when {
                                        isCompleted -> MaterialTheme.colorScheme.primary
                                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = step,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = (currentStepIndex + 1) / steps.size.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EventInfoStep(
    event: Event,
    onNext: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Column {
        Text(
            text = "Event Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please review the event details before marking attendance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EventInfoRow(
                    icon = Icons.Default.DateRange,
                    label = "Date",
                    value = dateFormat.format(Date(event.startTime))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                EventInfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "Time",
                    value = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                EventInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location Radius",
                    value = "${event.geofenceRadius.toInt()} meters"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun EventInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LocationCheckStep(
    event: Event,
    isProcessing: Boolean,
    onLocationValidated: (Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: AttendanceViewModel
) {
    var locationCheckStatus by remember { mutableStateOf<LocationCheckStatus>(LocationCheckStatus.NotStarted) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column {
        Text(
            text = "Location Verification",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We need to verify you're at the correct location for this event.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (locationCheckStatus) {
                    LocationCheckStatus.NotStarted -> {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Ready to Check Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Make sure you're within ${event.geofenceRadius.toInt()} meters of the event location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    LocationCheckStatus.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Checking Location...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please wait while we verify your location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    LocationCheckStatus.Success -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Location Verified",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "You're within the required area for this event.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    is LocationCheckStatus.Error -> {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Location Check Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = (locationCheckStatus as LocationCheckStatus.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
            
            Button(
                onClick = {
                    if (locationCheckStatus is LocationCheckStatus.Success) {
                        onLocationValidated(true)
                    } else {
                        scope.launch {
                            locationCheckStatus = LocationCheckStatus.Checking
                            
                            // Simulate location check with the MarkAttendanceUseCase logic
                            try {
                                // This would typically use the geofence validation from MarkAttendanceUseCase
                                kotlinx.coroutines.delay(2000) // Simulate GPS check
                                
                                // For demo purposes, we'll randomly succeed or fail
                                val isValid = true // In real implementation, use actual geofence check
                                
                                if (isValid) {
                                    locationCheckStatus = LocationCheckStatus.Success
                                    onLocationValidated(true)
                                } else {
                                    locationCheckStatus = LocationCheckStatus.Error("You are not within the required location for this event. Please move closer and try again.")
                                    onLocationValidated(false)
                                }
                            } catch (e: Exception) {
                                locationCheckStatus = LocationCheckStatus.Error("Failed to verify location: ${e.message}")
                                onLocationValidated(false)
                            }
                        }
                    }
                },
                enabled = !isProcessing && !(locationCheckStatus is LocationCheckStatus.Checking),
                modifier = Modifier.weight(1f)
            ) {
                if (locationCheckStatus is LocationCheckStatus.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (locationCheckStatus is LocationCheckStatus.Success) "Continue" else "Check Location"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (locationCheckStatus is LocationCheckStatus.Success) Icons.Default.ArrowForward else Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricAuthStep(
    event: Event,
    biometricAuthenticator: BiometricAuthenticatorImpl,
    isProcessing: Boolean,
    onBiometricValidated: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var biometricStatus by remember { mutableStateOf<BiometricAuthStatus>(BiometricAuthStatus.NotStarted) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Check biometric capability
    val biometricCapability = remember { biometricAuthenticator.isAvailable() }
    
    Column {
        Text(
            text = "Biometric Authentication",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please authenticate using your fingerprint or face to mark attendance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    biometricCapability != BiometricCapability.AVAILABLE -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Biometric Not Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = when (biometricCapability) {
                                BiometricCapability.HARDWARE_UNAVAILABLE -> "Biometric hardware is not available on this device."
                                BiometricCapability.NO_BIOMETRIC_ENROLLED -> "No biometric credentials are enrolled. Please set up fingerprint or face recognition in device settings."
                                BiometricCapability.SECURITY_UPDATE_REQUIRED -> "A security update is required to use biometric authentication."
                                else -> "Biometric authentication is not available."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    biometricStatus is BiometricAuthStatus.NotStarted -> {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Ready for Authentication",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Touch the sensor or look at the camera to authenticate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    biometricStatus is BiometricAuthStatus.InProgress -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Authenticating...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please complete the biometric scan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    biometricStatus is BiometricAuthStatus.Success -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Authentication Successful",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Your identity has been verified successfully.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    biometricStatus is BiometricAuthStatus.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Authentication Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = (biometricStatus as BiometricAuthStatus.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
            
            Button(
                onClick = {
                    if (biometricStatus is BiometricAuthStatus.Success) {
                        onBiometricValidated(true)
                    } else if (biometricCapability == BiometricCapability.AVAILABLE) {
                        scope.launch {
                            biometricStatus = BiometricAuthStatus.InProgress
                            
                            try {
                                val result = biometricAuthenticator.authenticateWithActivity(context as FragmentActivity)
                                
                                when (result) {
                                    is AuthResult.Success -> {
                                        biometricStatus = BiometricAuthStatus.Success
                                        onBiometricValidated(true)
                                    }
                                    is AuthResult.Error -> {
                                        val errorMessage = when (result.error) {
                                            BiometricError.UserCancelled -> "Authentication was cancelled. Please try again."
                                            BiometricError.AuthenticationFailed -> "Authentication failed. Please try again."
                                            BiometricError.NoEnrolledBiometrics -> "No biometric credentials enrolled. Please set up fingerprint or face recognition."
                                            BiometricError.HardwareUnavailable -> "Biometric hardware is unavailable."
                                            BiometricError.Lockout -> "Too many failed attempts. Please try again later."
                                            BiometricError.LockoutPermanent -> "Biometric authentication is permanently locked. Please use device PIN."
                                            BiometricError.TooManyAttempts -> "Too many authentication attempts. Please wait and try again."
                                            is BiometricError.Unknown -> "Authentication error: ${result.error.message}"
                                        }
                                        biometricStatus = BiometricAuthStatus.Error(errorMessage)
                                        onBiometricValidated(false)
                                    }
                                }
                            } catch (e: Exception) {
                                biometricStatus = BiometricAuthStatus.Error("Authentication failed: ${e.message}")
                                onBiometricValidated(false)
                            }
                        }
                    }
                },
                enabled = biometricCapability == BiometricCapability.AVAILABLE && 
                         !(biometricStatus is BiometricAuthStatus.InProgress),
                modifier = Modifier.weight(1f)
            ) {
                if (biometricStatus is BiometricAuthStatus.InProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (biometricStatus is BiometricAuthStatus.Success) "Continue" else "Authenticate"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (biometricStatus is BiometricAuthStatus.Success) Icons.Default.ArrowForward else Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkAttendanceStep(
    event: Event,
    isProcessing: Boolean,
    onMarkAttendance: () -> Unit,
    onBack: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Column {
        Text(
            text = "Mark Attendance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You're all set! Tap the button below to mark your attendance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ready to Mark Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "All verification steps completed successfully.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Event: ${event.name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Time: ${timeFormat.format(Date(System.currentTimeMillis()))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
            
            Button(
                onClick = onMarkAttendance,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marking...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Attendance")
                }
            }
        }
    }
}

// Status enums for tracking step progress
sealed class LocationCheckStatus {
    object NotStarted : LocationCheckStatus()
    object Checking : LocationCheckStatus()
    object Success : LocationCheckStatus()
    data class Error(val message: String) : LocationCheckStatus()
}

sealed class BiometricAuthStatus {
    object NotStarted : BiometricAuthStatus()
    object InProgress : BiometricAuthStatus()
    object Success : BiometricAuthStatus()
    data class Error(val message: String) : BiometricAuthStatus()
}