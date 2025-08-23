package dev.ml.smartattendance.domain.model

enum class UserRole {
    STUDENT, ADMIN
}

enum class AttendanceStatus {
    PRESENT, LATE, ABSENT, EXCUSED
}

enum class PenaltyType {
    WARNING, MINOR, MAJOR, CRITICAL
}

enum class BiometricType {
    FINGERPRINT, FACE
}

data class TimeWindow(
    val startOffset: Long, // Minutes before event start
    val endOffset: Long    // Minutes after event start
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)