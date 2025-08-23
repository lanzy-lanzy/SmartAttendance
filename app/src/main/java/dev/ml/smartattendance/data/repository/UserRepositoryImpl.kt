package dev.ml.smartattendance.data.repository

import dev.ml.smartattendance.data.dao.UserDao
import dev.ml.smartattendance.data.entity.User
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    
    override suspend fun getAllActiveUsers(): List<User> = userDao.getAllActiveUsers()
    
    override suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)
    
    override suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    
    override suspend fun getUsersByRole(role: UserRole): List<User> = userDao.getUsersByRole(role)
    
    override suspend fun insertUser(user: User) = userDao.insertUser(user)
    
    override suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    override suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    
    override suspend fun deactivateUser(userId: String) = userDao.deactivateUser(userId)
    
    override suspend fun updateLastLogin(userId: String, timestamp: Long) = userDao.updateLastLogin(userId, timestamp)
    
    override suspend fun updateBiometricEnabled(userId: String, enabled: Boolean) = 
        userDao.updateBiometricEnabled(userId, enabled)
    
    override suspend fun getUserCountByRole(role: UserRole): Int = userDao.getUserCountByRole(role)
}