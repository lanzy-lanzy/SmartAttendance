package dev.ml.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.presentation.navigation.Screen
import dev.ml.smartattendance.presentation.navigation.SmartAttendanceNavigation
import dev.ml.smartattendance.ui.theme.LocalFragmentActivity
import dev.ml.smartattendance.ui.theme.SmartAttendanceTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var authService: AuthService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            CompositionLocalProvider(LocalFragmentActivity provides this) {
                SmartAttendanceTheme {
                    var currentUser by remember { mutableStateOf<dev.ml.smartattendance.domain.model.auth.User?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    
                    LaunchedEffect(Unit) {
                        // Check if user is already signed in
                        currentUser = authService.getCurrentUser()
                        isLoading = false
                    }
                    
                    if (!isLoading) {
                        // Get the user role from the current user
                        val userRole = currentUser?.role ?: UserRole.STUDENT
                        
                        // Pass the explicit user role to ensure correct navigation
                        SmartAttendanceNavigation(
                            userRole = userRole
                        )
                    }
                }
            }
        }
    }
}