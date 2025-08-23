package dev.ml.smartattendance.data.database

import androidx.room.TypeConverter
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.BiometricType
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.domain.model.UserRole

class Converters {
    
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name
    
    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.valueOf(role)
    
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus): String = status.name
    
    @TypeConverter
    fun toAttendanceStatus(status: String): AttendanceStatus = AttendanceStatus.valueOf(status)
    
    @TypeConverter
    fun fromPenaltyType(penalty: PenaltyType?): String? = penalty?.name
    
    @TypeConverter
    fun toPenaltyType(penalty: String?): PenaltyType? = penalty?.let { PenaltyType.valueOf(it) }
    
    @TypeConverter
    fun fromBiometricType(type: BiometricType): String = type.name
    
    @TypeConverter
    fun toBiometricType(type: String): BiometricType = BiometricType.valueOf(type)
}