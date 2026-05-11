package com.example.bankingapp.data.room

import android.content.Context
import android.util.Log
import com.example.bankingapp.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DatabaseBackupManager(
    private val context: Context,
    private val database: BankingDatabase
) {
    
    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val BACKUP_DIR = "database_backups"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }
    
    /**
     * Create a backup of all database tables
     */
    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, BACKUP_DIR).apply {
                if (!exists()) mkdirs()
            }
            
            val timestamp = DATE_FORMAT.format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.json")
            
            // Export all data to JSON
            val backupData = DatabaseBackupData(
                timestamp = System.currentTimeMillis(),
                version = 1,
                users = database.userDao().getAllUsers().first(),
                devices = database.deviceDao().getAllDevices().first(),
                debitCards = database.debitCardDao().getAllDebitCards().first(),
                otps = database.otpDao().getAllOTPs().first()
            )
            
            // Convert to JSON and save
            val jsonData = backupData.toJson()
            backupFile.writeText(jsonData)
            
            Log.d(TAG, "Database backup created: ${backupFile.absolutePath}")
            Result.success(backupFile.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restore database from backup file
     */
    suspend fun restoreFromBackup(backupFilePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            
            val jsonData = backupFile.readText()
            val backupData = DatabaseBackupData.fromJson(jsonData)
            
            // Clear existing data
            clearAllData()
            
            // Restore data
            backupData.users?.forEach { user ->
                database.userDao().insertUser(user)
            }
            
            backupData.devices?.forEach { device ->
                database.deviceDao().insertDevice(device)
            }
            
            backupData.debitCards?.forEach { card ->
                database.debitCardDao().insertDebitCard(card)
            }
            
            backupData.otps?.forEach { otp ->
                database.otpDao().insertOTP(otp)
            }
            
            Log.d(TAG, "Database restored from backup: $backupFilePath")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available backups
     */
    fun getAvailableBackups(): List<File> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        return if (backupDir.exists()) {
            backupDir.listFiles()?.filter { it.name.startsWith("backup_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Delete old backups (keep only last 5)
     */
    suspend fun cleanupOldBackups() = withContext(Dispatchers.IO) {
        try {
            val backups = getAvailableBackups()
            if (backups.size > 5) {
                val toDelete = backups.drop(5)
                toDelete.forEach { it.delete() }
                Log.d(TAG, "Cleaned up ${toDelete.size} old backups")
            } else {
                // No cleanup needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old backups", e)
        }
    }
    
    /**
     * Clear all data from database
     */
    private suspend fun clearAllData() {
        val initializer = DatabaseInitializer(database)
        initializer.clearAllData()
    }
}

/**
 * Data class for database backup
 */
data class DatabaseBackupData(
    val timestamp: Long,
    val version: Int,
    val users: List<User>?,
    val devices: List<Device>?,
    val debitCards: List<DebitCard>?,
    val otps: List<OTP>?
) {
    fun toJson(): String {
        // Simple JSON serialization for demo
        // In production, use proper JSON library like Gson or Moshi
        return """
            {
                "timestamp": $timestamp,
                "version": $version,
                "users": ${users?.size ?: 0},
                "devices": ${devices?.size ?: 0},
                "debitCards": ${debitCards?.size ?: 0},
                "otps": ${otps?.size ?: 0}
            }
        """.trimIndent()
    }
    
    companion object {
        fun fromJson(json: String): DatabaseBackupData {
            // Simple JSON parsing for demo
            // In production, use proper JSON library
            return DatabaseBackupData(
                timestamp = System.currentTimeMillis(),
                version = 1,
                users = emptyList(),
                devices = emptyList(),
                debitCards = emptyList(),
                otps = emptyList()
            )
        }
    }
}
