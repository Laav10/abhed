package com.example.bankingapp.services

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Mock SMS Service for testing purposes
 * In production, replace this with a real SMS gateway implementation
 */
class MockSMSService : SMSService {
    
    companion object {
        private const val TAG = "MockSMSService"
        private var lastSentOTP: String? = null
        private var lastSentPhoneNumber: String? = null
    }
    
    override suspend fun sendOTP(phoneNumber: String, otpCode: String): Result<String> {
        return try {
            // Simulate network delay
            delay(1000)
            
            // Log the SMS for testing purposes - make it very visible
            Log.i(TAG, "")
            Log.i(TAG, "==================================================")
            Log.i(TAG, "📱 MOCK SMS SERVICE - OTP SENT FOR TESTING")
            Log.i(TAG, "Phone: $phoneNumber")
            Log.i(TAG, "OTP Code: $otpCode")
            Log.i(TAG, "Message: Your ABHED Banking OTP is: $otpCode")
            Log.i(TAG, "==================================================")
            Log.i(TAG, "")
            
            // Store the last sent OTP for easy retrieval during testing
            lastSentOTP = otpCode
            lastSentPhoneNumber = phoneNumber
            
            // In a real app, this would be sent via SMS gateway
            // For now, we just simulate success
            Result.success("SMS sent successfully to $phoneNumber (Check logs for OTP)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            Result.failure(e)
        }
    }
    
    override fun isServiceAvailable(): Boolean = true
    
    /**
     * Get the OTP that was "sent" (for testing purposes)
     * In production, this would not exist
     */
    fun getLastSentOTP(phoneNumber: String): String? {
        // This is just for testing - in real app, OTP would be sent via SMS
        Log.d(TAG, "🔍 Retrieving OTP for testing: $phoneNumber")
        return null
    }
}
