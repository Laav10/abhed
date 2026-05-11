package com.example.bankingapp.network

import android.content.Context
import android.util.Log
import com.example.bankingapp.data.BackendSyncData
import com.example.bankingapp.data.DeviceInfo
import com.example.bankingapp.data.models.TouchDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit



class BackendSyncService(private val context: Context) {
    
    companion object {
        private const val TAG = "BackendSyncService"
        private const val BASE_URL = "http://10.0.2.2:5000" // For Android emulator
        private const val TIMEOUT_SECONDS = 30L
        
        // API Endpoints
        private const val ENDPOINT_DEVICE_ACTIVATION = "/api/device/activate"
        private const val ENDPOINT_TOUCH_DATA_SYNC = "/api/touch-data/sync"
        private const val ENDPOINT_DEVICE_STATUS = "/api/device/status"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Activate device and sync all collected touch data
     */
    suspend fun activateDeviceAndSyncData(
        deviceInfo: DeviceInfo,
        touchData: List<TouchDataPoint>,
        userId: String
    ): BackendSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting device activation and data sync for device: ${deviceInfo.deviceId}")
            
            // Step 1: Activate device
            val activationResult = activateDevice(deviceInfo, userId)
            if (activationResult is BackendSyncResult.Failure) {
                return@withContext BackendSyncResult.Failure(
                    "Device activation failed: ${activationResult.errorMessage}"
                )
            }
            
            // Step 2: Sync touch data
            val syncResult = syncTouchData(deviceInfo.deviceId, userId, touchData)
            if (syncResult is BackendSyncResult.Failure) {
                Log.w(TAG, "Touch data sync failed, but device is activated")
                // Device is activated even if data sync fails
            }
            
            Log.d(TAG, "Device activation completed successfully")
            return@withContext BackendSyncResult.Success(
                deviceId = deviceInfo.deviceId,
                userId = userId,
                touchDataSynced = syncResult is BackendSyncResult.Success
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during device activation: ${e.message}", e)
            return@withContext BackendSyncResult.Failure(
                "Unexpected error: ${e.message}"
            )
        }
    }
    
    /**
     * Activate device on backend
     */
    private suspend fun activateDevice(
        deviceInfo: DeviceInfo,
        userId: String
    ): BackendSyncResult = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("device_id", deviceInfo.deviceId)
                put("user_id", userId)
                put("device_model", deviceInfo.deviceModel)
                put("android_version", deviceInfo.androidVersion)
                put("screen_resolution", deviceInfo.screenResolution)
                put("registration_time", deviceInfo.registrationTime)
                put("status", "ACTIVE")
            }.toString()
            
            val request = Request.Builder()
                .url("$BASE_URL$ENDPOINT_DEVICE_ACTIVATION")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device activated successfully on backend")
                return@withContext BackendSyncResult.Success(
                    deviceId = deviceInfo.deviceId,
                    userId = userId,
                    touchDataSynced = false
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Device activation failed: ${response.code} - $errorBody")
                return@withContext BackendSyncResult.Failure(
                    "Backend error: ${response.code} - $errorBody"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error activating device: ${e.message}", e)
            return@withContext BackendSyncResult.Failure(
                "Network error: ${e.message}"
            )
        }
    }
    
    /**
     * Sync touch data to backend
     */
    private suspend fun syncTouchData(
        deviceId: String,
        userId: String,
        touchData: List<TouchDataPoint>
    ): BackendSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Syncing ${touchData.size} touch data points to backend")
            
            val touchDataJson = touchData.map { point ->
                JSONObject().apply {
                    put("x", point.x)
                    put("y", point.y)
                    put("pressure", point.pressure)
                    put("timestamp", point.timestamp)
                    put("type", point.type.name)
                    put("velocity", point.velocity)
                    put("session_id", point.sessionId)
                    put("user_id", point.userId ?: userId)
                    put("device_uuid", point.deviceUUID)
                    put("additional_data", JSONObject(point.additionalData))
                }
            }
            
            val requestBody = JSONObject().apply {
                put("device_id", deviceId)
                put("user_id", userId)
                put("touch_data", touchDataJson)
                put("total_interactions", touchData.size)
                put("sync_timestamp", System.currentTimeMillis())
            }.toString()
            
            val request = Request.Builder()
                .url("$BASE_URL$ENDPOINT_TOUCH_DATA_SYNC")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Touch data synced successfully: ${touchData.size} points")
                return@withContext BackendSyncResult.Success(
                    deviceId = deviceId,
                    userId = userId,
                    touchDataSynced = true
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Touch data sync failed: ${response.code} - $errorBody")
                return@withContext BackendSyncResult.Failure(
                    "Backend error: ${response.code} - $errorBody"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing touch data: ${e.message}", e)
            return@withContext BackendSyncResult.Failure(
                "Network error: ${e.message}"
            )
        }
    }
    
    /**
     * Check device status on backend
     */
    suspend fun checkDeviceStatus(deviceId: String): DeviceStatusResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL$ENDPOINT_DEVICE_STATUS?device_id=$deviceId")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse = JSONObject(responseBody)
                
                val status = jsonResponse.optString("status", "UNKNOWN")
                val userId = jsonResponse.optString("user_id", null)
                val isActive = jsonResponse.optBoolean("is_active", false)
                
                Log.d(TAG, "Device status checked: $status, Active: $isActive")
                
                return@withContext DeviceStatusResult.Success(
                    status = status,
                    userId = userId,
                    isActive = isActive
                )
            } else {
                Log.e(TAG, "Failed to check device status: ${response.code}")
                return@withContext DeviceStatusResult.Failure(
                    "Backend error: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device status: ${e.message}", e)
            return@withContext DeviceStatusResult.Failure(
                "Network error: ${e.message}"
            )
        }
    }
    
    /**
     * Test backend connectivity
     */
    suspend fun testBackendConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val isConnected = response.isSuccessful
            
            Log.d(TAG, "Backend connection test: ${if (isConnected) "SUCCESS" else "FAILED"}")
            return@withContext isConnected
            
        } catch (e: Exception) {
            Log.e(TAG, "Backend connection test failed: ${e.message}")
            return@withContext false
        }
    }
}

