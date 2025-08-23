package dev.ml.smartattendance.data.service

import android.content.Context
import android.os.Build
import android.util.Log
import dev.ml.smartattendance.domain.model.error.AppError
import dev.ml.smartattendance.domain.model.error.ErrorContext
import dev.ml.smartattendance.domain.model.error.ErrorSeverity
import dev.ml.smartattendance.domain.service.ErrorHandlerService
import dev.ml.smartattendance.domain.service.ErrorReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandlerServiceImpl @Inject constructor(
    private val context: Context
) : ErrorHandlerService {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logTag = "SmartAttendanceError"
    
    override suspend fun handleError(error: AppError, errorContext: ErrorContext) {
        // Log the error
        logError(error, errorContext)
        
        // If critical, prepare for reporting
        if (error.severity == ErrorSeverity.CRITICAL) {
            val errorReport = createErrorReport(error, errorContext)
            reportCriticalError(errorReport)
        }
    }
    
    override suspend fun logError(error: Throwable, errorContext: ErrorContext): Unit = withContext(Dispatchers.IO) {
        val timestamp = dateFormat.format(Date())
        val stackTrace = getStackTrace(error)
        
        // Log to Android Log
        when {
            error is AppError && error.severity == ErrorSeverity.CRITICAL -> {
                Log.e(logTag, "CRITICAL: ${error.technicalMessage}", error)
            }
            error is AppError && error.severity == ErrorSeverity.HIGH -> {
                Log.e(logTag, "HIGH: ${error.technicalMessage}", error)
            }
            error is AppError && error.severity == ErrorSeverity.MEDIUM -> {
                Log.w(logTag, "MEDIUM: ${error.technicalMessage}", error)
            }
            else -> {
                Log.i(logTag, "LOW: ${error.message}", error)
            }
        }
        
        // Write to local log file
        try {
            val logFile = File(this@ErrorHandlerServiceImpl.context.getExternalFilesDir(null), "error_log.txt")
            FileWriter(logFile, true).use { writer ->
                writer.append("\n--- ERROR LOG ENTRY ---\n")
                writer.append("Timestamp: $timestamp\n")
                writer.append("Error Type: ${error.javaClass.simpleName}\n")
                writer.append("Message: ${error.message}\n")
                writer.append("Context: ${errorContext}\n")
                writer.append("Stack Trace:\n$stackTrace\n")
                writer.append("--- END ENTRY ---\n\n")
            }
        } catch (e: Exception) {
            Log.e(logTag, "Failed to write to log file", e)
        }
    }
    
    override fun mapExceptionToAppError(exception: Throwable): AppError {
        return when (exception) {
            is java.net.UnknownHostException, is java.net.ConnectException -> {
                AppError.NetworkError(
                    technicalMessage = exception.message ?: "Network connection failed"
                )
            }
            is java.net.SocketTimeoutException -> {
                AppError.NetworkError(
                    userMessage = "Request timed out. Please try again.",
                    technicalMessage = exception.message ?: "Socket timeout"
                )
            }
            is SecurityException -> {
                AppError.PermissionError(
                    userMessage = "Permission denied. Please grant required permissions.",
                    technicalMessage = exception.message ?: "Security exception"
                )
            }
            is IllegalArgumentException -> {
                AppError.ValidationError(
                    userMessage = "Invalid input provided.",
                    technicalMessage = exception.message ?: "Validation failed"
                )
            }
            is RuntimeException -> {
                when {
                    exception.message?.contains("database", ignoreCase = true) == true -> {
                        AppError.DatabaseError(
                            technicalMessage = exception.message ?: "Database operation failed"
                        )
                    }
                    exception.message?.contains("biometric", ignoreCase = true) == true -> {
                        AppError.BiometricError(
                            userMessage = "Biometric authentication failed. Please try again.",
                            technicalMessage = exception.message ?: "Biometric error"
                        )
                    }
                    exception.message?.contains("location", ignoreCase = true) == true -> {
                        AppError.LocationError(
                            userMessage = "Location access failed. Please enable location services.",
                            technicalMessage = exception.message ?: "Location error"
                        )
                    }
                    else -> {
                        AppError.UnknownError(
                            technicalMessage = exception.message ?: "Unknown runtime exception"
                        )
                    }
                }
            }
            else -> {
                AppError.UnknownError(
                    technicalMessage = exception.message ?: "Unknown exception: ${exception.javaClass.simpleName}"
                )
            }
        }
    }
    
    override suspend fun reportCriticalError(errorReport: ErrorReport): Unit = withContext(Dispatchers.IO) {
        // In a real app, this would send the error report to a remote server
        // For now, we'll just save it locally
        try {
            val reportFile = File(context.getExternalFilesDir(null), "critical_errors.txt")
            FileWriter(reportFile, true).use { writer ->
                writer.append("\n=== CRITICAL ERROR REPORT ===\n")
                writer.append("Timestamp: ${dateFormat.format(Date(errorReport.context.timestamp))}\n")
                writer.append("User ID: ${errorReport.context.userId ?: "Unknown"}\n")
                writer.append("Screen: ${errorReport.context.screen ?: "Unknown"}\n")
                writer.append("Action: ${errorReport.context.action ?: "Unknown"}\n")
                writer.append("Error Type: ${errorReport.error.javaClass.simpleName}\n")
                writer.append("User Message: ${errorReport.error.userMessage}\n")
                writer.append("Technical Message: ${errorReport.error.technicalMessage}\n")
                writer.append("Severity: ${errorReport.error.severity}\n")
                writer.append("Device Info:\n")
                errorReport.deviceInfo.forEach { (key, value) ->
                    writer.append("  $key: $value\n")
                }
                writer.append("Stack Trace:\n${errorReport.stackTrace}\n")
                writer.append("=== END REPORT ===\n\n")
            }
            
            Log.e(logTag, "Critical error reported and saved to file")
        } catch (e: Exception) {
            Log.e(logTag, "Failed to save critical error report", e)
        }
    }
    
    override fun shouldShowErrorToUser(error: AppError): Boolean {
        return when (error.severity) {
            ErrorSeverity.LOW, ErrorSeverity.MEDIUM -> true
            ErrorSeverity.HIGH -> true // Show but with more serious tone
            ErrorSeverity.CRITICAL -> false // Handle differently, maybe show generic message
        }
    }
    
    private fun createErrorReport(error: AppError, errorContext: ErrorContext): ErrorReport {
        val deviceInfo = mapOf(
            "device_model" to Build.MODEL,
            "device_manufacturer" to Build.MANUFACTURER,
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT.toString(),
            "app_version" to getAppVersion()
        )
        
        return ErrorReport(
            error = error,
            context = errorContext,
            stackTrace = getStackTrace(error),
            deviceInfo = deviceInfo
        )
    }
    
    private fun getStackTrace(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}