package dev.ml.smartattendance.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dev.ml.smartattendance.domain.model.security.SecurityEvent
import dev.ml.smartattendance.domain.service.SecurityManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManagerImpl @Inject constructor(
    private val context: Context
) : SecurityManager {
    
    private val securityEvents = mutableListOf<SecurityEvent>()
    
    override fun isDeviceSecure(): Boolean {
        // Check if device has screen lock set up
        return try {
            val secureSettings = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCK_PATTERN_ENABLED
            ) == 1 || Settings.Secure.getInt(
                context.contentResolver,
                "lockscreen.password_type"
            ) != 0
            secureSettings
        } catch (e: Exception) {
            false
        }
    }
    
    override fun detectRootAccess(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }
    
    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }
    
    private fun checkRootMethod3(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val bufferedReader = process.inputStream.bufferedReader()
            bufferedReader.readLine() != null
        } catch (t: Throwable) {
            false
        }
    }
    
    override fun validateAppIntegrity(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            // In a real implementation, you would check against known signatures
            // For now, we'll just check if signatures exist
            packageInfo.signatures?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun logSecurityEvent(event: SecurityEvent) {
        securityEvents.add(event)
        // In a real implementation, you might also send this to a remote server
        // or store it in the database for audit purposes
    }
    
    fun getSecurityEvents(): List<SecurityEvent> = securityEvents.toList()
}