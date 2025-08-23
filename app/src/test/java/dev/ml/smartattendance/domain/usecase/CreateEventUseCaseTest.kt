package dev.ml.smartattendance.domain.usecase

import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.repository.EventRepository
import dev.ml.smartattendance.domain.service.GeofenceManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class CreateEventUseCaseTest {

    private lateinit var createEventUseCase: CreateEventUseCase
    private val mockEventRepository = mockk<EventRepository>()
    private val mockGeofenceManager = mockk<GeofenceManager>()

    private val currentTime = System.currentTimeMillis()
    private val futureStartTime = currentTime + 86400000L // 1 day from now
    private val futureEndTime = futureStartTime + 3600000L // 1 hour after start
    private val testLocation = LatLng(40.7128, -74.0060) // New York coordinates

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock UUID generation for consistent testing
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "test-event-id"
        
        createEventUseCase = CreateEventUseCase(mockEventRepository, mockGeofenceManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute should return Success when event is created successfully`() = runTest {
        // Given
        val eventName = "Morning Assembly"
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.id == "test-event-id" &&
                event.name == eventName &&
                event.startTime == futureStartTime &&
                event.endTime == futureEndTime &&
                event.latitude == testLocation.latitude &&
                event.longitude == testLocation.longitude &&
                event.geofenceRadius == 50f &&
                event.isActive == true
            })
        }
        
        coVerify { 
            mockGeofenceManager.createGeofence("test-event-id", testLocation, 50f)
        }
    }

    @Test
    fun `execute should return Error when event name is blank`() = runTest {
        // Given
        val eventName = ""

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("Event name cannot be empty", (result as CreateEventUseCase.EventCreationResult.Error).message)
        
        coVerify(exactly = 0) { mockEventRepository.insertEvent(any()) }
        coVerify(exactly = 0) { mockGeofenceManager.createGeofence(any(), any(), any()) }
    }

    @Test
    fun `execute should return Error when event name is whitespace only`() = runTest {
        // Given
        val eventName = "   "

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("Event name cannot be empty", (result as CreateEventUseCase.EventCreationResult.Error).message)
    }

    @Test
    fun `execute should return Error when start time is not before end time`() = runTest {
        // Given
        val eventName = "Test Event"
        val startTime = futureStartTime
        val endTime = futureStartTime // Same time

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = startTime,
            endTime = endTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("End time must be after start time", (result as CreateEventUseCase.EventCreationResult.Error).message)
        
        coVerify(exactly = 0) { mockEventRepository.insertEvent(any()) }
        coVerify(exactly = 0) { mockGeofenceManager.createGeofence(any(), any(), any()) }
    }

    @Test
    fun `execute should return Error when start time is after end time`() = runTest {
        // Given
        val eventName = "Test Event"
        val startTime = futureEndTime
        val endTime = futureStartTime // Start is after end

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = startTime,
            endTime = endTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("End time must be after start time", (result as CreateEventUseCase.EventCreationResult.Error).message)
    }

    @Test
    fun `execute should return Error when start time is in the past`() = runTest {
        // Given
        val eventName = "Test Event"
        val pastStartTime = currentTime - 3600000L // 1 hour ago
        val pastEndTime = currentTime - 1800000L   // 30 minutes ago

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = pastStartTime,
            endTime = pastEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("Event start time must be in the future", (result as CreateEventUseCase.EventCreationResult.Error).message)
        
        coVerify(exactly = 0) { mockEventRepository.insertEvent(any()) }
        coVerify(exactly = 0) { mockGeofenceManager.createGeofence(any(), any(), any()) }
    }

    @Test
    fun `execute should return Error when geofence radius is zero`() = runTest {
        // Given
        val eventName = "Test Event"

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation,
            geofenceRadius = 0f
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("Geofence radius must be positive", (result as CreateEventUseCase.EventCreationResult.Error).message)
    }

    @Test
    fun `execute should return Error when geofence radius is negative`() = runTest {
        // Given
        val eventName = "Test Event"

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation,
            geofenceRadius = -10f
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertEquals("Geofence radius must be positive", (result as CreateEventUseCase.EventCreationResult.Error).message)
    }

    @Test
    fun `execute should use custom geofence radius when provided`() = runTest {
        // Given
        val eventName = "Test Event"
        val customRadius = 100f
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation,
            geofenceRadius = customRadius
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.geofenceRadius == customRadius
            })
        }
        
        coVerify { 
            mockGeofenceManager.createGeofence("test-event-id", testLocation, customRadius)
        }
    }

    @Test
    fun `execute should use custom sign-in and sign-out offsets`() = runTest {
        // Given
        val eventName = "Test Event"
        val customSignInStartOffset = 60L // 60 minutes
        val customSignInEndOffset = 30L   // 30 minutes
        val customSignOutStartOffset = 45L // 45 minutes
        val customSignOutEndOffset = 20L   // 20 minutes
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation,
            signInStartOffset = customSignInStartOffset,
            signInEndOffset = customSignInEndOffset,
            signOutStartOffset = customSignOutStartOffset,
            signOutEndOffset = customSignOutEndOffset
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.signInStartOffset == customSignInStartOffset &&
                event.signInEndOffset == customSignInEndOffset &&
                event.signOutStartOffset == customSignOutStartOffset &&
                event.signOutEndOffset == customSignOutEndOffset
            })
        }
    }

    @Test
    fun `execute should trim whitespace from event name`() = runTest {
        // Given
        val eventName = "  Test Event  "
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.name == "Test Event"
            })
        }
    }

    @Test
    fun `execute should return Error when repository throws exception`() = runTest {
        // Given
        val eventName = "Test Event"
        
        coEvery { mockEventRepository.insertEvent(any()) } throws RuntimeException("Database error")

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertTrue((result as CreateEventUseCase.EventCreationResult.Error).message.contains("Failed to create event"))
        assertTrue(result.message.contains("Database error"))
        
        coVerify { mockEventRepository.insertEvent(any()) }
        coVerify(exactly = 0) { mockGeofenceManager.createGeofence(any(), any(), any()) }
    }

    @Test
    fun `execute should return Error when geofence manager throws exception`() = runTest {
        // Given
        val eventName = "Test Event"
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } throws RuntimeException("Geofence error")

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Error)
        assertTrue((result as CreateEventUseCase.EventCreationResult.Error).message.contains("Failed to create event"))
        assertTrue(result.message.contains("Geofence error"))
        
        coVerify { mockEventRepository.insertEvent(any()) }
        coVerify { mockGeofenceManager.createGeofence(any(), any(), any()) }
    }

    @Test
    fun `execute should handle special characters in event name`() = runTest {
        // Given
        val eventName = "Café & Restaurant Event - 2023/24"
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.name == eventName
            })
        }
    }

    @Test
    fun `execute should handle extreme coordinates`() = runTest {
        // Given
        val eventName = "Test Event"
        val extremeLocation = LatLng(-90.0, -180.0) // South Pole, date line
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = extremeLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.latitude == extremeLocation.latitude &&
                event.longitude == extremeLocation.longitude
            })
        }
        
        coVerify { 
            mockGeofenceManager.createGeofence("test-event-id", extremeLocation, 50f)
        }
    }

    @Test
    fun `execute should set event as active by default`() = runTest {
        // Given
        val eventName = "Test Event"
        
        coEvery { mockEventRepository.insertEvent(any()) } just Runs
        coEvery { mockGeofenceManager.createGeofence(any(), any(), any()) } just Runs

        // When
        val result = createEventUseCase.execute(
            name = eventName,
            startTime = futureStartTime,
            endTime = futureEndTime,
            location = testLocation
        )

        // Then
        assertTrue(result is CreateEventUseCase.EventCreationResult.Success)
        
        coVerify { 
            mockEventRepository.insertEvent(match { event ->
                event.isActive == true
            })
        }
    }
}