package dev.ml.smartattendance.domain.model.biometric

sealed class BiometricError : Exception() {
    object HardwareUnavailable : BiometricError()
    object NoEnrolledBiometrics : BiometricError()
    object AuthenticationFailed : BiometricError()
    object UserCancelled : BiometricError()
    object TooManyAttempts : BiometricError()
    object Lockout : BiometricError()
    object LockoutPermanent : BiometricError()
    data class Unknown(override val message: String) : BiometricError()
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val error: BiometricError) : AuthResult()
}

sealed class EnrollmentResult {
    object Success : EnrollmentResult()
    data class Error(val error: BiometricError) : EnrollmentResult()
}

enum class BiometricCapability {
    AVAILABLE,
    HARDWARE_UNAVAILABLE,
    NO_BIOMETRIC_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    NOT_AVAILABLE
}

sealed class FaceDetectionResult {
    data class Success(val faceData: ByteArray) : FaceDetectionResult()
    object NoFaceDetected : FaceDetectionResult()
    object MultipleFacesDetected : FaceDetectionResult()
    data class Error(val message: String) : FaceDetectionResult()
}

sealed class LivenessResult {
    object Live : LivenessResult()
    object NotLive : LivenessResult()
    data class Error(val message: String) : LivenessResult()
}