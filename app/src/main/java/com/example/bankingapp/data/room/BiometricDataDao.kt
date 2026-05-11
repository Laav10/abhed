package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.BiometricData
import kotlinx.coroutines.flow.Flow

@Dao
interface BiometricDataDao {
    
    @Query("SELECT * FROM biometric_data WHERE userId = :userId")
    suspend fun getBiometricDataByUserId(userId: Long): List<BiometricData>
    
    @Query("SELECT * FROM biometric_data WHERE userId = :userId AND biometricType = :biometricType")
    suspend fun getBiometricDataByUserAndType(userId: Long, biometricType: String): BiometricData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiometricData(biometricData: BiometricData): Long
    
    @Update
    suspend fun updateBiometricData(biometricData: BiometricData)
    
    @Query("DELETE FROM biometric_data WHERE userId = :userId")
    suspend fun deleteBiometricDataByUserId(userId: Long)
    
    @Query("SELECT * FROM biometric_data")
    fun getAllBiometricData(): Flow<List<BiometricData>>
}
