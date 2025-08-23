package dev.ml.smartattendance.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ml.smartattendance.data.service.AuthServiceImpl
import dev.ml.smartattendance.data.service.FirestoreServiceImpl
import dev.ml.smartattendance.domain.service.AuthService
import dev.ml.smartattendance.domain.service.FirestoreService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthService(
        authService: AuthServiceImpl
    ): AuthService {
        return authService
    }

    @Provides
    @Singleton
    fun provideFirestoreService(
        firestoreService: FirestoreServiceImpl
    ): FirestoreService {
        return firestoreService
    }
}