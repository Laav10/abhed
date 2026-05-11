package com.example.bankingapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import java.util.*

class TouchDataStorage(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "touch_data_storage"
        private const val KEY_TOUCH_DATA_LIST = "touch_data_list"
        private const val KEY_TOTAL_INTERACTIONS = "total_interactions"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val TAG = "TouchDataStorage"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Store touch data locally until device activation
     */
    fun storeTouchData(touchData: TouchDataPoint) {
        val existingData = getStoredTouchData()
        existingData.add(touchData)
        
        val jsonData = gson.toJson(existingData)
        prefs.edit()
            .putString(KEY_TOUCH_DATA_LIST, jsonData)
            .putInt(KEY_TOTAL_INTERACTIONS, existingData.size)
            .apply()
        
        Log.d(TAG, "Touch data stored locally. Total: ${existingData.size}")
    }
    
    /**
     * Get all stored touch data
     */
    fun getStoredTouchData(): MutableList<TouchDataPoint> {
        val jsonData = prefs.getString(KEY_TOUCH_DATA_LIST, "[]")
        return try {
            val type = object : TypeToken<MutableList<TouchDataPoint>>() {}.type
            gson.fromJson(jsonData, type) ?: mutableListOf()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stored touch data: ${e.message}")
            mutableListOf()
        }
    }
    
    /**
     * Get total interactions count
     */
    fun getTotalInteractions(): Int {
        return prefs.getInt(KEY_TOTAL_INTERACTIONS, 0)
    }
    
    /**
     * Start new session
     */
    fun startNewSession() {
        val sessionId = "session_${System.currentTimeMillis()}"
        prefs.edit()
            .putString(KEY_SESSION_START_TIME, System.currentTimeMillis().toString())
            .apply()
        
        Log.d(TAG, "New session started: $sessionId")
    }
    
    /**
     * Get session start time
     */
    fun getSessionStartTime(): Long {
        return prefs.getLong(KEY_SESSION_START_TIME, System.currentTimeMillis())
    }
    
    /**
     * Get session duration in milliseconds
     */
    fun getSessionDuration(): Long {
        val startTime = getSessionStartTime()
        return System.currentTimeMillis() - startTime
    }
    
    /**
     * Get session statistics
     */
    fun getSessionStats(): SessionStats {
        val data = getStoredTouchData()
        val totalInteractions = data.size
        val sessionDuration = getSessionDuration()
        
        // Group by touch type
        val touchTypeCounts = data.groupBy { it.type }.mapValues { it.value.size }
        
        // Group by element
        val elementCounts = data.groupBy { 
            (it.additionalData["element"] as? String) ?: "unknown" 
        }.mapValues { it.value.size }
        
        // Group by action
        val actionCounts = data.groupBy { 
            (it.additionalData["action"] as? String) ?: "unknown" 
        }.mapValues { it.value.size }
        
        return SessionStats(
            totalInteractions = totalInteractions,
            sessionDuration = sessionDuration,
            touchTypeCounts = touchTypeCounts,
            elementCounts = elementCounts,
            actionCounts = actionCounts,
            averagePressure = data.map { it.pressure }.average(),
            averageVelocity = data.map { it.velocity }.average()
        )
    }
    
    /**
     * Clear all stored data (after successful backend sync)
     */
    fun clearStoredData() {
        prefs.edit()
            .remove(KEY_TOUCH_DATA_LIST)
            .remove(KEY_TOTAL_INTERACTIONS)
            .remove(KEY_SESSION_START_TIME)
            .apply()
        
        Log.d(TAG, "All stored touch data cleared after backend sync")
    }
    
    /**
     * Check if there's stored data to sync
     */
    fun hasStoredData(): Boolean {
        return getTotalInteractions() > 0
    }
    
    /**
     * Get data summary for logging
     */
    fun getDataSummary(): String {
        val stats = getSessionStats()
        return """
            📊 TOUCH DATA SUMMARY:
            🎯 Total Interactions: ${stats.totalInteractions}
            ⏱️ Session Duration: ${stats.sessionDuration}ms
            📱 Touch Types: ${stats.touchTypeCounts}
            🎮 Elements: ${stats.elementCounts}
            🚀 Actions: ${stats.actionCounts}
            💪 Avg Pressure: ${String.format("%.2f", stats.averagePressure)}
            🏃 Avg Velocity: ${String.format("%.2f", stats.averageVelocity)}
        """.trimIndent()
    }
    
    /**
     * Export data for backend sync
     */
    fun exportDataForBackend(): BackendSyncData {
        val deviceInfo = DeviceManager(context).getDeviceInfo()
        val touchData = getStoredTouchData()
        val sessionStats = getSessionStats()
        
        return BackendSyncData(
            deviceId = deviceInfo.deviceId,
            userId = deviceInfo.userId,
            sessionStartTime = getSessionStartTime(),
            sessionDuration = getSessionDuration(),
            totalInteractions = touchData.size,
            touchData = touchData,
            sessionStats = sessionStats,
            deviceInfo = deviceInfo
        )
    }
}

data class SessionStats(
    val totalInteractions: Int,
    val sessionDuration: Long,
    val touchTypeCounts: Map<TouchType, Int>,
    val elementCounts: Map<String, Int>,
    val actionCounts: Map<String, Int>,
    val averagePressure: Double,
    val averageVelocity: Double
)

data class BackendSyncData(
    val deviceId: String,
    val userId: String?,
    val sessionStartTime: Long,
    val sessionDuration: Long,
    val totalInteractions: Int,
    val touchData: List<TouchDataPoint>,
    val sessionStats: SessionStats,
    val deviceInfo: DeviceInfo
)
