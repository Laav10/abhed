package com.example.bankingapp.data.repository

import android.util.Log
import com.example.bankingapp.data.models.DeviceRegistration
import com.example.bankingapp.data.room.DeviceRegistrationDao

class DeviceRegistrationRepository(
    private val deviceRegistrationDao: DeviceRegistrationDao
) {
    
    /**
     * Check if device is registered and onboarding is completed
     */
    suspend fun isDeviceRegistered(deviceId: String): Boolean {
        val registration = deviceRegistrationDao.getDeviceRegistration(deviceId)
        return registration?.isOnboardingCompleted == true
    }
    
    /**
     * Get device registration info
     */
    suspend fun getDeviceRegistration(deviceId: String): DeviceRegistration? {
        return deviceRegistrationDao.getDeviceRegistration(deviceId)
    }
    
    /**
     * Register device during onboarding (before completion)
     */
    suspend fun registerDevice(
        deviceId: String,
        userId: Long,
        primaryBiometricType: String
    ) {
        val registration = DeviceRegistration(
            deviceId = deviceId,
            userId = userId,
            primaryBiometricType = primaryBiometricType,
            isOnboardingCompleted = false
        )
        deviceRegistrationDao.insertDeviceRegistration(registration)
        Log.d("DeviceRegistration", "Device registered: $deviceId with primary biometric: $primaryBiometricType")
    }
    
    /**
     * Mark onboarding as completed (called after successful biometric collection)
     */
    suspend fun completeOnboarding(deviceId: String) {
        deviceRegistrationDao.updateOnboardingStatus(deviceId, true)
        Log.d("DeviceRegistration", "Onboarding completed for device: $deviceId")
    }
    
    /**
     * Update last login timestamp
     */
    suspend fun updateLastLogin(deviceId: String) {
        deviceRegistrationDao.updateLastLogin(deviceId, System.currentTimeMillis())
    }
    
    /**
     * Get primary biometric type for this device
     */
    suspend fun getPrimaryBiometricType(deviceId: String): String? {
        return deviceRegistrationDao.getDeviceRegistration(deviceId)?.primaryBiometricType
    }
    
    /**
     * Unregister device (for testing purposes)
     */
    suspend fun unregisterDevice(deviceId: String) {
        deviceRegistrationDao.deleteDeviceRegistration(deviceId)
        Log.d("DeviceRegistration", "Device unregistered: $deviceId")
    }
}
