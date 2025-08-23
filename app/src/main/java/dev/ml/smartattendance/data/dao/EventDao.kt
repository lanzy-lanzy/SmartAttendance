package dev.ml.smartattendance.data.dao

import androidx.room.*
import dev.ml.smartattendance.data.entity.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    
    @Query("SELECT * FROM events WHERE isActive = 1 ORDER BY startTime ASC")
    fun getAllActiveEvents(): Flow<List<Event>>
    
    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<Event>>
    
    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): Event?
    
    @Query("SELECT * FROM events WHERE startTime <= :currentTime AND endTime >= :currentTime AND isActive = 1")
    suspend fun getCurrentEvents(currentTime: Long): List<Event>
    
    @Query("SELECT * FROM events WHERE startTime > :currentTime AND isActive = 1 ORDER BY startTime ASC")
    suspend fun getUpcomingEvents(currentTime: Long): List<Event>
    
    @Query("SELECT * FROM events WHERE endTime < :currentTime AND isActive = 1 ORDER BY startTime DESC")
    suspend fun getPastEvents(currentTime: Long): List<Event>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)
    
    @Update
    suspend fun updateEvent(event: Event)
    
    @Delete
    suspend fun deleteEvent(event: Event)
    
    @Query("UPDATE events SET isActive = 0 WHERE id = :eventId")
    suspend fun deactivateEvent(eventId: String)
    
    @Query("SELECT COUNT(*) FROM events WHERE isActive = 1")
    suspend fun getActiveEventCount(): Int
}