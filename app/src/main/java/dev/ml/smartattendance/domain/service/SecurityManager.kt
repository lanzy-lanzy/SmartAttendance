package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.security.SecurityEvent

interface SecurityManager {
    fun isDeviceSecure(): Boolean
    fun detectRootAccess(): Boolean
    fun validateAppIntegrity(): Boolean
    suspend fun logSecurityEvent(event: SecurityEvent)
}