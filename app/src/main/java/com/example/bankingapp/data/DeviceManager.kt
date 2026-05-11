package com.example.bankingapp.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import java.util.*

data class DeviceInfo(
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val screenResolution: String,
    val registrationTime: Long,
    val status: DeviceStatus,
    val userId: String? = null
)

enum class DeviceStatus {
    UNREGISTERED,    // Device ID generated, collecting data
    REGISTERING,     // Onboarding in progress
    ACTIVE,          // Onboarding complete, device active
    BIOMETRIC_READY  // Ready for biometric login
}

class DeviceManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_STATUS = "device_status"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REGISTRATION_TIME = "registration_time"
        private const val TAG = "DeviceManager"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Generate or retrieve device ID when app opens
     * Device starts as UNREGISTERED status
     */
    fun initializeDevice(): DeviceInfo {
        val deviceId = getOrCreateDeviceId()
        val status = getDeviceStatus()
        
        Log.d(TAG, "Device initialized: $deviceId with status: $status")
        
        return DeviceInfo(
            deviceId = deviceId,
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            screenResolution = getScreenResolution(),
            registrationTime = getRegistrationTime(),
            status = status,
            userId = getUserId()
        )
    }
    
    /**
     * Get existing device ID or generate new one
     */
    private fun getOrCreateDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            // Generate new device ID
            deviceId = generateUniqueDeviceId()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            Log.d(TAG, "New device ID generated: $deviceId")
        } else {
            Log.d(TAG, "Existing device ID retrieved: $deviceId")
        }
        
        return deviceId
    }
    
    /**
     * Generate unique device identifier
     */
    private fun generateUniqueDeviceId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, 
            Settings.Secure.ANDROID_ID
        )
        
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().substring(0, 8)
        
        return "DEV_${androidId}_${timestamp}_$random"
    }
    
    /**
     * Get current device status
     */
    fun getDeviceStatus(): DeviceStatus {
        val statusString = prefs.getString(KEY_DEVICE_STATUS, DeviceStatus.UNREGISTERED.name)
        return try {
            DeviceStatus.valueOf(statusString ?: DeviceStatus.UNREGISTERED.name)
        } catch (e: IllegalArgumentException) {
            DeviceStatus.UNREGISTERED
        }
    }
    
    /**
     * Update device status
     */
    fun updateDeviceStatus(status: DeviceStatus) {
        prefs.edit().putString(KEY_DEVICE_STATUS, status.name).apply()
        Log.d(TAG, "Device status updated to: $status")
    }
    
    /**
     * Check if device is registered and active
     */
    fun isDeviceActive(): Boolean {
        return getDeviceStatus() == DeviceStatus.ACTIVE || 
               getDeviceStatus() == DeviceStatus.BIOMETRIC_READY
    }
    
    /**
     * Check if device can use biometric login
     */
    fun canUseBiometric(): Boolean {
        return getDeviceStatus() == DeviceStatus.BIOMETRIC_READY
    }
    
    /**
     * Set user ID after onboarding completion
     */
    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        Log.d(TAG, "User ID set: $userId")
    }
    
    /**
     * Get stored user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get device registration time
     */
    private fun getRegistrationTime(): Long {
        var time = prefs.getLong(KEY_REGISTRATION_TIME, 0L)
        
        if (time == 0L) {
            time = System.currentTimeMillis()
            prefs.edit().putLong(KEY_REGISTRATION_TIME, time).apply()
        }
        
        return time
    }
    
    /**
     * Get screen resolution
     */
    private fun getScreenResolution(): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }
    
    /**
     * Reset device for testing (remove in production)
     */
    fun resetDevice() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Device reset - all data cleared")
    }
    
    /**
     * Get complete device info
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getOrCreateDeviceId(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            screenResolution = getScreenResolution(),
            registrationTime = getRegistrationTime(),
            status = getDeviceStatus(),
            userId = getUserId()
        )
    }
}
