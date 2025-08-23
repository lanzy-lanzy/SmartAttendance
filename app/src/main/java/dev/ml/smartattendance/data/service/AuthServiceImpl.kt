package dev.ml.smartattendance.data.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import dev.ml.smartattendance.domain.model.auth.AuthResult
import dev.ml.smartattendance.domain.model.auth.RegisterRequest
import dev.ml.smartattendance.domain.model.auth.User
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.domain.service.FirestoreService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestoreService: FirestoreService
) : AuthService {

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val user = firestoreService.getUser(firebaseUser.uid)
                if (user != null) {
                    // Update last login time
                    val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
                    firestoreService.updateUser(updatedUser)
                    
                    AuthResult(user = updatedUser, isSuccess = true)
                } else {
                    AuthResult(isSuccess = false, errorMessage = "User profile not found")
                }
            } else {
                AuthResult(isSuccess = false, errorMessage = "Authentication failed")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Sign in failed")
        }
    }

    override suspend fun signUp(request: RegisterRequest): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(request.email, request.password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Update Firebase user profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(request.name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Send email verification
                firebaseUser.sendEmailVerification().await()
                
                // Create user profile in Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    email = request.email,
                    name = request.name,
                    role = request.role,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    studentId = request.studentId,
                    course = request.course,
                    adminLevel = request.adminLevel,
                    enrollmentDate = if (request.role.name == "STUDENT") System.currentTimeMillis() else null
                )
                
                val created = firestoreService.createUser(user)
                if (created) {
                    AuthResult(user = user, isSuccess = true)
                } else {
                    // Cleanup Firebase user if Firestore creation fails
                    firebaseUser.delete().await()
                    AuthResult(isSuccess = false, errorMessage = "Failed to create user profile")
                }
            } else {
                AuthResult(isSuccess = false, errorMessage = "Failed to create user account")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Sign up failed")
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            firestoreService.getUser(firebaseUser.uid)
        } else {
            null
        }
    }

    override suspend fun sendPasswordReset(email: String): Boolean {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateUserProfile(user: User): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Update Firebase profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(user.name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Update Firestore profile
                firestoreService.updateUser(user)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteUser(uid: String): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null && firebaseUser.uid == uid) {
                firebaseUser.delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun getCurrentUserFlow(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                // Get user from Firestore
                // Note: This would need to be converted to a suspend function call
                // For now, we'll emit a basic user object
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun refreshUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            firebaseUser.reload().await()
            getCurrentUser()
        } else {
            null
        }
    }
}