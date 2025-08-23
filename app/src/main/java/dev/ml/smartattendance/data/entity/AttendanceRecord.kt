package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceRecord(
    @PrimaryKey val id: String,
    val studentId: String,
    val eventId: String,
    val timestamp: Long,
    val status: AttendanceStatus,
    val penalty: PenaltyType?,
    val latitude: Double?,
    val longitude: Double?,
    val synced: Boolean = false,
    val notes: String? = null
)