package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    
    @Query("SELECT * FROM devices WHERE deviceUUID = :deviceUUID LIMIT 1")
    suspend fun getDeviceByUUID(deviceUUID: String): Device?
    
    @Query("SELECT * FROM devices WHERE userId = :userId")
    suspend fun getDevicesByUserId(userId: Long): List<Device>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)
    
    @Update
    suspend fun updateDevice(device: Device)
    
    @Query("UPDATE devices SET lastLoginAt = :timestamp WHERE deviceUUID = :deviceUUID")
    suspend fun updateLastLogin(deviceUUID: String, timestamp: Long)
    
    @Query("UPDATE devices SET isTrusted = :isTrusted WHERE deviceUUID = :deviceUUID")
    suspend fun updateTrustStatus(deviceUUID: String, isTrusted: Boolean)
    
    @Query("DELETE FROM devices WHERE deviceUUID = :deviceUUID")
    suspend fun deleteDevice(deviceUUID: String)
    
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<Device>>
}
