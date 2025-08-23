package dev.ml.smartattendance.data.service

import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.model.location.Location
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeofenceManagerImplTest {
    
    private lateinit var geofenceManager: GeofenceManagerImpl
    
    // Manila, Philippines coordinates for testing
    private val testLocation = LatLng(14.5995, 120.9842)
    private val testRadius = 50f // 50 meters
    private val eventId = \"EVENT001\"
    
    @Before
    fun setup() {
        geofenceManager = GeofenceManagerImpl()
    }
    
    @Test
    fun `calculateDistance should return correct distance between two points`() {
        // Given - Two points in Manila with known approximate distance
        val point1 = LatLng(14.5995, 120.9842) // Manila City Hall
        val point2 = LatLng(14.6042, 120.9822) // Nearby location (~580 meters)
        
        // When
        val distance = geofenceManager.calculateDistance(point1, point2)
        
        // Then - Distance should be approximately 580 meters (allow 50m tolerance)
        assertTrue(abs(distance - 580) < 50, \"Distance calculation error: expected ~580m, got ${distance}m\")
    }
    
    @Test
    fun `calculateDistance should return zero for same location`() {
        // Given
        val location = LatLng(14.5995, 120.9842)
        
        // When
        val distance = geofenceManager.calculateDistance(location, location)
        
        // Then
        assertTrue(distance < 1, \"Distance should be near zero for same location, got ${distance}m\")
    }
    
    @Test
    fun `isWithinGeofence should return true when location is within radius`() = runTest {
        // Given
        geofenceManager.createGeofence(eventId, testLocation, testRadius)
        
        // Location within 30 meters (inside 50m radius)
        val nearbyLocation = Location(\n            latitude = 14.5998, // Slightly north\n            longitude = 120.9842,\n            accuracy = 5f,\n            timestamp = System.currentTimeMillis()\n        )\n        \n        // When\n        val isWithin = geofenceManager.isWithinGeofence(eventId, nearbyLocation)\n        \n        // Then\n        assertTrue(isWithin, \"Location should be within geofence\")\n    }\n    \n    @Test\n    fun `isWithinGeofence should return false when location is outside radius`() = runTest {\n        // Given\n        geofenceManager.createGeofence(eventId, testLocation, testRadius)\n        \n        // Location about 100 meters away (outside 50m radius)\n        val farLocation = Location(\n            latitude = 14.6005, // Further north\n            longitude = 120.9842,\n            accuracy = 5f,\n            timestamp = System.currentTimeMillis()\n        )\n        \n        // When\n        val isWithin = geofenceManager.isWithinGeofence(eventId, farLocation)\n        \n        // Then\n        assertFalse(isWithin, \"Location should be outside geofence\")\n    }\n    \n    @Test\n    fun `isWithinGeofence should return false for non-existent geofence`() = runTest {\n        // Given - No geofence created\n        val testLocationObj = Location(\n            latitude = testLocation.latitude,\n            longitude = testLocation.longitude,\n            accuracy = 5f,\n            timestamp = System.currentTimeMillis()\n        )\n        \n        // When\n        val isWithin = geofenceManager.isWithinGeofence(\"NON_EXISTENT_EVENT\", testLocationObj)\n        \n        // Then\n        assertFalse(isWithin, \"Non-existent geofence should return false\")\n    }\n    \n    @Test\n    fun `createGeofence and removeGeofence should work correctly`() = runTest {\n        // Given\n        val testLocationObj = Location(\n            latitude = testLocation.latitude,\n            longitude = testLocation.longitude,\n            accuracy = 5f,\n            timestamp = System.currentTimeMillis()\n        )\n        \n        // When - Create geofence\n        geofenceManager.createGeofence(eventId, testLocation, testRadius)\n        val isWithinAfterCreate = geofenceManager.isWithinGeofence(eventId, testLocationObj)\n        \n        // Remove geofence\n        geofenceManager.removeGeofence(eventId)\n        val isWithinAfterRemove = geofenceManager.isWithinGeofence(eventId, testLocationObj)\n        \n        // Then\n        assertTrue(isWithinAfterCreate, \"Should be within geofence after creation\")\n        assertFalse(isWithinAfterRemove, \"Should not be within geofence after removal\")\n    }\n    \n    @Test\n    fun `calculateDistance should handle very far distances correctly`() {\n        // Given - Manila to New York (very far distance)\n        val manila = LatLng(14.5995, 120.9842)\n        val newYork = LatLng(40.7128, -74.0060)\n        \n        // When\n        val distance = geofenceManager.calculateDistance(manila, newYork)\n        \n        // Then - Distance should be approximately 17,000 km (allow 1000km tolerance)\n        val expectedDistance = 17000000f // 17,000 km in meters\n        assertTrue(abs(distance - expectedDistance) < 1000000, \n            \"Very far distance calculation error: expected ~17,000km, got ${distance/1000}km\")\n    }\n    \n    @Test\n    fun `calculateDistance should handle crossing international date line`() {\n        // Given - Points that cross the international date line\n        val fiji = LatLng(-18.1248, 178.4501) // Fiji (positive longitude)\n        val samoa = LatLng(-13.7590, -172.1046) // Samoa (negative longitude)\n        \n        // When\n        val distance = geofenceManager.calculateDistance(fiji, samoa)\n        \n        // Then - Should calculate reasonable distance (approximately 1,000-2,000 km)\n        assertTrue(distance > 500000 && distance < 3000000, \n            \"Date line crossing distance seems incorrect: ${distance/1000}km\")\n    }\n}"