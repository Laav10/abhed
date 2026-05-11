package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.OTP
import kotlinx.coroutines.flow.Flow

@Dao
interface OTPDao {
    
    @Query("SELECT * FROM otps WHERE phoneNumber = :phoneNumber AND otpCode = :otpCode AND isUsed = 0 AND expiresAt > :currentTime LIMIT 1")
    suspend fun getValidOTP(phoneNumber: String, otpCode: String, currentTime: Long): OTP?
    
    @Query("SELECT * FROM otps WHERE phoneNumber = :phoneNumber ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestOTP(phoneNumber: String): OTP?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOTP(otp: OTP): Long
    
    @Query("UPDATE otps SET isUsed = 1 WHERE id = :otpId")
    suspend fun markOTPAsUsed(otpId: Long)
    
    @Query("DELETE FROM otps WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredOTPs(currentTime: Long)
    
    @Query("DELETE FROM otps WHERE phoneNumber = :phoneNumber")
    suspend fun deleteOTPsByPhoneNumber(phoneNumber: String)
    
    @Query("SELECT * FROM otps")
    fun getAllOTPs(): Flow<List<OTP>>
}
