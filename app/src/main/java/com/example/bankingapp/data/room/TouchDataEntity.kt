package com.example.bankingapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlin.math.pow

/**
 * Touch Data Entity for Room Database
 * 
 * Stores comprehensive behavioral data for ABHED continuous authentication:
 * - Touch coordinates and pressure
 * - Velocity and timing information
 * - Session and user context
 * - Additional behavioral metrics
 */
@Entity(
    tableName = "touch_data",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["deviceUUID"]),
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class TouchDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Touch coordinates and pressure
    val x: Float,
    val y: Float,
    val pressure: Float,
    
    // Timing and velocity
    val timestamp: Long,
    val velocity: Float,
    
    // Touch type and classification
    val type: String,
    
    // Pressure statistics (serialized)
    val pressureStats: String,
    
    // Session and user context
    val sessionId: String,
    val userId: String,
    val deviceUUID: String,
    
    // Additional behavioral data (serialized)
    val additionalData: String,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if this entity is valid for analysis
     */
    fun isValidForAnalysis(): Boolean {
        return pressure > 0.1f && 
               timestamp > 0 && 
               x >= 0 && y >= 0 &&
               sessionId.isNotEmpty() &&
               userId.isNotEmpty() &&
               deviceUUID.isNotEmpty()
    }
    
    /**
     * Get touch intensity category
     */
    fun getTouchIntensityCategory(): String {
        val intensity = (pressure + velocity) / 2.0f
        return when {
            intensity < 0.3f -> "Light"
            intensity < 0.7f -> "Medium"
            else -> "Heavy"
        }
    }
    
    /**
     * Get time category for analysis
     */
    fun getTimeCategory(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        return when {
            hour in 6..11 -> "Morning"
            hour in 12..17 -> "Afternoon"
            hour in 18..23 -> "Evening"
            else -> "Night"
        }
    }
    
    /**
     * Get spatial region for analysis
     */
    fun getSpatialRegion(): String {
        return when {
            x < 200f && y < 400f -> "Top-Left"
            x < 400f && y < 400f -> "Top-Center"
            x < 600f && y < 400f -> "Top-Right"
            x < 200f && y < 800f -> "Middle-Left"
            x < 400f && y < 800f -> "Middle-Center"
            x < 600f && y < 800f -> "Middle-Right"
            x < 200f -> "Bottom-Left"
            x < 400f -> "Bottom-Center"
            else -> "Bottom-Right"
        }
    }
    
    companion object {
        /**
         * Create entity from basic touch data
         */
        fun fromBasicTouch(
            x: Float,
            y: Float,
            pressure: Float,
            type: String,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            return TouchDataEntity(
                x = x,
                y = y,
                pressure = pressure,
                timestamp = System.currentTimeMillis(),
                velocity = 0f,
                type = type,
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = ""
            )
        }
        
        /**
         * Create entity for keystroke dynamics
         */
        fun forKeystroke(
            keyCode: Int,
            holdTime: Long,
            interKeyDelay: Long,
            pressure: Float,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            val additionalData = mapOf(
                "keyCode" to keyCode,
                "holdTime" to holdTime,
                "interKeyDelay" to interKeyDelay,
                "typingSpeed" to (1000f / interKeyDelay.coerceAtLeast(1))
            )
            
            return TouchDataEntity(
                x = 0f,
                y = 0f,
                pressure = pressure,
                timestamp = System.currentTimeMillis(),
                velocity = 0f,
                type = "KEYSTROKE",
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = additionalData.toString()
            )
        }
        
        /**
         * Create entity for navigation patterns
         */
        fun forNavigation(
            screenName: String,
            action: String,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            val additionalData = mapOf(
                "screenName" to screenName,
                "action" to action,
                "timestamp" to System.currentTimeMillis()
            )
            
            return TouchDataEntity(
                x = 0f,
                y = 0f,
                pressure = 1.0f,
                timestamp = System.currentTimeMillis(),
                velocity = 0f,
                type = "NAVIGATION",
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = additionalData.toString()
            )
        }
        
        /**
         * Create entity for scroll behavior
         */
        fun forScroll(
            startY: Float,
            endY: Float,
            velocity: Float,
            duration: Long,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            val distance = kotlin.math.abs(endY - startY)
            val speed = distance / duration.coerceAtLeast(1)
            
            val additionalData = mapOf(
                "startY" to startY,
                "endY" to endY,
                "distance" to distance,
                "duration" to duration,
                "speed" to speed
            )
            
            return TouchDataEntity(
                x = 0f,
                y = startY,
                pressure = 1.0f,
                timestamp = System.currentTimeMillis(),
                velocity = velocity,
                type = "SCROLL",
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = additionalData.toString()
            )
        }
        
        /**
         * Create entity for swipe gestures
         */
        fun forSwipe(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            duration: Long,
            pressure: Float,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            val distance = kotlin.math.sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
            val velocity = distance / duration.coerceAtLeast(1)
            val angle = kotlin.math.atan2(endY - startY, endX - startX)
            
            val additionalData = mapOf(
                "startX" to startX,
                "startY" to startY,
                "endX" to endX,
                "endY" to endY,
                "distance" to distance,
                "duration" to duration,
                "angle" to angle,
                "direction" to getSwipeDirection(angle)
            )
            
            return TouchDataEntity(
                x = startX,
                y = startY,
                pressure = pressure,
                timestamp = System.currentTimeMillis(),
                velocity = velocity,
                type = "SWIPE",
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = additionalData.toString()
            )
        }
        
        /**
         * Create entity for sensor data
         */
        fun forSensor(
            sensorType: String,
            values: FloatArray,
            userId: String,
            deviceUUID: String,
            sessionId: String
        ): TouchDataEntity {
            val magnitude = when (values.size) {
                3 -> kotlin.math.sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
                else -> 0f
            }
            
            val additionalData = mapOf(
                "sensorType" to sensorType,
                "values" to values.contentToString(),
                "magnitude" to magnitude
            )
            
            return TouchDataEntity(
                x = 0f,
                y = 0f,
                pressure = 1.0f,
                timestamp = System.currentTimeMillis(),
                velocity = 0f,
                type = "SENSOR",
                pressureStats = "",
                sessionId = sessionId,
                userId = userId,
                deviceUUID = deviceUUID,
                additionalData = additionalData.toString()
            )
        }
        
        private fun getSwipeDirection(angle: Float): String {
            return when {
                angle in -0.785f..0.785f -> "RIGHT"
                angle in 0.785f..2.356f -> "DOWN"
                angle in 2.356f..3.927f || angle < -2.356f -> "LEFT"
                else -> "UP"
            }
        }
    }
}


