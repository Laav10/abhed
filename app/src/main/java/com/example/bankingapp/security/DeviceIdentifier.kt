package com.example.bankingapp.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class DeviceIdentifier(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "device_identifier_prefs"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_IS_REGISTERED = "is_registered"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Get or generate a unique device UUID
     * This UUID is generated once and stored securely on the device
     */
    fun getDeviceUUID(): String {
        var deviceUUID = encryptedPrefs.getString(KEY_DEVICE_UUID, null)
        
        if (deviceUUID == null) {
            // Generate new UUID if none exists
            deviceUUID = generateDeviceUUID()
            encryptedPrefs.edit().putString(KEY_DEVICE_UUID, deviceUUID).apply()
        }
        
        return deviceUUID
    }
    
    /**
     * Check if device is already registered with backend
     */
    fun isDeviceRegistered(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_REGISTERED, false)
    }
    
    /**
     * Mark device as registered after successful backend registration
     */
    fun markDeviceAsRegistered() {
        encryptedPrefs.edit().putBoolean(KEY_IS_REGISTERED, true).apply()
    }
    
    /**
     * Generate a unique device identifier
     * Combines device-specific info with random UUID for uniqueness
     */
    private fun generateDeviceUUID(): String {
        val deviceInfo = StringBuilder()
        
        // Add device-specific information for uniqueness
        deviceInfo.append(android.os.Build.MANUFACTURER)
        deviceInfo.append(android.os.Build.MODEL)
        deviceInfo.append(android.os.Build.PRODUCT)
        deviceInfo.append(android.os.Build.DEVICE)
        deviceInfo.append(android.os.Build.BOARD)
        deviceInfo.append(android.os.Build.BRAND)
        
        // Add random UUID
        val randomUUID = UUID.randomUUID().toString()
        
        // Create a hash of device info + random UUID
        val combined = deviceInfo.toString() + randomUUID
        return UUID.nameUUIDFromBytes(combined.toByteArray()).toString()
    }
    
    /**
     * Clear device registration status (for testing/logout)
     */
    fun clearDeviceRegistration() {
        encryptedPrefs.edit().putBoolean(KEY_IS_REGISTERED, false).apply()
    }
}
