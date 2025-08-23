package dev.ml.smartattendance.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.presentation.screen.*
import dev.ml.smartattendance.presentation.screen.admin.AdminDashboardScreen
import dev.ml.smartattendance.presentation.screen.admin.EventDetailScreen
import dev.ml.smartattendance.presentation.screen.auth.LoginScreen
import dev.ml.smartattendance.presentation.screen.auth.RegisterScreen

@Composable
fun SmartAttendanceNavigation(
    navController: NavHostController = rememberNavController(),
    authService: AuthService
) {
    // Get the current user role dynamically
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    var shouldRefreshRole by remember { mutableStateOf(false) }
    var navigateAfterLogin by remember { mutableStateOf(false) }
    var navigateAfterRegister by remember { mutableStateOf(false) }
    
    // Fetch current user role when navigation starts or when refresh is requested
    LaunchedEffect(Unit, shouldRefreshRole) {
        if (shouldRefreshRole) {
            val currentUser = authService.getCurrentUser()
            currentUserRole = currentUser?.role ?: UserRole.STUDENT
            shouldRefreshRole = false
            
            // Handle navigation after role refresh
            if (navigateAfterLogin || navigateAfterRegister) {
                val role = currentUserRole ?: UserRole.STUDENT
                if (role == UserRole.ADMIN) {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(if (navigateAfterLogin) Screen.Login.route else Screen.Register.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(if (navigateAfterLogin) Screen.Login.route else Screen.Register.route) { inclusive = true }
                    }
                }
                navigateAfterLogin = false
                navigateAfterRegister = false
            }
        } else {
            // Initial load
            val currentUser = authService.getCurrentUser()
            currentUserRole = currentUser?.role ?: UserRole.STUDENT
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    // Trigger role refresh and navigation
                    shouldRefreshRole = true
                    navigateAfterLogin = true
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // Trigger role refresh and navigation
                    shouldRefreshRole = true
                    navigateAfterRegister = true
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Automatic redirect for admin users
            LaunchedEffect(Unit) {
                if (role == UserRole.ADMIN) {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
            
            DashboardScreen(
                userRole = role,
                onNavigateToStudentManagement = {
                    if (role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                },
                onNavigateToEventManagement = {
                    if (role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                },
                onNavigateToAttendance = { eventId ->
                    if (role == UserRole.STUDENT) {
                        navController.navigate(Screen.Attendance.createRoute(eventId))
                    }
                },
                onNavigateToAdminDashboard = {
                    if (role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                }
            )
        }
        
        composable(Screen.Attendance.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            AttendanceScreen(
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentManagement.route) {
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Only allow admins to access this screen
            if (role == UserRole.ADMIN) {
                StudentManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.EventManagement.route) {
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Only allow admins to access this screen
            if (role == UserRole.ADMIN) {
                EventManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEventDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }
        }
        
        composable(Screen.EventDetail.route) { backStackEntry ->
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Only allow admins to access this screen
            if (role == UserRole.ADMIN) {
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                EventDetailScreen(
                    eventId = eventId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.AdminDashboard.route) {
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            if (role == UserRole.ADMIN) {
                AdminDashboardScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        // Handle logout - this would typically navigate back to login
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                        }
                        // Reset user role after logout
                        currentUserRole = UserRole.STUDENT
                    },
                    onNavigateToEventDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }
        }
    }
}