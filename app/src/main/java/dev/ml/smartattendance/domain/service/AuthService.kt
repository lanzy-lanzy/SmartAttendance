package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.auth.AuthResult
import dev.ml.smartattendance.domain.model.auth.RegisterRequest
import dev.ml.smartattendance.domain.model.auth.User
import kotlinx.coroutines.flow.Flow

interface AuthService {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(request: RegisterRequest): AuthResult
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    suspend fun sendPasswordReset(email: String): Boolean
    suspend fun updateUserProfile(user: User): Boolean
    suspend fun deleteUser(uid: String): Boolean
    fun getCurrentUserFlow(): Flow<User?>
    fun isUserSignedIn(): Boolean
    suspend fun refreshUser(): User?
}