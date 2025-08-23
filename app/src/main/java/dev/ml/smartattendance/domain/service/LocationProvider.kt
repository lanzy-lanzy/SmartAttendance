package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.model.location.LocationCallback

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
    fun startLocationUpdates(callback: LocationCallback)
    fun stopLocationUpdates()
    fun isLocationEnabled(): Boolean
}