package dev.ml.smartattendance.data.service

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import dev.ml.smartattendance.domain.model.biometric.*
import dev.ml.smartattendance.domain.service.BiometricAuthenticator
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class BiometricAuthenticatorImpl @Inject constructor(
    private val context: Context
) : BiometricAuthenticator {
    
    override fun isAvailable(): BiometricCapability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NO_BIOMETRIC_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
            else -> BiometricCapability.NOT_AVAILABLE
        }
    }
    
    override suspend fun authenticate(): AuthResult {
        // This method is now deprecated - authentication should be handled in UI layer
        // Return error to indicate that UI-based authentication should be used instead
        return AuthResult.Error(BiometricError.Unknown("Use UI-based authentication"))
    }
    
    suspend fun authenticateWithActivity(activity: FragmentActivity): AuthResult = suspendCancellableCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricError.HardwareUnavailable
                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> BiometricError.AuthenticationFailed
                    BiometricPrompt.ERROR_TIMEOUT -> BiometricError.AuthenticationFailed
                    BiometricPrompt.ERROR_NO_SPACE -> BiometricError.AuthenticationFailed
                    BiometricPrompt.ERROR_CANCELED -> BiometricError.UserCancelled
                    BiometricPrompt.ERROR_LOCKOUT -> BiometricError.Lockout
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricError.LockoutPermanent
                    BiometricPrompt.ERROR_USER_CANCELED -> BiometricError.UserCancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricError.NoEnrolledBiometrics
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> BiometricError.HardwareUnavailable
                    else -> BiometricError.Unknown(errString.toString())
                }
                continuation.resume(AuthResult.Error(error))
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                continuation.resume(AuthResult.Success)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                continuation.resume(AuthResult.Error(BiometricError.AuthenticationFailed))
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use your biometric credential to mark attendance")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
        
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }
    
    override suspend fun enrollBiometric(userId: String): EnrollmentResult {
        // For now, we'll return success as actual enrollment happens in system settings
        // In a production app, you might guide users to system settings
        return when (isAvailable()) {
            BiometricCapability.AVAILABLE -> EnrollmentResult.Success
            BiometricCapability.NO_BIOMETRIC_ENROLLED -> EnrollmentResult.Error(BiometricError.NoEnrolledBiometrics)
            BiometricCapability.HARDWARE_UNAVAILABLE -> EnrollmentResult.Error(BiometricError.HardwareUnavailable)
            else -> EnrollmentResult.Error(BiometricError.HardwareUnavailable)
        }
    }
}