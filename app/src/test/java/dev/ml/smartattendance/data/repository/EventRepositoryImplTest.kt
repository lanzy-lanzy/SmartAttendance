package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.EventDao
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventRepositoryImplTest {
    
    @Mock
    private lateinit var eventDao: EventDao
    
    @Mock
    private lateinit var firestoreService: FirestoreService
    
    private lateinit var eventRepository: EventRepositoryImpl
    
    private val eventId = "EVENT001"
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        eventRepository = EventRepositoryImpl(eventDao, firestoreService)
    }
    
    @Test
    fun `getEventById should return event from local cache when available`() = runTest {
        // Given
        val localEvent = createMockEvent()
        whenever(eventDao.getEventById(eventId)).thenReturn(localEvent)
        
        // When
        val result = eventRepository.getEventById(eventId)
        
        // Then
        assertEquals(localEvent, result)
        verify(eventDao).getEventById(eventId)
        verify(firestoreService, never()).getEvent(eventId)
    }
    
    @Test
    fun `getEventById should return event from Firebase when not in local cache`() = runTest {
        // Given
        val firebaseEvent = createMockEvent()
        whenever(eventDao.getEventById(eventId)).thenReturn(null)
        whenever(firestoreService.getEvent(eventId)).thenReturn(firebaseEvent)
        
        // When
        val result = eventRepository.getEventById(eventId)
        
        // Then
        assertEquals(firebaseEvent, result)
        verify(eventDao).getEventById(eventId)
        verify(firestoreService).getEvent(eventId)
        verify(eventDao).insertEvent(firebaseEvent)
    }
    
    @Test
    fun `getEventById should return null when event not found anywhere`() = runTest {
        // Given
        whenever(eventDao.getEventById(eventId)).thenReturn(null)
        whenever(firestoreService.getEvent(eventId)).thenReturn(null)
        
        // When
        val result = eventRepository.getEventById(eventId)
        
        // Then
        assertNull(result)
        verify(eventDao).getEventById(eventId)
        verify(firestoreService).getEvent(eventId)
        verify(eventDao, never()).insertEvent(any())
    }
    
    private fun createMockEvent(): Event {
        return Event(
            id = eventId,
            name = "Test Event",
            description = "Test Description",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000,
            latitude = 14.5995,
            longitude = 120.9842,
            geofenceRadius = 50f,
            signInWindow = dev.ml.smartattendance.data.entity.SignInWindow(30, 15),
            signOutWindow = dev.ml.smartattendance.data.entity.SignOutWindow(30, 15),
            isActive = true,
            course = "CS101"
        )
    }
    
    // Helper function for Mockito any() matcher
    private inline fun <reified T> any(): T = org.mockito.kotlin.any()
}