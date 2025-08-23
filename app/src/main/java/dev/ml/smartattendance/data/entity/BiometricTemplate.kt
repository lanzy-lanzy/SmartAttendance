package dev.ml.smartattendance.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.ml.smartattendance.domain.model.BiometricType

@Entity(
    tableName = "biometric_templates",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BiometricTemplate(
    @PrimaryKey val id: String,
    val studentId: String,
    val type: BiometricType,
    val encryptedTemplate: String,
    val createdAt: Long,
    val isActive: Boolean = true
)