package dev.ml.smartattendance.data.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.domain.service.FirestoreService

/**
 * Utility class to provide direct access to Firebase services
 * Use this only when Hilt dependency injection is not available
 */
object FirebaseModule {
    
    /**
     * Provides a direct instance of FirestoreService
     * Only use this when Hilt injection is not available
     */
    fun provideFirestoreService(): FirestoreService {
        return FirestoreServiceImpl(FirebaseFirestore.getInstance())
    }
    
    /**
     * Provides a direct instance of FirebaseAuth
     * Only use this when Hilt injection is not available
     */
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    /**
     * Provides a direct instance of AuthService
     * Only use this when Hilt injection is not available
     */
    fun provideAuthService(): AuthService {
        return AuthServiceImpl(provideFirebaseAuth(), provideFirestoreService())
    }
}