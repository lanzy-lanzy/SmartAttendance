package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.EventDao
import dev.ml.smartattendance.data.entity.Event
import dev.ml.smartattendance.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {
    
    override fun getAllActiveEvents(): Flow<List<Event>> = eventDao.getAllActiveEvents()
    
    override fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()
    
    override suspend fun getEventById(eventId: String): Event? = eventDao.getEventById(eventId)
    
    override suspend fun getCurrentEvents(currentTime: Long): List<Event> = eventDao.getCurrentEvents(currentTime)
    
    override suspend fun getUpcomingEvents(currentTime: Long): List<Event> = eventDao.getUpcomingEvents(currentTime)
    
    override suspend fun getPastEvents(currentTime: Long): List<Event> = eventDao.getPastEvents(currentTime)
    
    override suspend fun insertEvent(event: Event) = eventDao.insertEvent(event)
    
    override suspend fun insertEvents(events: List<Event>) = eventDao.insertEvents(events)
    
    override suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
    
    override suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)
    
    override suspend fun deactivateEvent(eventId: String) = eventDao.deactivateEvent(eventId)
    
    override suspend fun getActiveEventCount(): Int = eventDao.getActiveEventCount()
}