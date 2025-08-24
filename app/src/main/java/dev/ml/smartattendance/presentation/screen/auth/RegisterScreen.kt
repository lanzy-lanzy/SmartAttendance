package dev.ml.smartattendance.presentation.screen.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.model.auth.AdminLevel
import dev.ml.smartattendance.presentation.viewmodel.AuthViewModel
import dev.ml.smartattendance.presentation.viewmodel.AuthUiState
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.STUDENT) }
    var studentId by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var showPasswordMismatch by remember { mutableStateOf(false) }
    
    // Animation states
    var startAnimations by remember { mutableStateOf(false) }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutCubic
        ),
        label = "contentAlpha"
    )
    
    // Start animations on composition
    LaunchedEffect(Unit) {
        startAnimations = true
    }
    
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }
    
    // Password validation
    LaunchedEffect(password, confirmPassword) {
        showPasswordMismatch = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary,
                        PrimaryVariant,
                        Gradient3
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha)
            ) {
                // Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(
                                color = OnPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OnPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = OnPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = OnPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Join the secure attendance system",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnPrimary.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registration Form
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                colors = CardDefaults.cardColors(
                    containerColor = OnPrimary
                )
            ) {
                // Personal Information Section
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Name Field
                ModernTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Email Field
                ModernTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // Role Selection
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = role == UserRole.STUDENT,
                        onClick = { role = UserRole.STUDENT },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Student")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    FilterChip(
                        selected = role == UserRole.ADMIN,
                        onClick = { role = UserRole.ADMIN },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Student-specific fields
                if (role == UserRole.STUDENT) {
                    Text(
                        text = "Student Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ModernTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = "Student ID",
                        leadingIcon = Icons.Default.Badge,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    ModernTextField(
                        value = course,
                        onValueChange = { course = it },
                        label = "Course/Program",
                        leadingIcon = Icons.Default.School,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
                
                // Security Section
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Password Field
                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Confirm Password Field
                ModernTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    isError = showPasswordMismatch,
                    errorMessage = if (showPasswordMismatch) "Passwords do not match" else null,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Error Message
                if (uiState is AuthUiState.Error) {
                    AlertCard(
                        message = (uiState as AuthUiState.Error).message,
                        type = AlertType.Error,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                
                // Register Button
                val isFormValid = name.isNotBlank() && 
                                  email.isNotBlank() && 
                                  password.isNotBlank() && 
                                  confirmPassword.isNotBlank() && 
                                  password == confirmPassword &&
                                  (role != UserRole.STUDENT || (studentId.isNotBlank() && course.isNotBlank()))
                
                GradientButton(
                    onClick = {
                        viewModel.signUp(
                            email = email,
                            password = password,
                            name = name,
                            role = role,
                            studentId = if (role == UserRole.STUDENT) studentId else null,
                            course = if (role == UserRole.STUDENT) course else null,
                            adminLevel = if (role == UserRole.ADMIN) AdminLevel.BASIC else AdminLevel.BASIC
                        )
                    },
                    enabled = isFormValid && uiState !is AuthUiState.Loading,
                    isLoading = uiState is AuthUiState.Loading,
                    text = "Create Account",
                    icon = Icons.Default.PersonAdd,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Login Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha)
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OnPrimary
                    )
                ) {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}