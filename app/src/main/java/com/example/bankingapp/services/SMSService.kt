package com.example.bankingapp.services

interface SMSService {
    /**
     * Send OTP via SMS to the specified phone number
     * @param phoneNumber The recipient phone number
     * @param otpCode The OTP code to send
     * @return Result indicating success or failure
     */
    suspend fun sendOTP(phoneNumber: String, otpCode: String): Result<String>
    
    /**
     * Check if SMS service is available
     */
    fun isServiceAvailable(): Boolean
}
