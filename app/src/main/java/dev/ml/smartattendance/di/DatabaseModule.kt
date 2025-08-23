package dev.ml.smartattendance.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ml.smartattendance.data.database.SmartAttendanceDatabase
import dev.ml.smartattendance.data.dao.*
import dev.ml.smartattendance.data.repository.*
import dev.ml.smartattendance.data.service.*
import dev.ml.smartattendance.domain.repository.*
import dev.ml.smartattendance.domain.service.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    
    companion object {
        @Provides
        @Singleton
        fun provideContext(@ApplicationContext context: Context): Context = context
        
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): SmartAttendanceDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmartAttendanceDatabase::class.java,
                "smart_attendance_database"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
        
        @Provides
        fun provideUserDao(database: SmartAttendanceDatabase): UserDao = database.userDao()
        
        @Provides
        fun provideStudentDao(database: SmartAttendanceDatabase): StudentDao = database.studentDao()
        
        @Provides
        fun provideEventDao(database: SmartAttendanceDatabase): EventDao = database.eventDao()
        
        @Provides
        fun provideAttendanceRecordDao(database: SmartAttendanceDatabase): AttendanceRecordDao = database.attendanceRecordDao()
        
        @Provides
        fun provideBiometricTemplateDao(database: SmartAttendanceDatabase): BiometricTemplateDao = database.biometricTemplateDao()
    }
    
    @Binds
    abstract fun bindStudentRepository(studentRepositoryImpl: StudentRepositoryImpl): StudentRepository
    
    @Binds
    abstract fun bindEventRepository(eventRepositoryImpl: EventRepositoryImpl): EventRepository
    
    @Binds
    abstract fun bindAttendanceRepository(attendanceRepositoryImpl: AttendanceRepositoryImpl): AttendanceRepository
    
    @Binds
    abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository
    
    @Binds
    abstract fun bindBiometricAuthenticator(biometricAuthenticatorImpl: BiometricAuthenticatorImpl): BiometricAuthenticator
    
    @Binds
    abstract fun bindFaceDetectionService(faceDetectionServiceImpl: FaceDetectionServiceImpl): FaceDetectionService
    
    @Binds
    abstract fun bindGeofenceManager(geofenceManagerImpl: GeofenceManagerImpl): GeofenceManager
    
    @Binds
    abstract fun bindLocationProvider(locationProviderImpl: LocationProviderImpl): LocationProvider
    
    @Binds
    abstract fun bindEncryptionService(encryptionServiceImpl: EncryptionServiceImpl): EncryptionService
    
    @Binds
    abstract fun bindSecurityManager(securityManagerImpl: SecurityManagerImpl): SecurityManager
    
    @Binds
    abstract fun bindPenaltyCalculationService(penaltyCalculationServiceImpl: PenaltyCalculationServiceImpl): PenaltyCalculationService
    
    @Binds
    abstract fun bindReportGenerationService(reportGenerationServiceImpl: ReportGenerationServiceImpl): ReportGenerationService
    
    @Binds
    abstract fun bindErrorHandlerService(errorHandlerServiceImpl: ErrorHandlerServiceImpl): ErrorHandlerService
}