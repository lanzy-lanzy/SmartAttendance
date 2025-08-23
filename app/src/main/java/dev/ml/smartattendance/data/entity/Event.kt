package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.LatLng
import dev.ml.smartattendance.domain.model.TimeWindow

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadius: Float,
    val signInStartOffset: Long,  // Minutes before event start
    val signInEndOffset: Long,    // Minutes after event start
    val signOutStartOffset: Long, // Minutes before event end
    val signOutEndOffset: Long,   // Minutes after event end
    val isActive: Boolean = true
) {
    val location: LatLng
        get() = LatLng(latitude, longitude)
    
    val signInWindow: TimeWindow
        get() = TimeWindow(signInStartOffset, signInEndOffset)
    
    val signOutWindow: TimeWindow
        get() = TimeWindow(signOutStartOffset, signOutEndOffset)
}