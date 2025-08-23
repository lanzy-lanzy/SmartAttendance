package dev.ml.smartattendance.data.dao

import androidx.room.*
import dev.ml.smartattendance.data.entity.User
import dev.ml.smartattendance.domain.model.UserRole

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE isActive = 1")
    suspend fun getAllActiveUsers(): List<User>
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1")
    suspend fun getUserByEmail(email: String): User?
    
    @Query("SELECT * FROM users WHERE role = :role AND isActive = 1")
    suspend fun getUsersByRole(role: UserRole): List<User>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun deactivateUser(userId: String)
    
    @Query("UPDATE users SET lastLogin = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)
    
    @Query("UPDATE users SET biometricEnabled = :enabled WHERE id = :userId")
    suspend fun updateBiometricEnabled(userId: String, enabled: Boolean)
    
    @Query("SELECT COUNT(*) FROM users WHERE role = :role AND isActive = 1")
    suspend fun getUserCountByRole(role: UserRole): Int
}