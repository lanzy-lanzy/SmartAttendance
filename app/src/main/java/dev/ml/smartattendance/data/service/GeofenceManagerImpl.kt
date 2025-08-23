package dev.ml.smartattendance.data.service

import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.service.GeofenceManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class GeofenceManagerImpl @Inject constructor() : GeofenceManager {
    
    private val geofences = mutableMapOf<String, GeofenceData>()
    private val mutex = Mutex()
    
    data class GeofenceData(
        val location: LatLng,
        val radius: Float
    )
    
    override suspend fun createGeofence(eventId: String, location: LatLng, radius: Float) {
        mutex.withLock {
            geofences[eventId] = GeofenceData(location, radius)
        }
    }
    
    override suspend fun isWithinGeofence(eventId: String, currentLocation: Location): Boolean {
        return mutex.withLock {
            val geofence = geofences[eventId]
            if (geofence == null) {
                // If no geofence exists, allow access for testing purposes
                return true
            }
            
            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            val distance = calculateDistance(geofence.location, currentLatLng)
            
            // For demo/testing purposes, be more lenient with geofence validation
            val isWithin = distance <= geofence.radius
            
            // Debug logging (in production this would use a proper logging framework)
            println("GeofenceManager Debug:")
            println("Event ID: $eventId")
            println("Current Location: ${currentLatLng.latitude}, ${currentLatLng.longitude}")
            println("Geofence Center: ${geofence.location.latitude}, ${geofence.location.longitude}")
            println("Distance: ${distance}m, Radius: ${geofence.radius}m")
            println("Within Geofence: $isWithin")
            
            return isWithin
        }
    }
    
    override suspend fun removeGeofence(eventId: String) {
        mutex.withLock {
            geofences.remove(eventId)
        }
    }
    
    override fun calculateDistance(location1: LatLng, location2: LatLng): Float {
        val earthRadius = 6371000f // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(location1.latitude)
        val lat2Rad = Math.toRadians(location2.latitude)
        val deltaLatRad = Math.toRadians(location2.latitude - location1.latitude)
        val deltaLngRad = Math.toRadians(location2.longitude - location1.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return (earthRadius * c).toFloat()
    }
}