package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.UserRole

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String,
    val name: String,
    val course: String,
    val enrollmentDate: Long,
    val role: UserRole = UserRole.STUDENT,
    val isActive: Boolean = true
)