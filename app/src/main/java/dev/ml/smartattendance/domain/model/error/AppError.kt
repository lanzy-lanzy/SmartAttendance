package dev.ml.smartattendance.domain.model.error

sealed class AppError : Exception() {
    abstract val userMessage: String
    abstract val technicalMessage: String
    abstract val severity: ErrorSeverity
    
    data class NetworkError(
        override val userMessage: String = "Network connection problem. Please check your internet connection.",
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ) : AppError()
    
    data class DatabaseError(
        override val userMessage: String = "Data storage problem. Please try again.",
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.HIGH
    ) : AppError()
    
    data class BiometricError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ) : AppError()
    
    data class LocationError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ) : AppError()
    
    data class PermissionError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.HIGH
    ) : AppError()
    
    data class ValidationError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.LOW
    ) : AppError()
    
    data class SecurityError(
        override val userMessage: String = "Security check failed. Please contact administrator.",
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.CRITICAL
    ) : AppError()
    
    data class UnknownError(
        override val userMessage: String = "An unexpected error occurred. Please try again.",
        override val technicalMessage: String,
        override val severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ) : AppError()
}

enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class ErrorContext(
    val userId: String?,
    val screen: String?,
    val action: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalData: Map<String, Any> = emptyMap()
)