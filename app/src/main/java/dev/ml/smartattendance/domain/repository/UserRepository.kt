package dev.ml.smartattendance.domain.repository

import dev.ml.smartattendance.data.entity.User
import dev.ml.smartattendance.domain.model.UserRole

interface UserRepository {
    suspend fun getAllActiveUsers(): List<User>
    suspend fun getUserById(userId: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUsersByRole(role: UserRole): List<User>
    suspend fun insertUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
    suspend fun deactivateUser(userId: String)
    suspend fun updateLastLogin(userId: String, timestamp: Long)
    suspend fun updateBiometricEnabled(userId: String, enabled: Boolean)
    suspend fun getUserCountByRole(role: UserRole): Int
}