package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.DeviceRegistration
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceRegistrationDao {
    
    @Query("SELECT * FROM device_registration WHERE deviceId = :deviceId")
    suspend fun getDeviceRegistration(deviceId: String): DeviceRegistration?
    
    @Query("SELECT * FROM device_registration WHERE userId = :userId")
    suspend fun getDeviceRegistrationByUserId(userId: Long): DeviceRegistration?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceRegistration(deviceRegistration: DeviceRegistration)
    
    @Update
    suspend fun updateDeviceRegistration(deviceRegistration: DeviceRegistration)
    
    @Query("UPDATE device_registration SET isOnboardingCompleted = :completed WHERE deviceId = :deviceId")
    suspend fun updateOnboardingStatus(deviceId: String, completed: Boolean)
    
    @Query("UPDATE device_registration SET lastLoginAt = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastLogin(deviceId: String, timestamp: Long)
    
    @Query("DELETE FROM device_registration WHERE deviceId = :deviceId")
    suspend fun deleteDeviceRegistration(deviceId: String)
    
    @Query("SELECT * FROM device_registration")
    fun getAllDeviceRegistrations(): Flow<List<DeviceRegistration>>
}
