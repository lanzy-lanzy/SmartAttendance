package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.AttendanceRecord
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.model.biometric.AuthResult
import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.repository.AttendanceRepository
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.BiometricAuthenticator
import dev.ml.smartattendance.domain.service.GeofenceManager
import dev.ml.smartattendance.domain.service.LocationProvider
import dev.ml.smartattendance.domain.service.PenaltyCalculationService
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkAttendanceUseCaseTest {
    
    @Mock
    private lateinit var attendanceRepository: AttendanceRepository
    
    @Mock
    private lateinit var eventRepository: EventRepository
    
    @Mock
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    
    @Mock
    private lateinit var locationProvider: LocationProvider
    
    @Mock
    private lateinit var geofenceManager: GeofenceManager
    
    @Mock
    private lateinit var penaltyCalculationService: PenaltyCalculationService
    
    private lateinit var markAttendanceUseCase: MarkAttendanceUseCase
    
    private val studentId = "STUDENT001"
    private val eventId = "EVENT001"
    private val currentTime = System.currentTimeMillis()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        markAttendanceUseCase = MarkAttendanceUseCase(
            attendanceRepository,
            eventRepository,
            biometricAuthenticator,
            locationProvider,
            geofenceManager,
            penaltyCalculationService
        )
    }
    
    @Test
    fun `execute should return error when event not found`() = runTest {
        // Given
        whenever(eventRepository.getEventById(eventId)).thenReturn(null)
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Error)
        assertEquals("Event not found or has been removed.", (result as MarkAttendanceUseCase.AttendanceResult.Error).message)
    }
    
    @Test
    fun `execute should return error when attendance already marked`() = runTest {
        // Given
        val event = createMockEvent()
        val existingRecord = createMockAttendanceRecord()
        
        whenever(eventRepository.getEventById(eventId)).thenReturn(event)
        whenever(attendanceRepository.getAttendanceRecord(studentId, eventId)).thenReturn(existingRecord)
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Error)
        assertEquals("Attendance already marked for this event", (result as MarkAttendanceUseCase.AttendanceResult.Error).message)
    }
    
    @Test
    fun `execute should return error when location unavailable`() = runTest {
        // Given
        val event = createMockEvent()
        
        whenever(eventRepository.getEventById(eventId)).thenReturn(event)
        whenever(attendanceRepository.getAttendanceRecord(studentId, eventId)).thenReturn(null)
        whenever(locationProvider.getCurrentLocation()).thenReturn(null)
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Error)
        assertEquals("Unable to get current location", (result as MarkAttendanceUseCase.AttendanceResult.Error).message)
    }
    
    @Test
    fun `execute should return error when outside geofence`() = runTest {
        // Given
        val event = createMockEvent()
        val location = createMockLocation()
        
        whenever(eventRepository.getEventById(eventId)).thenReturn(event)
        whenever(attendanceRepository.getAttendanceRecord(studentId, eventId)).thenReturn(null)
        whenever(locationProvider.getCurrentLocation()).thenReturn(location)
        whenever(geofenceManager.isWithinGeofence(eventId, location)).thenReturn(false)
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Error)
        assertEquals("You are not within the event location", (result as MarkAttendanceUseCase.AttendanceResult.Error).message)
    }
    
    @Test
    fun `execute should return error when biometric authentication fails`() = runTest {
        // Given
        val event = createMockEvent()
        val location = createMockLocation()
        
        whenever(eventRepository.getEventById(eventId)).thenReturn(event)
        whenever(attendanceRepository.getAttendanceRecord(studentId, eventId)).thenReturn(null)
        whenever(locationProvider.getCurrentLocation()).thenReturn(location)
        whenever(geofenceManager.isWithinGeofence(eventId, location)).thenReturn(true)
        whenever(biometricAuthenticator.authenticate()).thenReturn(AuthResult.Error(dev.ml.smartattendance.domain.model.biometric.BiometricError.AuthenticationFailed))
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Error)
        assertEquals("Biometric authentication failed", (result as MarkAttendanceUseCase.AttendanceResult.Error).message)
    }
    
    @Test
    fun `execute should successfully mark attendance when all conditions met`() = runTest {
        // Given
        val event = createMockEvent()
        val location = createMockLocation()
        
        whenever(eventRepository.getEventById(eventId)).thenReturn(event)
        whenever(attendanceRepository.getAttendanceRecord(studentId, eventId)).thenReturn(null)
        whenever(locationProvider.getCurrentLocation()).thenReturn(location)
        whenever(geofenceManager.isWithinGeofence(eventId, location)).thenReturn(true)
        whenever(biometricAuthenticator.authenticate()).thenReturn(AuthResult.Success)
        whenever(penaltyCalculationService.calculatePenalty(any())).thenReturn(null)
        
        // When
        val result = markAttendanceUseCase.execute(studentId, eventId)
        
        // Then
        assertTrue(result is MarkAttendanceUseCase.AttendanceResult.Success)
        verify(attendanceRepository).insertAttendanceRecord(any())
    }
    
    private fun createMockEvent(): Event {
        return Event(
            id = eventId,
            name = "Test Event",
            startTime = currentTime,
            endTime = currentTime + 3600000, // 1 hour later
            latitude = 14.5995,
            longitude = 120.9842,
            geofenceRadius = 50f,
            signInWindow = dev.ml.smartattendance.data.entity.SignInWindow(30, 15),
            signOutWindow = dev.ml.smartattendance.data.entity.SignOutWindow(30, 15),
            isActive = true,
            course = "CS101"
        )
    }
    
    private fun createMockLocation(): Location {
        return Location(
            latitude = 14.5995,
            longitude = 120.9842,
            accuracy = 10f,
            timestamp = currentTime
        )
    }
    
    private fun createMockAttendanceRecord(): AttendanceRecord {
        return AttendanceRecord(
            id = "RECORD001",
            studentId = studentId,
            eventId = eventId,
            timestamp = currentTime,
            status = AttendanceStatus.PRESENT,
            penalty = null,
            latitude = 14.5995,
            longitude = 120.9842
        )
    }
}