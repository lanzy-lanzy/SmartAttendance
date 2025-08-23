package dev.ml.smartattendance.data.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.ml.smartattendance.domain.model.security.EncryptedData
import dev.ml.smartattendance.domain.service.EncryptionService
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionServiceImpl @Inject constructor() : EncryptionService {
    
    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "SmartAttendanceSecretKey"
        const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateSecretKey()
        }
    }
    
    override suspend fun encryptBiometricData(data: ByteArray): EncryptedData {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedBytes = cipher.doFinal(data)
        val iv = cipher.iv
        
        return EncryptedData(encryptedBytes, iv)
    }
    
    override suspend fun decryptBiometricData(encryptedData: EncryptedData): ByteArray {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivSpec = IvParameterSpec(encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        
        return cipher.doFinal(encryptedData.encryptedBytes)
    }
    
    override suspend fun encryptString(plainText: String): String {
        val encryptedData = encryptBiometricData(plainText.toByteArray())
        val combined = encryptedData.iv + encryptedData.encryptedBytes
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    override suspend fun decryptString(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        val iv = combined.sliceArray(0..15) // AES block size is 16 bytes
        val encryptedBytes = combined.sliceArray(16 until combined.size)
        
        val encryptedData = EncryptedData(encryptedBytes, iv)
        val decryptedBytes = decryptBiometricData(encryptedData)
        return String(decryptedBytes)
    }
    
    override fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            generateSecretKey()
        }
    }
}