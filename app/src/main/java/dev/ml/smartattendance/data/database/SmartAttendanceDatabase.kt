package dev.ml.smartattendance.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import dev.ml.smartattendance.data.dao.*
import dev.ml.smartattendance.data.entity.*

@Database(
    entities = [
        User::class,
        Student::class,
        Event::class,
        AttendanceRecord::class,
        BiometricTemplate::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartAttendanceDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun eventDao(): EventDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun biometricTemplateDao(): BiometricTemplateDao
    
    companion object {
        @Volatile
        private var INSTANCE: SmartAttendanceDatabase? = null
        
        fun getDatabase(context: Context): SmartAttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartAttendanceDatabase::class.java,
                    "smart_attendance_database"
                )
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Enable foreign key constraints
                        db.execSQL("PRAGMA foreign_keys=ON")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}