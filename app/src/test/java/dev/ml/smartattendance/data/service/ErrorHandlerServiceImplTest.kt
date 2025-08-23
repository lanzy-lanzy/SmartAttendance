package dev.ml.smartattendance.data.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dev.ml.smartattendance.domain.model.error.*
import dev.ml.smartattendance.domain.service.ErrorReport
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandlerServiceImplTest {

    private lateinit var errorHandlerService: ErrorHandlerServiceImpl
    private val mockContext = mockk<Context>()
    private val mockPackageManager = mockk<PackageManager>()
    private val mockFile = mockk<File>()
    private val mockFileWriter = mockk<FileWriter>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock context and dependencies
        every { mockContext.getExternalFilesDir(null) } returns mockFile
        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.packageName } returns "dev.ml.smartattendance"
        
        // Mock package info
        val mockPackageInfo = mockk<PackageInfo>()
        mockPackageInfo.versionName = "1.0.0"
        mockPackageInfo.versionCode = 1
        every { mockPackageManager.getPackageInfo("dev.ml.smartattendance", 0) } returns mockPackageInfo
        
        // Mock file operations
        every { File(mockFile, any<String>()) } returns mockFile
        mockkConstructor(FileWriter::class)
        every { anyConstructed<FileWriter>().use(any()) } answers {
            val block = firstArg<(FileWriter) -> Unit>()
            block(mockFileWriter)
        }
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any(), any<Throwable>()) } returns 0
        every { Log.i(any(), any(), any<Throwable>()) } returns 0
        
        errorHandlerService = ErrorHandlerServiceImpl(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `handleError should log error and not report non-critical errors`() = runTest {
        // Given
        val error = AppError.ValidationError(
            userMessage = "Invalid input",
            technicalMessage = "Validation failed"
        )
        val context = ErrorContext(
            userId = "user123",
            screen = "login",
            action = "submit",
            timestamp = System.currentTimeMillis()
        )

        // When
        errorHandlerService.handleError(error, context)

        // Then
        verify { Log.w("SmartAttendanceError", "MEDIUM: Validation failed", error) }
        verify { mockFileWriter.append(match { it.contains("ERROR LOG ENTRY") }) }
        verify { mockFileWriter.append(match { it.contains("Invalid input") }) }
    }

    @Test
    fun `handleError should report critical errors`() = runTest {
        // Given
        val error = AppError.DatabaseError(
            technicalMessage = "Database connection lost",
            severity = ErrorSeverity.CRITICAL
        )
        val context = ErrorContext(
            userId = "user123",
            screen = "attendance",
            action = "mark_attendance",
            timestamp = System.currentTimeMillis()
        )

        // When
        errorHandlerService.handleError(error, context)

        // Then
        verify { Log.e("SmartAttendanceError", "CRITICAL: Database connection lost", error) }
        verify { mockFileWriter.append(match { it.contains("CRITICAL ERROR REPORT") }) }
        verify { mockFileWriter.append(match { it.contains("Database connection lost") }) }
    }

    @Test
    fun `logError should write to both Android Log and file`() = runTest {
        // Given
        val error = RuntimeException("Test error")
        val context = ErrorContext(
            userId = "user123",
            screen = "test_screen",
            action = "test_action",
            timestamp = System.currentTimeMillis()
        )

        // When
        errorHandlerService.logError(error, context)

        // Then
        verify { Log.i("SmartAttendanceError", "LOW: Test error", error) }
        verify { mockFileWriter.append(match { it.contains("ERROR LOG ENTRY") }) }
        verify { mockFileWriter.append(match { it.contains("Test error") }) }
        verify { mockFileWriter.append(match { it.contains("RuntimeException") }) }
    }

    @Test
    fun `mapExceptionToAppError should map UnknownHostException to NetworkError`() {
        // Given
        val exception = UnknownHostException("Unable to resolve host")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.NetworkError)
        assertEquals("Unable to resolve host", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map ConnectException to NetworkError`() {
        // Given
        val exception = ConnectException("Connection refused")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.NetworkError)
        assertEquals("Connection refused", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map SocketTimeoutException to NetworkError with user message`() {
        // Given
        val exception = SocketTimeoutException("timeout")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.NetworkError)
        assertEquals("Request timed out. Please try again.", result.userMessage)
        assertEquals("timeout", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map SecurityException to PermissionError`() {
        // Given
        val exception = SecurityException("Permission denied")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.PermissionError)
        assertEquals("Permission denied. Please grant required permissions.", result.userMessage)
        assertEquals("Permission denied", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map IllegalArgumentException to ValidationError`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.ValidationError)
        assertEquals("Invalid input provided.", result.userMessage)
        assertEquals("Invalid argument", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map database RuntimeException to DatabaseError`() {
        // Given
        val exception = RuntimeException("Database operation failed")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.DatabaseError)
        assertEquals("Database operation failed", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map biometric RuntimeException to BiometricError`() {
        // Given
        val exception = RuntimeException("Biometric authentication failed")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.BiometricError)
        assertEquals("Biometric authentication failed. Please try again.", result.userMessage)
        assertEquals("Biometric authentication failed", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map location RuntimeException to LocationError`() {
        // Given
        val exception = RuntimeException("Location services unavailable")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.LocationError)
        assertEquals("Location access failed. Please enable location services.", result.userMessage)
        assertEquals("Location services unavailable", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map unknown RuntimeException to UnknownError`() {
        // Given
        val exception = RuntimeException("Unknown runtime error")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.UnknownError)
        assertEquals("Unknown runtime error", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should map unknown exception to UnknownError`() {
        // Given
        val exception = Exception("Generic exception")

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.UnknownError)
        assertEquals("Generic exception", result.technicalMessage)
    }

    @Test
    fun `mapExceptionToAppError should handle null exception message`() {
        // Given
        val exception = RuntimeException()

        // When
        val result = errorHandlerService.mapExceptionToAppError(exception)

        // Then
        assertTrue(result is AppError.UnknownError)
        assertTrue(result.technicalMessage.contains("Unknown runtime exception"))
    }

    @Test
    fun `shouldShowErrorToUser should return true for low severity`() {
        // Given
        val error = AppError.ValidationError(
            userMessage = "Test error",
            technicalMessage = "Test technical",
            severity = ErrorSeverity.LOW
        )

        // When
        val result = errorHandlerService.shouldShowErrorToUser(error)

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldShowErrorToUser should return true for medium severity`() {
        // Given
        val error = AppError.ValidationError(
            userMessage = "Test error",
            technicalMessage = "Test technical",
            severity = ErrorSeverity.MEDIUM
        )

        // When
        val result = errorHandlerService.shouldShowErrorToUser(error)

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldShowErrorToUser should return true for high severity`() {
        // Given
        val error = AppError.DatabaseError(
            technicalMessage = "Test technical",
            severity = ErrorSeverity.HIGH
        )

        // When
        val result = errorHandlerService.shouldShowErrorToUser(error)

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldShowErrorToUser should return false for critical severity`() {
        // Given
        val error = AppError.DatabaseError(
            technicalMessage = "Test technical",
            severity = ErrorSeverity.CRITICAL
        )

        // When
        val result = errorHandlerService.shouldShowErrorToUser(error)

        // Then
        assertFalse(result)
    }

    @Test
    fun `reportCriticalError should save error report to file`() = runTest {
        // Given
        val error = AppError.DatabaseError(
            technicalMessage = "Critical database error",
            severity = ErrorSeverity.CRITICAL
        )
        val context = ErrorContext(
            userId = "user123",
            screen = "attendance",
            action = "mark_attendance",
            timestamp = System.currentTimeMillis()
        )
        val errorReport = ErrorReport(
            error = error,
            context = context,
            stackTrace = "Stack trace here",
            deviceInfo = mapOf(
                "device_model" to Build.MODEL,
                "android_version" to Build.VERSION.RELEASE
            )
        )

        // When
        errorHandlerService.reportCriticalError(errorReport)

        // Then
        verify { mockFileWriter.append(match { it.contains("CRITICAL ERROR REPORT") }) }
        verify { mockFileWriter.append(match { it.contains("user123") }) }
        verify { mockFileWriter.append(match { it.contains("attendance") }) }
        verify { mockFileWriter.append(match { it.contains("Critical database error") }) }
        verify { mockFileWriter.append(match { it.contains("Stack trace here") }) }
        verify { Log.e("SmartAttendanceError", "Critical error reported and saved to file") }
    }

    @Test
    fun `logError should handle file write failure gracefully`() = runTest {
        // Given
        val error = RuntimeException("Test error")
        val context = ErrorContext(
            userId = "user123",
            screen = "test_screen",
            action = "test_action",
            timestamp = System.currentTimeMillis()
        )
        
        // Mock file writer to throw exception
        every { anyConstructed<FileWriter>().use(any()) } throws RuntimeException("File write failed")

        // When
        errorHandlerService.logError(error, context)

        // Then
        verify { Log.e("SmartAttendanceError", "Failed to write to log file", any<RuntimeException>()) }
    }
}