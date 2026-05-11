package com.example.bankingapp.config

/**
 * Configuration for SMS services
 * 
 * Backend configuration for Twilio SMS service.
 * These credentials are used to send SMS to users.
 * 
 * For production, consider:
 * - Using environment variables
 * - Using a secure configuration service
 * - Rotating credentials regularly
 */
object SMSConfig {
    
    // Twilio configuration placeholders.
    // Keep real values out of source control and provide them at runtime.
    const val TWILIO_ACCOUNT_SID = ""
    const val TWILIO_AUTH_TOKEN = ""
    const val TWILIO_FROM_PHONE = "" // e.g. +15551234567
    
    // SMS Gateway Configuration
    const val SMS_GATEWAY_URL = "https://your-sms-gateway.com/api/send"
    const val SMS_API_KEY = "your_sms_api_key_here"
    
    // OTP Configuration
    const val OTP_LENGTH = 6
    const val OTP_EXPIRY_MINUTES = 10L
    const val OTP_RESEND_COOLDOWN_SECONDS = 60L
    
    // Rate Limiting
    const val MAX_OTP_ATTEMPTS_PER_HOUR = 5
    const val MAX_OTP_ATTEMPTS_PER_DAY = 20
    
    // SMS Templates
    const val OTP_SMS_TEMPLATE = "Your ABHED Banking OTP is: {OTP}. Valid for {EXPIRY} minutes. Do not share this code."
    const val REGISTRATION_SUCCESS_SMS = "Welcome to ABHED Banking! Your device has been successfully registered."
    
    /**
     * Check if Twilio SMS service is configured
     * This should be checked via CredentialsManager instead
     */
    fun isTwilioConfigured(): Boolean {
        // Always return false - use CredentialsManager.areCredentialsValid() instead
        return false
    }
    
    /**
     * Check if custom SMS gateway is configured
     */
    fun isCustomGatewayConfigured(): Boolean {
        return SMS_GATEWAY_URL != "https://your-sms-gateway.com/api/send" &&
               SMS_API_KEY != "your_sms_api_key_here"
    }
}
