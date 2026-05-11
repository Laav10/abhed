package com.example.bankingapp.services

import android.util.Log
import com.example.bankingapp.config.SMSConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Real SMS Service using Twilio SMS Gateway
 * 
 * Backend service for sending SMS to users.
 * Credentials are configured in SMSConfig.kt
 */
class TwilioSMSService() : SMSService {
    
    private val accountSid = SMSConfig.TWILIO_ACCOUNT_SID
    private val authToken = SMSConfig.TWILIO_AUTH_TOKEN
    private val fromPhoneNumber = SMSConfig.TWILIO_FROM_PHONE
    
    companion object {
        private const val TAG = "TwilioSMSService"
        private const val TWILIO_API_URL = "https://api.twilio.com/2010-04-01/Accounts"
        private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()
    }
    
    private val client = OkHttpClient()
    
    override suspend fun sendOTP(phoneNumber: String, otpCode: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val message = "Your ABHED Banking OTP is: $otpCode. Valid for 10 minutes. Do not share this code."
                
                // Validate phone number
                if (phoneNumber.isBlank()) {
                    Log.e(TAG, "Phone number is blank!")
                    return@withContext Result.failure(Exception("Phone number is blank"))
                }
                
                // Twilio expects form-encoded data, not JSON
                val formData = "To=${phoneNumber}&From=${fromPhoneNumber}&Body=${message}"
                val requestBody = formData.toRequestBody(FORM_MEDIA_TYPE)
                
                Log.d(TAG, "Sending SMS to: '$phoneNumber'")
                Log.d(TAG, "From: '$fromPhoneNumber'")
                Log.d(TAG, "Message: '$message'")
                Log.d(TAG, "Request body: $formData")
                Log.d(TAG, "Account SID: $accountSid")
                Log.d(TAG, "Auth Token: ${authToken.take(8)}...")
                Log.d(TAG, "API URL: $TWILIO_API_URL/$accountSid/Messages.json")
                
                val request = Request.Builder()
                    .url("$TWILIO_API_URL/$accountSid/Messages.json")
                    .addHeader("Authorization", Credentials.basic(accountSid, authToken))
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "SMS sent successfully to $phoneNumber. Response: $responseBody")
                    Result.success("SMS sent successfully to $phoneNumber")
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Failed to send SMS. Status: ${response.code}, Error: $errorBody")
                    Log.e(TAG, "Response headers: ${response.headers}")
                    Log.e(TAG, "Request URL: ${request.url}")
                    Result.failure(Exception("Failed to send SMS: ${response.code} - $errorBody"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS to $phoneNumber", e)
                Result.failure(e)
            }
        }
    }
    
    override fun isServiceAvailable(): Boolean = 
        accountSid.isNotBlank() && authToken.isNotBlank() && fromPhoneNumber.isNotBlank()
}
