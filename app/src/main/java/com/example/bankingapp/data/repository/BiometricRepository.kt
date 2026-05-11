package com.example.bankingapp.data.repository

import android.util.Log
import com.example.bankingapp.data.models.BiometricData
import com.example.bankingapp.data.room.BiometricDataDao
import kotlinx.coroutines.flow.Flow

class BiometricRepository(
    private val biometricDataDao: BiometricDataDao
) {
    
    /**
     * Save biometric data for a user
     */
    suspend fun saveBiometricData(
        userId: Long,
        biometricType: String,
        status: String,
        dataHash: String? = null
    ): Long {
        val biometricData = BiometricData(
            userId = userId,
            biometricType = biometricType,
            status = status,
            dataHash = dataHash
        )
        
        val id = biometricDataDao.insertBiometricData(biometricData)
        Log.d("BiometricRepository", "Saved biometric data: $biometricType for user $userId with status: $status")
        return id
    }
    
    /**
     * Get biometric data for a user
     */
    suspend fun getBiometricDataByUserId(userId: Long): List<BiometricData> { 
        return biometricDataDao.getBiometricDataByUserId(userId)
    }
    
    /**
     * Get specific biometric data for a user
     */
    suspend fun getBiometricDataByUserAndType(userId: Long, biometricType: String): BiometricData? {
        return biometricDataDao.getBiometricDataByUserAndType(userId, biometricType)
    }
    
    /**
     * Update biometric data
     */
    suspend fun updateBiometricData(biometricData: BiometricData) {
        biometricDataDao.updateBiometricData(biometricData)
    }
    
    /**
     * Delete all biometric data for a user
     */
    suspend fun deleteBiometricDataByUserId(userId: Long) {
        biometricDataDao.deleteBiometricDataByUserId(userId)
        Log.d("BiometricRepository", "Deleted all biometric data for user $userId")
    }
    
    /**
     * Get all biometric data (for admin purposes)
     */
    fun getAllBiometricData(): Flow<List<BiometricData>> {
        return biometricDataDao.getAllBiometricData()
    }
    
    /**
     * Check if user has completed biometric setup
     */
    suspend fun hasCompletedBiometricSetup(userId: Long): Boolean {
        val biometricData = getBiometricDataByUserId(userId)
        return biometricData.any { it.status == "completed" || it.status == "available" }
    }
}
