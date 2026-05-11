package com.example.bankingapp.data.repository

import android.util.Log
import com.example.bankingapp.data.models.*
import com.example.bankingapp.data.room.*
import com.example.bankingapp.security.DeviceIdentifier
import com.example.bankingapp.services.SMSService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val userDao: UserDao,
    private val deviceDao: DeviceDao,
    private val debitCardDao: DebitCardDao,
    private val otpDao: OTPDao,
    private val deviceIdentifier: DeviceIdentifier,
    private val smsService: SMSService
) {
    
    /**
     * Get device UUID for this device
     */
    fun getDeviceUUID(): String = deviceIdentifier.getDeviceUUID()
    
    /**
     * Check if device is already registered
     */
    fun isDeviceRegistered(): Boolean = deviceIdentifier.isDeviceRegistered()
    
    /**
     * Verify debit card exists and get associated phone number
     */
    suspend fun verifyDebitCard(debitCardNumber: String): DebitCard? {
        Log.d("AuthRepository", "Verifying debit card: '$debitCardNumber'")
        
        // Get all cards for debugging
        val allCards = debitCardDao.getAllDebitCards().first()
        Log.d("AuthRepository", "All cards in database: ${allCards.map { it.cardNumber }}")
        Log.d("AuthRepository", "Total cards: ${allCards.size}")
        
        val result = debitCardDao.getDebitCardByNumber(debitCardNumber)
        Log.d("AuthRepository", "Query result for '$debitCardNumber': $result")
        
        return result
    }
    
    /**
     * Generate and store OTP for phone number
     */
    suspend fun generateOTP(phoneNumber: String): String {
        // Generate 6-digit OTP
        val otpCode = generateRandomOTP()
        
        // OTP expires in 10 minutes
        val expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)
        
        val otp = OTP(
            phoneNumber = phoneNumber,
            otpCode = otpCode,
            expiresAt = expiresAt
        )
        
        // Store OTP in database
        otpDao.insertOTP(otp)
        
        // Send OTP via SMS
        Log.d("AuthRepository", "Sending OTP via Twilio SMS Service")
        Log.d("AuthRepository", "Phone number: $phoneNumber")
        Log.d("AuthRepository", "OTP code: $otpCode")
        Log.d("AuthRepository", "SMS service available: ${smsService.isServiceAvailable()}")
        
        val smsResult = smsService.sendOTP(phoneNumber, otpCode)
        if (smsResult.isFailure) {
            Log.e("AuthRepository", "Failed to send SMS to $phoneNumber", smsResult.exceptionOrNull())
            Log.e("AuthRepository", "SMS error details: ${smsResult.exceptionOrNull()?.message}")
            // In production, you might want to handle this differently
            // For now, we'll still return the OTP for testing
        } else {
            Log.d("AuthRepository", "SMS sent successfully: ${smsResult.getOrNull()}")
        }
        
        return otpCode
    }
    
    /**
     * Verify OTP and complete registration
     */
    suspend fun verifyOTPAndRegister(
        phoneNumber: String,
        otpCode: String,
        name: String,
        debitCardNumber: String
    ): Result<User> {
        return try {
            // Verify OTP
            val currentTime = System.currentTimeMillis()
            val validOTP = otpDao.getValidOTP(phoneNumber, otpCode, currentTime)
            
            if (validOTP == null) {
                return Result.failure(Exception("Invalid or expired OTP"))
            }
            
            // Mark OTP as used
            otpDao.markOTPAsUsed(validOTP.id)
            
            // Get device UUID
            val deviceUUID = deviceIdentifier.getDeviceUUID()
            
            // Create user
            val user = User(
                name = name,
                debitCardNumber = debitCardNumber,
                phoneNumber = phoneNumber,
                deviceUUID = deviceUUID
            )
            
            val userId = userDao.insertUser(user)
            val createdUser = user.copy(id = userId)
            
            // Create device record
            val device = Device(
                deviceUUID = deviceUUID,
                userId = userId
            )
            deviceDao.insertDevice(device)
            
            // Mark device as registered
            deviceIdentifier.markDeviceAsRegistered()
            
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user by device UUID
     */
    suspend fun getUserByDeviceUUID(deviceUUID: String): User? {
        return userDao.getUserByDeviceUUID(deviceUUID)
    }
    
    /**
     * Update device last login time
     */
    suspend fun updateDeviceLastLogin(deviceUUID: String) {
        deviceDao.updateLastLogin(deviceUUID, System.currentTimeMillis())
    }
    
    /**
     * Generate random 6-digit OTP
     */
    private fun generateRandomOTP(): String {
        return (100000..999999).random().toString()
    }
    
    /**
     * Clean up expired OTPs
     */
    suspend fun cleanupExpiredOTPs() {
        val currentTime = System.currentTimeMillis()
        otpDao.deleteExpiredOTPs(currentTime)
    }
    
    /**
     * Get all users (for admin purposes)
     */
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    /**
     * Get all devices (for admin purposes)
     */
    fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()
    
    /**
     * Get all debit cards (for admin purposes)
     */
    fun getAllDebitCards(): Flow<List<DebitCard>> = debitCardDao.getAllDebitCards()
    

}
