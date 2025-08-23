package dev.ml.smartattendance.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import dev.ml.smartattendance.domain.model.location.Location
import dev.ml.smartattendance.domain.model.location.LocationCallback
import dev.ml.smartattendance.domain.model.location.LocationError
import dev.ml.smartattendance.domain.service.LocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationProviderImpl @Inject constructor(
    private val context: Context
) : LocationProvider {
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null
    private var appLocationCallback: LocationCallback? = null
    
    override suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            // For demo purposes, return a default location if no permissions
            // In production, this should prompt for permissions
            val demoLocation = Location(
                latitude = 37.7749, // San Francisco coordinates
                longitude = -122.4194,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis()
            )
            continuation.resume(demoLocation)
            return@suspendCancellableCoroutine
        }
        
        if (!isLocationEnabled()) {
            // Return demo location if location services are disabled
            val demoLocation = Location(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis()
            )
            continuation.resume(demoLocation)
            return@suspendCancellableCoroutine
        }
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { androidLocation ->
                    if (androidLocation != null) {
                        val location = Location(
                            latitude = androidLocation.latitude,
                            longitude = androidLocation.longitude,
                            accuracy = androidLocation.accuracy,
                            timestamp = androidLocation.time
                        )
                        continuation.resume(location)
                    } else {
                        // Request fresh location
                        requestFreshLocation { freshLocation ->
                            if (freshLocation != null) {
                                continuation.resume(freshLocation)
                            } else {
                                // Fallback to demo location
                                val demoLocation = Location(
                                    latitude = 37.7749,
                                    longitude = -122.4194,
                                    accuracy = 10.0f,
                                    timestamp = System.currentTimeMillis()
                                )
                                continuation.resume(demoLocation)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Fallback to demo location on failure
                    val demoLocation = Location(
                        latitude = 37.7749,
                        longitude = -122.4194,
                        accuracy = 10.0f,
                        timestamp = System.currentTimeMillis()
                    )
                    continuation.resume(demoLocation)
                }
        } catch (e: SecurityException) {
            // Fallback to demo location on security exception
            val demoLocation = Location(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 10.0f,
                timestamp = System.currentTimeMillis()
            )
            continuation.resume(demoLocation)
        }
    }
    
    private fun requestFreshLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMaxUpdates(1)
            .build()
        
        val freshLocationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val androidLocation = locationResult.lastLocation
                if (androidLocation != null) {
                    val location = Location(
                        latitude = androidLocation.latitude,
                        longitude = androidLocation.longitude,
                        accuracy = androidLocation.accuracy,
                        timestamp = androidLocation.time
                    )
                    callback(location)
                } else {
                    callback(null)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                freshLocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback(null)
        }
    }
    
    override fun startLocationUpdates(callback: LocationCallback) {
        if (!hasLocationPermission()) {
            callback.onLocationError(LocationError.PermissionDenied)
            return
        }
        
        if (!isLocationEnabled()) {
            callback.onLocationError(LocationError.LocationDisabled)
            return
        }
        
        appLocationCallback = callback
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        
        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { androidLocation ->
                    val location = Location(
                        latitude = androidLocation.latitude,
                        longitude = androidLocation.longitude,
                        accuracy = androidLocation.accuracy,
                        timestamp = androidLocation.time
                    )
                    callback.onLocationUpdated(location)
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                if (!availability.isLocationAvailable) {
                    callback.onLocationError(LocationError.LocationUnavailable)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback.onLocationError(LocationError.PermissionDenied)
        }
    }
    
    override fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
            appLocationCallback = null
        }
    }
    
    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}