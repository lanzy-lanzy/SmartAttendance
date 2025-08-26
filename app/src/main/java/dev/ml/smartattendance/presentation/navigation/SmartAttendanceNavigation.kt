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
import dev.ml.smartattendance.presentation.screen.auth.SplashScreen

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
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToStudentDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdminDashboard = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
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
                        navController.navigate(Screen.AttendanceMarking.createRoute(eventId))
                    }
                },
                onNavigateToAdminDashboard = {
                    if (role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route)
                    }
                },
                onNavigateToAttendanceHistory = {
                    navController.navigate(Screen.AttendanceHistory.route)
                },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToEventDetail = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
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
        
        composable(Screen.AttendanceMarking.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Only allow students to access this screen
            if (role == UserRole.STUDENT) {
                AttendanceMarkingScreen(
                    eventId = eventId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAttendanceMarked = {
                        // Navigate back to events list after successful attendance marking
                        // so user can see the "Already Marked" status
                        navController.popBackStack(Screen.Events.route, false)
                    }
                )
            }
        }
        
        composable(Screen.StudentManagement.route) {
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT
            
            // Only allow admins to access this screen
            if (role == UserRole.ADMIN) {
                dev.ml.smartattendance.presentation.screen.admin.ComprehensiveStudentManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToStudentDetail = { studentId ->
                        // TODO: Navigate to student detail screen
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
                        android.util.Log.d("Navigation", "EventManagement: Navigating to event detail with ID: $eventId")
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }
        }
        
        composable(Screen.EventDetail.route) { backStackEntry ->
            // Use the current user role
            val role = currentUserRole ?: UserRole.STUDENT

            // Extract event ID properly from navigation arguments with better error handling
            val rawEventId = backStackEntry.arguments?.getString("eventId")
            val eventId = rawEventId?.trim() ?: ""

            // Log the event ID being processed
            android.util.Log.d("Navigation", "Processing navigation to event details for ID: '$eventId'")

            if (eventId.isNotEmpty()) {
                if (role == UserRole.ADMIN) {
                    // Admin view with comprehensive management features
                    dev.ml.smartattendance.presentation.screen.admin.ComprehensiveEventDetailScreen(
                        eventId = eventId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    // Student view with basic event details and attendance marking
                    dev.ml.smartattendance.presentation.screen.StudentEventDetailScreen(
                        eventId = eventId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAttendanceMarking = { id ->
                            navController.navigate(Screen.AttendanceMarking.createRoute(id))
                        }
                    )
                }
            } else {
                // Handle invalid event ID by showing an error and navigating back
                android.util.Log.e("Navigation", "Invalid event ID in navigation: '$rawEventId'")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
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
                        // Handle logout - navigate to splash for auth check
                        navController.navigate(Screen.Splash.route) {
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
        
        composable(Screen.AttendanceHistory.route) {
            val role = currentUserRole ?: UserRole.STUDENT
            
            if (role == UserRole.STUDENT) {
                AttendanceHistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAttendanceHistory = {
                        // Already here
                    },
                    onNavigateToEvents = {
                        navController.navigate(Screen.Events.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route)
                    }
                )
            }
        }
        
        composable(Screen.Events.route) {
            val role = currentUserRole ?: UserRole.STUDENT
            
            if (role == UserRole.STUDENT) {
                EventsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAttendanceHistory = {
                        navController.navigate(Screen.AttendanceHistory.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToAttendanceMarking = { eventId ->
                        navController.navigate(Screen.AttendanceMarking.createRoute(eventId))
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route)
                    },
                    onNavigateToEventDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }
        }
        
        composable(Screen.Profile.route) {
            val role = currentUserRole ?: UserRole.STUDENT
            
            if (role == UserRole.STUDENT) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAttendanceHistory = {
                        navController.navigate(Screen.AttendanceHistory.route)
                    },
                    onNavigateToEvents = {
                        navController.navigate(Screen.Events.route)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                        // Reset user role after logout
                        currentUserRole = UserRole.STUDENT
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route)
                    }
                )
            }
        }
    }
}