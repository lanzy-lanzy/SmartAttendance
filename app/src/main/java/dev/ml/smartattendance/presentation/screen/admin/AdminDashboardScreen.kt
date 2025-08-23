package dev.ml.smartattendance.presentation.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ml.smartattendance.presentation.navigation.Screen
import dev.ml.smartattendance.presentation.screen.EventManagementScreen
import dev.ml.smartattendance.presentation.screen.StudentManagementScreen
import dev.ml.smartattendance.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            // Add extra emphasis on the bottom navigation bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                shadowElevation = 8.dp
            ) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Students") },
                        selected = currentScreen == Screen.StudentManagement.route,
                        onClick = {
                            bottomNavController.navigate(Screen.StudentManagement.route) {
                                popUpTo(Screen.StudentManagement.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Event, contentDescription = null) },
                        label = { Text("Events") },
                        selected = currentScreen == Screen.EventManagement.route,
                        onClick = {
                            bottomNavController.navigate(Screen.EventManagement.route) {
                                popUpTo(Screen.EventManagement.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        label = { Text("Reports") },
                        selected = currentScreen == "reports",
                        onClick = {
                            bottomNavController.navigate("reports") {
                                popUpTo("reports") { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentScreen == "settings",
                        onClick = {
                            bottomNavController.navigate("settings") {
                                popUpTo("settings") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.StudentManagement.route,
            modifier = Modifier.padding(paddingValues)
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Attendance reports and analytics will be displayed here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "System settings and configurations will be available here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}