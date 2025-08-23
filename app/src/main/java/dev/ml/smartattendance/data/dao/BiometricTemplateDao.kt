package dev.ml.smartattendance.data.dao

import androidx.room.*
import dev.ml.smartattendance.data.entity.BiometricTemplate
import dev.ml.smartattendance.domain.model.BiometricType

@Dao
interface BiometricTemplateDao {
    
    @Query("SELECT * FROM biometric_templates WHERE studentId = :studentId AND isActive = 1")
    suspend fun getTemplatesByStudentId(studentId: String): List<BiometricTemplate>
    
    @Query("SELECT * FROM biometric_templates WHERE studentId = :studentId AND type = :type AND isActive = 1")
    suspend fun getTemplateByStudentAndType(studentId: String, type: BiometricType): BiometricTemplate?
    
    @Query("SELECT * FROM biometric_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: String): BiometricTemplate?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: BiometricTemplate)
    
    @Update
    suspend fun updateTemplate(template: BiometricTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: BiometricTemplate)
    
    @Query("UPDATE biometric_templates SET isActive = 0 WHERE studentId = :studentId AND type = :type")
    suspend fun deactivateTemplatesForStudentAndType(studentId: String, type: BiometricType)
    
    @Query("UPDATE biometric_templates SET isActive = 0 WHERE studentId = :studentId")
    suspend fun deactivateAllTemplatesForStudent(studentId: String)
    
    @Query("SELECT COUNT(*) FROM biometric_templates WHERE studentId = :studentId AND isActive = 1")
    suspend fun getActiveTemplateCount(studentId: String): Int
    
    @Query("DELETE FROM biometric_templates WHERE studentId = :studentId")
    suspend fun deleteAllTemplatesForStudent(studentId: String)
}