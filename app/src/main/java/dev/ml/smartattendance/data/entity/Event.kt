package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.model.TimeWindow

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geofenceRadius: Float = 50f,
    val signInStartOffset: Long = 30L,  // Minutes before event start
    val signInEndOffset: Long = 15L,    // Minutes after event start
    val signOutStartOffset: Long = 30L, // Minutes before event end
    val signOutEndOffset: Long = 15L,   // Minutes after event end
    val isActive: Boolean = true
) {
    val location: LatLng
        get() = LatLng(latitude, longitude)
    
    val signInWindow: TimeWindow
        get() = TimeWindow(signInStartOffset, signInEndOffset)
    
    val signOutWindow: TimeWindow
        get() = TimeWindow(signOutStartOffset, signOutEndOffset)
}