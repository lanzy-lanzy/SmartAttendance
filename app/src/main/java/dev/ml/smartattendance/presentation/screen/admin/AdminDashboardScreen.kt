package dev.ml.smartattendance.presentation.screen.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.navigation.Screen
import dev.ml.smartattendance.presentation.screen.EventManagementScreen
import dev.ml.smartattendance.presentation.screen.StudentManagementScreen
import dev.ml.smartattendance.presentation.viewmodel.AuthViewModel
import dev.ml.smartattendance.ui.components.*
import dev.ml.smartattendance.ui.theme.*

@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToEventDetail: (String) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val bottomNavController = rememberNavController()
    val backStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry.value?.destination?.route ?: Screen.StudentManagement.route
    
    // Animation states
    var startAnimations by remember { mutableStateOf(false) }
    
    val headerAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutCubic
        ),
        label = "headerAlpha"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 300,
            easing = EaseOutCubic
        ),
        label = "contentAlpha"
    )
    
    // Start animations on composition
    LaunchedEffect(Unit) {
        startAnimations = true
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Admin Dashboard",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        onLogout()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout, 
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = currentScreen,
                userRole = UserRole.ADMIN,
                onNavigate = { route ->
                    try {
                        bottomNavController.navigate(route) {
                            popUpTo(bottomNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } catch (e: Exception) {
                        // Handle navigation errors gracefully
                        e.printStackTrace()
                        // Fallback: navigate without options
                        try {
                            bottomNavController.navigate(route)
                        } catch (fallbackError: Exception) {
                            fallbackError.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.alpha(contentAlpha)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Modern Welcome Header with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Primary,
                            PrimaryVariant
                        )
                    )
                )
                .alpha(headerAlpha)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Manage students, events, and monitor attendance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnPrimary.copy(alpha = 0.9f)
                )
            }
        }
        
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.StudentManagement.route,
            modifier = Modifier
                .padding(paddingValues)
                .alpha(contentAlpha)
        ) {
            composable(Screen.StudentManagement.route) {
                StudentManagementScreen(
                    onNavigateBack = { /* Handled by bottom navigation */ }
                )
            }
            
            composable(Screen.EventManagement.route) {
                EventManagementScreen(
                    onNavigateBack = { /* Handled by bottom navigation */ },
                    onNavigateToEventDetail = onNavigateToEventDetail
                )
            }
            
            composable("reports") {
                ReportsScreen()
            }
            
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun ReportsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Icon with modern background
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Primary
            )
        }
        
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Generate and view attendance reports",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick Report Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Daily Reports",
                description = "View today's attendance summary",
                icon = Icons.Default.Today,
                onClick = { /* TODO: Navigate to daily reports */ }
            )
            
            FeatureCard(
                title = "Weekly Reports",
                description = "Analyze weekly attendance patterns",
                icon = Icons.Default.DateRange,
                onClick = { /* TODO: Navigate to weekly reports */ }
            )
            
            FeatureCard(
                title = "Export Reports",
                description = "Download attendance data as Excel/PDF",
                icon = Icons.Default.Download,
                onClick = { /* TODO: Export functionality */ }
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Icon with modern background
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Secondary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Secondary
            )
        }
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Configure system preferences and security",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Settings Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "System Settings",
                description = "Configure app behavior and preferences",
                icon = Icons.Default.SettingsApplications,
                onClick = { /* TODO: Navigate to system settings */ }
            )
            
            FeatureCard(
                title = "Security Settings",
                description = "Manage biometric and authentication settings",
                icon = Icons.Default.Security,
                onClick = { /* TODO: Navigate to security settings */ }
            )
            
            FeatureCard(
                title = "Backup & Sync",
                description = "Configure data backup and synchronization",
                icon = Icons.Default.CloudSync,
                onClick = { /* TODO: Navigate to backup settings */ }
            )
            
            FeatureCard(
                title = "About",
                description = "App version and information",
                icon = Icons.Default.Info,
                onClick = { /* TODO: Show about dialog */ }
            )
        }
    }
}