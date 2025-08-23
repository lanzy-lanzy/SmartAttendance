package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.security.EncryptedData
import javax.crypto.SecretKey

interface EncryptionService {
    suspend fun encryptBiometricData(data: ByteArray): EncryptedData
    suspend fun decryptBiometricData(encryptedData: EncryptedData): ByteArray
    fun generateSecretKey(): SecretKey
    suspend fun encryptString(plainText: String): String
    suspend fun decryptString(encryptedText: String): String
}