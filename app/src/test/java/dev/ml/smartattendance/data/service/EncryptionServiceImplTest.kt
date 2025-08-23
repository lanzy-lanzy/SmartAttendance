package dev.ml.smartattendance.data.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.ml.smartattendance.domain.model.security.EncryptedData
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionServiceImplTest {

    private lateinit var encryptionService: EncryptionServiceImpl
    private val mockKeyStore = mockk<KeyStore>(relaxed = true)
    private val mockSecretKey = mockk<SecretKey>()
    private val mockCipher = mockk<Cipher>(relaxed = true)
    private val mockKeyGenerator = mockk<KeyGenerator>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock KeyStore static methods
        mockkStatic(KeyStore::class)
        every { KeyStore.getInstance("AndroidKeyStore") } returns mockKeyStore
        
        // Mock Base64 static methods
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns "encodedString"
        every { Base64.decode(any<String>(), any()) } returns byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        
        // Mock Cipher static methods
        mockkStatic(Cipher::class)
        every { Cipher.getInstance("AES/CBC/PKCS7Padding") } returns mockCipher
        
        // Mock KeyGenerator static methods
        mockkStatic(KeyGenerator::class)
        every { KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore") } returns mockKeyGenerator
        
        // Setup default mock behaviors
        every { mockKeyStore.containsAlias("SmartAttendanceSecretKey") } returns true
        every { mockKeyStore.getKey("SmartAttendanceSecretKey", null) } returns mockSecretKey
        every { mockCipher.iv } returns byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        every { mockCipher.doFinal(any()) } returns byteArrayOf(10, 20, 30, 40)
        every { mockKeyGenerator.generateKey() } returns mockSecretKey
        
        encryptionService = EncryptionServiceImpl()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `encryptBiometricData should return EncryptedData with encrypted bytes and IV`() = runTest {
        // Given
        val inputData = "test data".toByteArray()
        val expectedEncryptedBytes = byteArrayOf(10, 20, 30, 40)
        val expectedIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        
        every { mockCipher.doFinal(inputData) } returns expectedEncryptedBytes
        every { mockCipher.iv } returns expectedIv

        // When
        val result = encryptionService.encryptBiometricData(inputData)

        // Then
        assertArrayEquals(expectedEncryptedBytes, result.encryptedBytes)
        assertArrayEquals(expectedIv, result.iv)
        
        verify { mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey) }
        verify { mockCipher.doFinal(inputData) }
    }

    @Test
    fun `decryptBiometricData should return original data`() = runTest {
        // Given
        val encryptedBytes = byteArrayOf(10, 20, 30, 40)
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val encryptedData = EncryptedData(encryptedBytes, iv)
        val expectedDecryptedData = "test data".toByteArray()
        
        every { mockCipher.doFinal(encryptedBytes) } returns expectedDecryptedData

        // When
        val result = encryptionService.decryptBiometricData(encryptedData)

        // Then
        assertArrayEquals(expectedDecryptedData, result)
        
        verify { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<IvParameterSpec>()) }
        verify { mockCipher.doFinal(encryptedBytes) }
    }

    @Test
    fun `encryptString should return base64 encoded string`() = runTest {
        // Given
        val plainText = "test string"
        val expectedEncryptedBytes = byteArrayOf(10, 20, 30, 40)
        val expectedIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val expectedEncodedString = "encodedString"
        
        every { mockCipher.doFinal(plainText.toByteArray()) } returns expectedEncryptedBytes
        every { mockCipher.iv } returns expectedIv
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns expectedEncodedString

        // When
        val result = encryptionService.encryptString(plainText)

        // Then
        assertEquals(expectedEncodedString, result)
        
        verify { mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey) }
        verify { Base64.encodeToString(any(), Base64.DEFAULT) }
    }

    @Test
    fun `decryptString should return original plain text`() = runTest {
        // Given
        val encryptedText = "encryptedString"
        val decodedBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 10, 20, 30, 40)
        val expectedPlainText = "test string"
        
        every { Base64.decode(encryptedText, Base64.DEFAULT) } returns decodedBytes
        every { mockCipher.doFinal(byteArrayOf(10, 20, 30, 40)) } returns expectedPlainText.toByteArray()

        // When
        val result = encryptionService.decryptString(encryptedText)

        // Then
        assertEquals(expectedPlainText, result)
        
        verify { Base64.decode(encryptedText, Base64.DEFAULT) }
        verify { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<IvParameterSpec>()) }
    }

    @Test
    fun `generateSecretKey should create new key with proper specifications`() {
        // Given
        mockkConstructor(KeyGenParameterSpec.Builder::class)
        val mockBuilder = mockk<KeyGenParameterSpec.Builder>(relaxed = true)
        val mockKeyGenParameterSpec = mockk<KeyGenParameterSpec>()
        
        every { anyConstructed<KeyGenParameterSpec.Builder>().setBlockModes(any()) } returns mockBuilder
        every { mockBuilder.setEncryptionPaddings(any()) } returns mockBuilder
        every { mockBuilder.setUserAuthenticationRequired(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockKeyGenParameterSpec
        
        // When
        val result = encryptionService.generateSecretKey()

        // Then
        assertEquals(mockSecretKey, result)
        
        verify { mockKeyGenerator.init(mockKeyGenParameterSpec) }
        verify { mockKeyGenerator.generateKey() }
    }

    @Test
    fun `constructor should generate new key if alias does not exist`() {
        // Given
        every { mockKeyStore.containsAlias("SmartAttendanceSecretKey") } returns false
        
        // When
        val service = EncryptionServiceImpl()

        // Then
        verify { mockKeyGenerator.generateKey() }
    }

    @Test
    fun `constructor should not generate new key if alias exists`() {
        // Given
        every { mockKeyStore.containsAlias("SmartAttendanceSecretKey") } returns true
        
        // When
        val service = EncryptionServiceImpl()

        // Then
        verify(exactly = 0) { mockKeyGenerator.generateKey() }
    }

    @Test
    fun `encryptBiometricData should handle empty data`() = runTest {
        // Given
        val inputData = byteArrayOf()
        val expectedEncryptedBytes = byteArrayOf()
        val expectedIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        
        every { mockCipher.doFinal(inputData) } returns expectedEncryptedBytes
        every { mockCipher.iv } returns expectedIv

        // When
        val result = encryptionService.encryptBiometricData(inputData)

        // Then
        assertArrayEquals(expectedEncryptedBytes, result.encryptedBytes)
        assertArrayEquals(expectedIv, result.iv)
    }

    @Test
    fun `decryptBiometricData should handle empty encrypted data`() = runTest {
        // Given
        val encryptedBytes = byteArrayOf()
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val encryptedData = EncryptedData(encryptedBytes, iv)
        val expectedDecryptedData = byteArrayOf()
        
        every { mockCipher.doFinal(encryptedBytes) } returns expectedDecryptedData

        // When
        val result = encryptionService.decryptBiometricData(encryptedData)

        // Then
        assertArrayEquals(expectedDecryptedData, result)
    }

    @Test
    fun `encryptString should handle empty string`() = runTest {
        // Given
        val plainText = ""
        val expectedEncryptedBytes = byteArrayOf()
        val expectedIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val expectedEncodedString = "encodedEmptyString"
        
        every { mockCipher.doFinal(plainText.toByteArray()) } returns expectedEncryptedBytes
        every { mockCipher.iv } returns expectedIv
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns expectedEncodedString

        // When
        val result = encryptionService.encryptString(plainText)

        // Then
        assertEquals(expectedEncodedString, result)
    }

    @Test
    fun `decryptString should handle empty encrypted string case`() = runTest {
        // Given
        val encryptedText = "emptyEncryptedString"
        val decodedBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16) // Only IV, no encrypted data
        val expectedPlainText = ""
        
        every { Base64.decode(encryptedText, Base64.DEFAULT) } returns decodedBytes
        every { mockCipher.doFinal(byteArrayOf()) } returns expectedPlainText.toByteArray()

        // When
        val result = encryptionService.decryptString(encryptedText)

        // Then
        assertEquals(expectedPlainText, result)
    }

    @Test
    fun `round trip encryption and decryption should return original data`() = runTest {
        // Given
        val originalData = "Hello, World! This is a test string with special characters: @#$%^&*()".toByteArray()
        val encryptedBytes = byteArrayOf(50, 60, 70, 80, 90, 100)
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        
        // Mock encryption
        every { mockCipher.doFinal(originalData) } returns encryptedBytes
        every { mockCipher.iv } returns iv
        
        // Mock decryption
        every { mockCipher.doFinal(encryptedBytes) } returns originalData

        // When - encrypt then decrypt
        val encryptedData = encryptionService.encryptBiometricData(originalData)
        val decryptedData = encryptionService.decryptBiometricData(encryptedData)

        // Then
        assertArrayEquals(originalData, decryptedData)
    }

    @Test
    fun `round trip string encryption and decryption should return original string`() = runTest {
        // Given
        val originalString = "Hello, World! This is a test string with special characters: @#$%^&*()"
        val encryptedBytes = byteArrayOf(50, 60, 70, 80, 90, 100)
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val combinedBytes = iv + encryptedBytes
        val encodedString = "testEncodedString"
        
        // Mock encryption path
        every { mockCipher.doFinal(originalString.toByteArray()) } returns encryptedBytes
        every { mockCipher.iv } returns iv
        every { Base64.encodeToString(combinedBytes, Base64.DEFAULT) } returns encodedString
        
        // Mock decryption path
        every { Base64.decode(encodedString, Base64.DEFAULT) } returns combinedBytes
        every { mockCipher.doFinal(encryptedBytes) } returns originalString.toByteArray()

        // When - encrypt then decrypt
        val encryptedString = encryptionService.encryptString(originalString)
        val decryptedString = encryptionService.decryptString(encryptedString)

        // Then
        assertEquals(originalString, decryptedString)
    }
}