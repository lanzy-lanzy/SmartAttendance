package dev.ml.smartattendance.domain.model.auth

import dev.ml.smartattendance.domain.model.UserRole

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    
    // Student-specific fields
    val studentId: String? = null,
    val course: String? = null,
    val enrollmentDate: Long? = null,
    
    // Admin-specific fields
    val adminLevel: AdminLevel = AdminLevel.BASIC,
    val permissions: List<String> = emptyList(),
    
    val isActive: Boolean = true
)

enum class AdminLevel {
    BASIC,      // Can manage events and view reports
    SUPER       // Can manage users and system settings
}

data class AuthResult(
    val user: User? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole,
    
    // Student-specific
    val studentId: String? = null,
    val course: String? = null,
    
    // Admin-specific
    val adminLevel: AdminLevel = AdminLevel.BASIC
)