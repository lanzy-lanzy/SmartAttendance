package dev.ml.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.ml.smartattendance.domain.service.AuthService
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
                    SmartAttendanceNavigation(
                        authService = authService
                    )
                }
            }
        }
    }
}