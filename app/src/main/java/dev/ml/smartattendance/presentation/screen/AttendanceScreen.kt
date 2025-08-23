package dev.ml.smartattendance.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import dev.ml.smartattendance.presentation.viewmodel.AttendanceViewModel
import dev.ml.smartattendance.ui.theme.LocalFragmentActivity
import android.content.Context
import android.content.ContextWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    eventId: String,
    studentId: String = "STUDENT001", // This should come from user session
    onNavigateBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentEvents()
    }
    
    // Clear messages after showing them
    LaunchedEffect(state.attendanceMessage, state.error) {
        if (state.attendanceMessage != null || state.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Event Information
            val event = state.currentEvents.find { it.id == eventId }
            
            if (event != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Location: ${event.latitude}, ${event.longitude}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Radius: ${event.geofenceRadius}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Attendance Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instructions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Ensure you are within the event location\n" +
                                    "2. Your biometric authentication will be required\n" +
                                    "3. Location verification will be performed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Mark Attendance Button
                val context = LocalContext.current
                val fragmentActivity = LocalFragmentActivity.current
                
                Button(
                    onClick = {
                        // First check location permissions
                        val activity = fragmentActivity ?: findActivity(context)
                        if (activity != null) {
                            if (hasLocationPermissions(context)) {
                                // Location permissions granted, proceed with biometric authentication
                                proceedWithBiometricAuth(context, activity, viewModel, studentId, eventId)
                            } else {
                                // Request location permissions
                                requestLocationPermissions(activity) {
                                    if (hasLocationPermissions(context)) {
                                        proceedWithBiometricAuth(context, activity, viewModel, studentId, eventId)
                                    } else {
                                        viewModel.setBiometricError("Location permission is required to mark attendance. Please grant location access in app settings.")
                                    }
                                }
                            }
                        } else {
                            viewModel.setBiometricError("Unable to access activity context. Please restart the app and try again.")
                        }
                    },
                    enabled = !state.isMarkingAttendance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.isMarkingAttendance) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marking Attendance...")
                    } else {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticate & Mark Attendance")
                    }
                }
                
                // Success/Error Messages
                state.attendanceMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                // Event not found
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Event not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// Helper function to check location permissions
private fun hasLocationPermissions(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

// Helper function to request location permissions
private fun requestLocationPermissions(activity: FragmentActivity, onResult: () -> Unit) {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    ActivityCompat.requestPermissions(activity, permissions, 1001)
    
    // Note: In a real app, you'd need to handle the result in onRequestPermissionsResult
    // For now, we'll assume the user grants permission and call onResult
    onResult()
}

// Helper function to proceed with biometric authentication after location permissions are granted
private fun proceedWithBiometricAuth(
    context: Context,
    activity: FragmentActivity,
    viewModel: AttendanceViewModel,
    studentId: String,
    eventId: String
) {
    val biometricManager = BiometricManager.from(context)
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    viewModel.markAttendance(studentId, eventId)
                },
                onError = { error ->
                    viewModel.setBiometricError(error)
                }
            )
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            viewModel.setBiometricError("No biometric hardware available on this device")
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            viewModel.setBiometricError("Biometric hardware currently unavailable")
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            viewModel.setBiometricError("No biometric credentials enrolled. Please set up fingerprint or face recognition in device settings.")
        }
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
            viewModel.setBiometricError("Security update required for biometric authentication")
        }
        else -> {
            viewModel.setBiometricError("Biometric authentication not available on this device")
        }
    }
}

// Helper function to find FragmentActivity from context
private fun findActivity(context: Context): FragmentActivity? {
    // First, check if context is already a FragmentActivity
    if (context is FragmentActivity) {
        return context
    }
    
    // If not, traverse the context wrapper chain
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
        
        // Prevent infinite loop
        if (currentContext == context) {
            break
        }
    }
    
    return null
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val errorMessage = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED -> "Authentication cancelled by user"
                    BiometricPrompt.ERROR_LOCKOUT -> "Too many failed attempts. Please try again in 30 seconds."
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometric authentication locked. Please use device PIN/password."
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> "No biometric credentials enrolled. Please set up fingerprint or face recognition."
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> "No biometric hardware found on this device"
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Biometric hardware currently unavailable"
                    BiometricPrompt.ERROR_NO_SPACE -> "Not enough storage space for biometric authentication"
                    BiometricPrompt.ERROR_TIMEOUT -> "Authentication timed out. Please try again."
                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "Unable to process biometric data. Please try again."
                    BiometricPrompt.ERROR_VENDOR -> "Vendor-specific biometric error occurred"
                    else -> "Authentication failed: $errString"
                }
                onError(errorMessage)
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Please try again.")
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mark Attendance")
            .setSubtitle("Use your fingerprint or face to verify your identity")
            .setDescription("Touch the fingerprint sensor or look at the camera to authenticate and mark your attendance")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setConfirmationRequired(true)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
        
    } catch (e: Exception) {
        onError("Failed to initialize biometric authentication: ${e.message}")
    }
}