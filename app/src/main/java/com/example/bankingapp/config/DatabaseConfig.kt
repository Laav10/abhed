package com.example.bankingapp.config

/**
 * Database configuration settings
 * 
 * This file contains all database-related configuration
 * that can be easily modified for different environments
 */
object DatabaseConfig {
    
    // Database Settings
    const val DATABASE_NAME = "banking_database"
    const val DATABASE_VERSION = 1
    
    // Table Names
    const val TABLE_USERS = "users"
    const val TABLE_DEVICES = "devices"
    const val TABLE_DEBIT_CARDS = "debit_cards"
    const val TABLE_OTPS = "otps"
    
    // Backup Settings
    const val MAX_BACKUP_FILES = 5
    const val BACKUP_RETENTION_DAYS = 30
    
    // Cleanup Settings
    const val OTP_EXPIRY_HOURS = 24L
    const val MAX_INACTIVE_USER_DAYS = 90L
    const val MAX_ORPHANED_DEVICE_DAYS = 30L
    
    // Performance Settings
    const val MAX_QUERY_RESULTS = 1000
    const val BATCH_SIZE = 100
    
    // Security Settings
    const val ENCRYPT_DATABASE = true
    const val ENCRYPT_BACKUPS = true
    
    // Logging Settings
    const val LOG_DATABASE_OPERATIONS = true
    const val LOG_SLOW_QUERIES = true
    const val SLOW_QUERY_THRESHOLD_MS = 100L
    
    /**
     * Get database file path
     */
    fun getDatabasePath(context: android.content.Context): String {
        return context.getDatabasePath(DATABASE_NAME).absolutePath
    }
    
    /**
     * Check if database encryption is enabled
     */
    fun isEncryptionEnabled(): Boolean = ENCRYPT_DATABASE
    
    /**
     * Get backup retention period in milliseconds
     */
    fun getBackupRetentionPeriodMs(): Long {
        return BACKUP_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }
}


















