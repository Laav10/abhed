package com.example.bankingapp.data.room

import android.util.Log
import com.example.bankingapp.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DatabaseHealthChecker(
    private val database: BankingDatabase
) {
    
    companion object {
        private const val TAG = "DatabaseHealthChecker"
        private const val MAX_OTP_AGE_HOURS = 24L
        private const val MAX_INACTIVE_USER_DAYS = 90L
    }
    
    /**
     * Perform comprehensive database health check
     */
    suspend fun performHealthCheck(): DatabaseHealthReport = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            val report = DatabaseHealthReport(
                timestamp = System.currentTimeMillis(),
                totalUsers = database.userDao().getAllUsers().first().size,
                totalDevices = database.deviceDao().getAllDevices().first().size,
                totalDebitCards = database.debitCardDao().getAllDebitCards().first().size,
                totalOTPs = database.otpDao().getAllOTPs().first().size,
                expiredOTPs = countExpiredOTPs(),
                inactiveUsers = countInactiveUsers(),
                orphanedDevices = countOrphanedDevices(),
                databaseSize = estimateDatabaseSize(),
                lastBackupTime = getLastBackupTime(),
                recommendations = generateRecommendations()
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Health check completed in ${duration}ms")
            
            report
            
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            DatabaseHealthReport(
                timestamp = System.currentTimeMillis(),
                error = e.message
            )
        }
    }
    
    /**
     * Count expired OTPs
     */
    private suspend fun countExpiredOTPs(): Int {
        val currentTime = System.currentTimeMillis()
        val allOTPs: List<com.example.bankingapp.data.models.OTP> = database.otpDao().getAllOTPs().first()
        return allOTPs.count { it.expiresAt < currentTime }
    }
    
    /**
     * Count inactive users
     */
    private suspend fun countInactiveUsers(): Int {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_INACTIVE_USER_DAYS)
        val allUsers: List<com.example.bankingapp.data.models.User> = database.userDao().getAllUsers().first()
        return allUsers.count { it.updatedAt < cutoffTime }
    }
    
    /**
     * Count orphaned devices (devices without users)
     */
    private suspend fun countOrphanedDevices(): Int {
        val allDevices: List<com.example.bankingapp.data.models.Device> = database.deviceDao().getAllDevices().first()
        val allUsers: List<com.example.bankingapp.data.models.User> = database.userDao().getAllUsers().first()
        val userIds = allUsers.map { it.id }.toSet()
        
        return allDevices.count { device ->
            !userIds.contains(device.userId)
        }
    }
    
    /**
     * Estimate database size
     */
    private suspend fun estimateDatabaseSize(): Long {
        val users = database.userDao().getAllUsers().first().size
        val devices = database.deviceDao().getAllDevices().first().size
        val cards = database.debitCardDao().getAllDebitCards().first().size
        val otps = database.otpDao().getAllOTPs().first().size
        
        // Rough estimation (in bytes)
        return (users * 256L + devices * 128L + cards * 512L + otps * 64L)
    }
    
    /**
     * Get last backup time
     */
    private fun getLastBackupTime(): Long? {
        // This would be implemented with actual backup tracking
        return null
    }
    
    /**
     * Generate health recommendations
     */
    private suspend fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Add recommendations based on health metrics
        val expiredOTPs = countExpiredOTPs()
        val inactiveUsers = countInactiveUsers()
        val orphanedDevices = countOrphanedDevices()
        
        if (expiredOTPs > 0) {
            recommendations.add("Clean up $expiredOTPs expired OTPs")
        }
        
        if (inactiveUsers > 0) {
            recommendations.add("Review $inactiveUsers inactive users")
        }
        
        if (orphanedDevices > 0) {
            recommendations.add("Remove $orphanedDevices orphaned devices")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Database is healthy - no action needed")
        }
        
        return recommendations
    }
    
    /**
     * Clean up expired OTPs
     */
    suspend fun cleanupExpiredOTPs(): Int = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val allOTPs: List<com.example.bankingapp.data.models.OTP> = database.otpDao().getAllOTPs().first()
            val expiredOTPs = allOTPs.filter { it.expiresAt < currentTime }
            
            for (otp in expiredOTPs) {
                database.otpDao().deleteOTPsByPhoneNumber(otp.phoneNumber)
            }
            
            Log.d(TAG, "Cleaned up ${expiredOTPs.size} expired OTPs")
            expiredOTPs.size
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup expired OTPs", e)
            0
        }
    }
    
    /**
     * Optimize database
     */
    suspend fun optimizeDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clean up expired OTPs
            cleanupExpiredOTPs()
            
            // In a real app, you might also:
            // - Vacuum the database
            // - Update statistics
            // - Reindex tables
            
            Log.d(TAG, "Database optimization completed")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Database optimization failed", e)
            false
        }
    }
}

/**
 * Data class for database health report
 */
data class DatabaseHealthReport(
    val timestamp: Long,
    val totalUsers: Int = 0,
    val totalDevices: Int = 0,
    val totalDebitCards: Int = 0,
    val totalOTPs: Int = 0,
    val expiredOTPs: Int = 0,
    val inactiveUsers: Int = 0,
    val orphanedDevices: Int = 0,
    val databaseSize: Long = 0,
    val lastBackupTime: Long? = null,
    val recommendations: List<String> = emptyList(),
    val error: String? = null
) {
    val isHealthy: Boolean
        get() = error == null && expiredOTPs == 0 && orphanedDevices == 0
    
    val healthScore: Int
        get() = when {
            error != null -> 0
            expiredOTPs > 10 -> 50
            orphanedDevices > 5 -> 70
            else -> 100
        }
}
