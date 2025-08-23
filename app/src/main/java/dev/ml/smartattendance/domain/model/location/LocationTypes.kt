package dev.ml.smartattendance.domain.model.location

sealed class LocationError : Exception() {
    object PermissionDenied : LocationError()
    object LocationDisabled : LocationError()
    object OutsideGeofence : LocationError()
    object LocationUnavailable : LocationError()
    data class Unknown(override val message: String) : LocationError()
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

interface LocationCallback {
    fun onLocationUpdated(location: Location)
    fun onLocationError(error: LocationError)
}