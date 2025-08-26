package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.UserRole

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String = "", // Default values for Firestore deserialization
    val name: String = "",
    val course: String = "",
    val enrollmentDate: Long = 0,
    val role: UserRole = UserRole.STUDENT,
    val isActive: Boolean = true,
    val email: String = "" // Add email field for better user tracking
)