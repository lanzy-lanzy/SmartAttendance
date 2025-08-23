package dev.ml.smartattendance.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.screen.*
import dev.ml.smartattendance.presentation.screen.admin.AdminDashboardScreen
import dev.ml.smartattendance.presentation.screen.admin.EventDetailScreen
import dev.ml.smartattendance.presentation.screen.auth.LoginScreen
import dev.ml.smartattendance.presentation.screen.auth.RegisterScreen

@Composable
fun SmartAttendanceNavigation(
    navController: NavHostController = rememberNavController(),
    userRole: UserRole = UserRole.STUDENT // This should come from authentication state
) {
    // Automatic redirect for admin users
    LaunchedEffect(userRole) {
        if (userRole == UserRole.ADMIN && navController.currentBackStackEntry?.destination?.route == Screen.Dashboard.route) {
            navController.navigate(Screen.AdminDashboard.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // ... existing login, register routes ...
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    if (userRole == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    if (userRole == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                userRole = userRole,
                onNavigateToStudentManagement = {
                    if (userRole == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                },
                onNavigateToEventManagement = {
                    if (userRole == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                },
                onNavigateToAttendance = { eventId ->
                    if (userRole == UserRole.STUDENT) {
                        navController.navigate(Screen.Attendance.createRoute(eventId))
                    }
                },
                onNavigateToAdminDashboard = {
                    if (userRole == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                }
            )
        }
        
        composable(Screen.Attendance.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            AttendanceScreen(
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentManagement.route) {
            // Only allow admins to access this screen
            if (userRole == UserRole.ADMIN) {
                StudentManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.EventManagement.route) {
            // Only allow admins to access this screen
            if (userRole == UserRole.ADMIN) {
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
            // Only allow admins to access this screen
            if (userRole == UserRole.ADMIN) {
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
            if (userRole == UserRole.ADMIN) {
                AdminDashboardScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        // Handle logout - this would typically navigate back to login
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToEventDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }
        }
    }
}