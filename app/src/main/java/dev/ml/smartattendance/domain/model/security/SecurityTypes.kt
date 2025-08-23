package dev.ml.smartattendance.domain.model.security

import javax.crypto.SecretKey

data class EncryptedData(
    val encryptedBytes: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

enum class SecurityEventType {
    BIOMETRIC_AUTH_SUCCESS,
    BIOMETRIC_AUTH_FAILURE,
    LOCATION_ACCESS_DENIED,
    UNAUTHORIZED_ACCESS_ATTEMPT,
    DATA_ENCRYPTION_FAILURE,
    ROOT_DETECTION,
    APP_INTEGRITY_VIOLATION
}

data class SecurityEvent(
    val type: SecurityEventType,
    val timestamp: Long,
    val userId: String?,
    val details: String,
    val severity: SecuritySeverity
)

enum class SecuritySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}