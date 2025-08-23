package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.biometric.AuthResult
import dev.ml.smartattendance.domain.model.biometric.BiometricCapability
import dev.ml.smartattendance.domain.model.biometric.EnrollmentResult

interface BiometricAuthenticator {
    suspend fun authenticate(): AuthResult
    suspend fun enrollBiometric(userId: String): EnrollmentResult
    fun isAvailable(): BiometricCapability
}