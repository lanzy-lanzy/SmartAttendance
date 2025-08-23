package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.error.AppError
import dev.ml.smartattendance.domain.model.error.ErrorContext
import dev.ml.smartattendance.domain.model.error.ErrorSeverity

data class ErrorReport(
    val error: AppError,
    val context: ErrorContext,
    val stackTrace: String,
    val deviceInfo: Map<String, String>
)

interface ErrorHandlerService {
    suspend fun handleError(error: AppError, context: ErrorContext)
    suspend fun logError(error: Throwable, context: ErrorContext)
    fun mapExceptionToAppError(exception: Throwable): AppError
    suspend fun reportCriticalError(errorReport: ErrorReport)
    fun shouldShowErrorToUser(error: AppError): Boolean
}