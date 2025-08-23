package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.model.location.Location

interface GeofenceManager {
    suspend fun createGeofence(eventId: String, location: LatLng, radius: Float)
    suspend fun isWithinGeofence(eventId: String, currentLocation: Location): Boolean
    suspend fun removeGeofence(eventId: String)
    fun calculateDistance(location1: LatLng, location2: LatLng): Float
}