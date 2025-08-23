package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.UserRole

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val role: UserRole,
    val biometricEnabled: Boolean = false,
    val lastLogin: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)