package com.example.bankingapp.behavioral

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import com.example.bankingapp.data.room.TouchDataDao
import com.example.bankingapp.data.room.TouchDataEntity
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * ABHED Touch Data Manager
 * 
 * Collects comprehensive behavioral data for continuous authentication:
 * - Touch patterns (pressure, velocity, timing)
 * - Swipe gestures (direction, speed, curvature)
 * - Keystroke dynamics (hold time, inter-key delay)
 * - Navigation patterns (app flow, screen transitions)
 * - Device sensors (accelerometer, gyroscope)
 */
class TouchDataManager(
    private val context: Context,
    private val touchDataDao: TouchDataDao
) : SensorEventListener {
    
    companion object {
        private const val TAG = "TouchDataManager"
        private const val MIN_TOUCH_INTERVAL = 50L // Minimum time between touch events (ms)
        private const val VELOCITY_WINDOW_SIZE = 10 // Number of points for velocity calculation
        private const val PRESSURE_THRESHOLD = 0.1f // Minimum pressure to record
        private const val LOCATION_UPDATE_INTERVAL = 5000L // Location update interval (ms)
    }
    
    // Data collection state
    private var isCollecting = false
    private var sessionId = ""
    private var userId = ""
    private var deviceUUID = ""
    
    // Touch data buffers
    private val touchBuffer = mutableListOf<TouchDataPoint>()
    private val velocityTracker = VelocityTracker()
    private val pressureHistory = mutableListOf<Float>()
    private val timingHistory = mutableListOf<Long>()
    
    // Sensor data
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    
    // Location data
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentSpeed = 0.0f
    private var lastLocationUpdate = 0L
    
    // Behavioral patterns
    private val navigationPattern = mutableListOf<String>()
    private val typingPattern = mutableListOf<Long>()
    private val scrollPattern = mutableListOf<Float>()
    
    /**
     * Start data collection session
     */
    fun startCollection(userId: String, deviceUUID: String) {
        this.userId = userId
        this.deviceUUID = deviceUUID
        this.sessionId = generateSessionId()
        this.isCollecting = true
        
        Log.d(TAG, "Started touch data collection for user: $userId, session: $sessionId")
        
        // Initialize sensors
        initializeSensors()
        
        // Start background collection
        startBackgroundCollection()
    }
    
    /**
     * Stop data collection session
     */
    fun stopCollection() {
        isCollecting = false
        
        // Stop sensors
        stopSensors()
        
        // Save remaining data
        saveBufferedData()
        
        Log.d(TAG, "Stopped touch data collection for session: $sessionId")
    }
    
    /**
     * Record touch event with comprehensive data
     */
    fun recordTouchEvent(
        x: Float,
        y: Float,
        pressure: Float,
        type: TouchType,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        if (!isCollecting || pressure < PRESSURE_THRESHOLD) return
        
        val timestamp = System.currentTimeMillis()
        
        // Calculate velocity if we have previous points
        val velocity = calculateVelocity(x, y, timestamp)
        
        // Calculate pressure statistics
        updatePressureHistory(pressure)
        val pressureStats = calculatePressureStatistics()
        
        // Create comprehensive touch data point
        val touchData = TouchDataPoint(
            x = x,
            y = y,
            pressure = pressure,
            timestamp = timestamp,
            type = type,
            velocity = velocity,
            pressureStats = pressureStats,
            sessionId = sessionId,
            userId = userId,
            deviceUUID = deviceUUID,
            additionalData = additionalData
        )
        
        // Add to buffer
        touchBuffer.add(touchData)
        
        // Update velocity tracker
        velocityTracker.addPosition(timestamp, androidx.compose.ui.geometry.Offset(x, y))
        
        // Save to database if buffer is full
        if (touchBuffer.size >= 50) {
            saveBufferedData()
        }
        
        Log.v(TAG, "Recorded touch event: $type at ($x, $y) with pressure $pressure")
    }
    
    /**
     * Record keystroke dynamics
     */
    fun recordKeystroke(
        keyCode: Int,
        pressTime: Long,
        releaseTime: Long,
        pressure: Float
    ) {
        val holdTime = releaseTime - pressTime
        val interKeyDelay = if (typingPattern.isNotEmpty()) {
            pressTime - typingPattern.last()
        } else 0L
        
        typingPattern.add(pressTime)
        
        // Keep only recent typing history
        if (typingPattern.size > 100) {
            typingPattern.removeAt(0)
        }
        
        val keystrokeData = mapOf(
            "keyCode" to keyCode,
            "holdTime" to holdTime,
            "interKeyDelay" to interKeyDelay,
            "typingSpeed" to calculateTypingSpeed(),
            "pressure" to pressure
        )
        
        recordTouchEvent(
            x = 0f,
            y = 0f,
            pressure = pressure,
            type = TouchType.KEYSTROKE,
            additionalData = keystrokeData
        )
    }
    
    /**
     * Record navigation pattern
     */
    fun recordNavigation(screenName: String, action: String) {
        navigationPattern.add("$screenName:$action")
        
        // Keep only recent navigation history
        if (navigationPattern.size > 50) {
            navigationPattern.removeAt(0)
        }
        
        val navigationData = mapOf(
            "screenName" to screenName,
            "action" to action,
            "navigationPath" to navigationPattern.joinToString("->"),
            "patternLength" to navigationPattern.size
        )
        
        recordTouchEvent(
            x = 0f,
            y = 0f,
            pressure = 1.0f,
            type = TouchType.NAVIGATION,
            additionalData = navigationData
        )
    }
    
    /**
     * Record scroll behavior
     */
    fun recordScroll(
        startY: Float,
        endY: Float,
        velocity: Float,
        duration: Long
    ) {
        val scrollDistance = abs(endY - startY)
        val scrollSpeed = scrollDistance / duration
        
        scrollPattern.add(scrollSpeed)
        
        // Keep only recent scroll history
        if (scrollPattern.size > 100) {
            scrollPattern.removeAt(0)
        }
        
        val scrollData = mapOf(
            "startY" to startY,
            "endY" to endY,
            "distance" to scrollDistance,
            "velocity" to velocity,
            "duration" to duration,
            "speed" to scrollSpeed,
            "averageSpeed" to scrollPattern.average().toFloat()
        )
        
        recordTouchEvent(
            x = 0f,
            y = startY,
            pressure = 1.0f,
            type = TouchType.SCROLL,
            additionalData = scrollData
        )
    }
    
    /**
     * Record swipe gesture
     */
    fun recordSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long,
        pressure: Float
    ) {
        val distance = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val velocity = distance / duration
        val angle = atan2(endY - startY, endX - startX)
        
        val swipeData = mapOf(
            "startX" to startX,
            "startY" to startY,
            "endX" to endX,
            "endY" to endY,
            "distance" to distance,
            "duration" to duration,
            "velocity" to velocity,
            "angle" to angle,
            "direction" to getSwipeDirection(angle)
        )
        
        recordTouchEvent(
            x = startX,
            y = startY,
            pressure = pressure,
            type = TouchType.SWIPE,
            additionalData = swipeData
        )
    }
    
    /**
     * Get current behavioral profile
     */
    fun getBehavioralProfile(): BehavioralProfile {
        return BehavioralProfile(
            userId = userId,
            deviceUUID = deviceUUID,
            sessionId = sessionId,
            touchPatterns = analyzeTouchPatterns(),
            navigationPatterns = analyzeNavigationPatterns(),
            typingPatterns = analyzeTypingPatterns(),
            scrollPatterns = analyzeScrollPatterns(),
            pressureProfile = calculatePressureProfile(),
            velocityProfile = calculateVelocityProfile()
        )
    }
    
    // Private helper methods
    
    private fun initializeSensors() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        // Register sensor listeners
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    
    private fun stopSensors() {
        sensorManager?.unregisterListener(this)
    }
    
    private fun startBackgroundCollection() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isCollecting) {
                // Collect device state data
                collectDeviceState()
                
                // Collect location data periodically
                if (System.currentTimeMillis() - lastLocationUpdate > LOCATION_UPDATE_INTERVAL) {
                    collectLocationData()
                }
                
                delay(1000) // Collect every second
            }
        }
    }
    
    private fun collectDeviceState() {
        // Collect battery level, CPU usage, memory usage, etc.
        val deviceState = mapOf(
            "batteryLevel" to getBatteryLevel(),
            "cpuUsage" to getCpuUsage(),
            "memoryUsage" to getMemoryUsage(),
            "screenBrightness" to getScreenBrightness()
        )
        
        recordTouchEvent(
            x = 0f,
            y = 0f,
            pressure = 1.0f,
            type = TouchType.DEVICE_STATE,
            additionalData = deviceState
        )
    }
    
    private fun collectLocationData() {
        // This would integrate with actual location services
        // For now, we'll simulate location data
        lastLocationUpdate = System.currentTimeMillis()
        
        val locationData = mapOf(
            "latitude" to currentLatitude,
            "longitude" to currentLongitude,
            "speed" to currentSpeed,
            "accuracy" to 10.0f
        )
        
        recordTouchEvent(
            x = 0f,
            y = 0f,
            pressure = 1.0f,
            type = TouchType.LOCATION,
            additionalData = locationData
        )
    }
    
    private fun calculateVelocity(x: Float, y: Float, timestamp: Long): Float {
        if (touchBuffer.isEmpty()) return 0f
        
        val lastPoint = touchBuffer.last()
        val timeDiff = timestamp - lastPoint.timestamp
        
        if (timeDiff < MIN_TOUCH_INTERVAL) return 0f
        
        val distance = sqrt((x - lastPoint.x).pow(2) + (y - lastPoint.y).pow(2))
        return distance / timeDiff
    }
    
    private fun updatePressureHistory(pressure: Float) {
        pressureHistory.add(pressure)
        if (pressureHistory.size > 100) {
            pressureHistory.removeAt(0)
        }
    }
    
    private fun calculatePressureStatistics(): PressureStatistics {
        if (pressureHistory.isEmpty()) return PressureStatistics()
        
        return PressureStatistics(
            mean = pressureHistory.average().toFloat(),
            standardDeviation = calculateStandardDeviation(pressureHistory.map { it.toDouble() }),
            min = pressureHistory.minOrNull() ?: 0f,
            max = pressureHistory.maxOrNull() ?: 0f,
            median = calculateMedian(pressureHistory)
        )
    }
    
    private fun calculateTypingSpeed(): Float {
        if (typingPattern.size < 2) return 0f
        
        val totalTime = typingPattern.last() - typingPattern.first()
        val keyCount = typingPattern.size - 1
        
        return if (totalTime > 0) (keyCount * 1000f) / totalTime else 0f
    }
    
    private fun getSwipeDirection(angle: Float): String {
        return when {
            angle in -0.785f..0.785f -> "RIGHT"
            angle in 0.785f..2.356f -> "DOWN"
            angle in 2.356f..3.927f || angle < -2.356f -> "LEFT"
            else -> "UP"
        }
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    private fun saveBufferedData() {
        if (touchBuffer.isEmpty()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = touchBuffer.map { it.toEntity() }
                touchDataDao.insertTouchDataList(entities)
                
                Log.d(TAG, "Saved ${entities.size} touch data points to database")
                
                // Clear buffer after saving
                touchBuffer.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving touch data: ${e.message}")
            }
        }
    }
    
    // Sensor event listener implementation
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]
                    
                    val accelerometerData = mapOf(
                        "accelX" to x,
                        "accelY" to y,
                        "accelZ" to z,
                        "magnitude" to sqrt(x.pow(2) + y.pow(2) + z.pow(2))
                    )
                    
                    recordTouchEvent(
                        x = 0f,
                        y = 0f,
                        pressure = 1.0f,
                        type = TouchType.ACCELEROMETER,
                        additionalData = accelerometerData
                    )
                }
                
                Sensor.TYPE_GYROSCOPE -> {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]
                    
                    val gyroscopeData = mapOf(
                        "gyroX" to x,
                        "gyroY" to y,
                        "gyroZ" to z,
                        "rotationMagnitude" to sqrt(x.pow(2) + y.pow(2) + z.pow(2))
                    )
                    
                    recordTouchEvent(
                        x = 0f,
                        y = 0f,
                        pressure = 1.0f,
                        type = TouchType.GYROSCOPE,
                        additionalData = gyroscopeData
                    )
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed
    }
    
    // Analysis methods for behavioral profile
    private fun analyzeTouchPatterns(): TouchPatternAnalysis {
        if (touchBuffer.isEmpty()) return TouchPatternAnalysis()
        
        val xCoords = touchBuffer.map { it.x }
        val yCoords = touchBuffer.map { it.y }
        val pressures = touchBuffer.map { it.pressure }
        
        return TouchPatternAnalysis(
            averageX = xCoords.average().toFloat(),
            averageY = yCoords.average().toFloat(),
            xStandardDeviation = calculateStandardDeviation(xCoords.map { it.toDouble() }),
            yStandardDeviation = calculateStandardDeviation(yCoords.map { it.toDouble() }),
            pressureProfile = calculatePressureProfile(),
            touchFrequency = calculateTouchFrequency()
        )
    }
    
    private fun analyzeNavigationPatterns(): NavigationPatternAnalysis {
        if (navigationPattern.isEmpty()) return NavigationPatternAnalysis()
        
        val transitions = mutableMapOf<String, Int>()
        for (i in 0 until navigationPattern.size - 1) {
            val transition = "${navigationPattern[i]}->${navigationPattern[i + 1]}"
            transitions[transition] = transitions.getOrDefault(transition, 0) + 1
        }
        
        return NavigationPatternAnalysis(
            totalTransitions = navigationPattern.size - 1,
            uniqueTransitions = transitions.size,
            mostCommonTransition = transitions.maxByOrNull { it.value }?.key ?: "",
            transitionEntropy = calculateEntropy(transitions.values.toList())
        )
    }
    
    private fun analyzeTypingPatterns(): TypingPatternAnalysis {
        if (typingPattern.isEmpty()) return TypingPatternAnalysis()
        
        val intervals = mutableListOf<Long>()
        for (i in 1 until typingPattern.size) {
            intervals.add(typingPattern[i] - typingPattern[i - 1])
        }
        
        return TypingPatternAnalysis(
            totalKeystrokes = typingPattern.size,
            averageInterval = intervals.average().toLong(),
            typingSpeed = calculateTypingSpeed(),
            intervalVariance = calculateVariance(intervals.map { it.toDouble() })
        )
    }
    
    private fun analyzeScrollPatterns(): ScrollPatternAnalysis {
        if (scrollPattern.isEmpty()) return ScrollPatternAnalysis()
        
        return ScrollPatternAnalysis(
            totalScrolls = scrollPattern.size,
            averageSpeed = scrollPattern.average().toFloat(),
            speedVariance = calculateVariance(scrollPattern.map { it.toDouble() }),
            preferredDirection = if (scrollPattern.average() > 0) "DOWN" else "UP"
        )
    }
    
    private fun calculatePressureProfile(): PressureProfile {
        if (pressureHistory.isEmpty()) return PressureProfile()
        
        return PressureProfile(
            mean = pressureHistory.average().toFloat(),
            standardDeviation = calculateStandardDeviation(pressureHistory.map { it.toDouble() }),
            skewness = calculateSkewness(pressureHistory.map { it.toDouble() }),
            kurtosis = calculateKurtosis(pressureHistory.map { it.toDouble() })
        )
    }
    
    private fun calculateVelocityProfile(): VelocityProfile {
        val velocities = touchBuffer.mapNotNull { it.velocity }
        if (velocities.isEmpty()) return VelocityProfile()
        
        return VelocityProfile(
            mean = velocities.average().toFloat(),
            standardDeviation = calculateStandardDeviation(velocities.map { it.toDouble() }),
            maxVelocity = velocities.maxOrNull() ?: 0f,
            velocityDistribution = calculateVelocityDistribution(velocities)
        )
    }
    
    // Statistical calculation helpers
    private fun calculateStandardDeviation(values: List<Double>): Float {
        if (values.size < 2) return 0f
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
    
    private fun calculateMedian(values: List<Float>): Float {
        val sorted = values.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateEntropy(values: List<Int>): Double {
        val total = values.sum().toDouble()
        if (total == 0.0) return 0.0
        
        return -values.sumOf { count ->
            val probability = count / total
            if (probability > 0) probability * ln(probability) else 0.0
        }
    }
    
    private fun calculateSkewness(values: List<Double>): Float {
        if (values.size < 3) return 0f
        
        val mean = values.average()
        val stdDev = sqrt(calculateVariance(values))
        if (stdDev == 0.0) return 0f
        
        val n = values.size.toDouble()
        val skewness = values.sumOf { ((it - mean) / stdDev).pow(3) } / n
        return skewness.toFloat()
    }
    
    private fun calculateKurtosis(values: List<Double>): Float {
        if (values.size < 4) return 0f
        
        val mean = values.average()
        val stdDev = sqrt(calculateVariance(values))
        if (stdDev == 0.0) return 0f
        
        val n = values.size.toDouble()
        val kurtosis = values.sumOf { ((it - mean) / stdDev).pow(4) } / n - 3
        return kurtosis.toFloat()
    }
    
    private fun calculateTouchFrequency(): Float {
        if (touchBuffer.size < 2) return 0f
        
        val timeSpan = touchBuffer.last().timestamp - touchBuffer.first().timestamp
        return if (timeSpan > 0) (touchBuffer.size * 1000f) / timeSpan else 0f
    }
    
    private fun calculateVelocityDistribution(velocities: List<Float>): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        
        velocities.forEach { velocity ->
            val range = when {
                velocity < 0.1f -> "Very Slow"
                velocity < 0.5f -> "Slow"
                velocity < 1.0f -> "Medium"
                velocity < 2.0f -> "Fast"
                else -> "Very Fast"
            }
            distribution[range] = distribution.getOrDefault(range, 0) + 1
        }
        
        return distribution
    }
    
    // Device state collection methods (simplified)
    private fun getBatteryLevel(): Int = 85 // Would integrate with BatteryManager
    private fun getCpuUsage(): Float = 0.3f // Would integrate with system stats
    private fun getMemoryUsage(): Float = 0.6f // Would integrate with ActivityManager
    private fun getScreenBrightness(): Int = 150 // Would integrate with Settings
    
    // Extension function to convert TouchDataPoint to Entity
    private fun TouchDataPoint.toEntity(): TouchDataEntity {
        return TouchDataEntity(
            id = 0, // Auto-generated
            x = x,
            y = y,
            pressure = pressure,
            timestamp = timestamp,
            type = type.name,
            velocity = velocity,
            pressureStats = pressureStats.toString(),
            sessionId = sessionId,
            userId = userId,
            deviceUUID = deviceUUID,
            additionalData = additionalData.toString()
        )
    }
}

// Data classes for behavioral analysis
data class BehavioralProfile(
    val userId: String,
    val deviceUUID: String,
    val sessionId: String,
    val touchPatterns: TouchPatternAnalysis,
    val navigationPatterns: NavigationPatternAnalysis,
    val typingPatterns: TypingPatternAnalysis,
    val scrollPatterns: ScrollPatternAnalysis,
    val pressureProfile: PressureProfile,
    val velocityProfile: VelocityProfile
)

data class TouchPatternAnalysis(
    val averageX: Float = 0f,
    val averageY: Float = 0f,
    val xStandardDeviation: Float = 0f,
    val yStandardDeviation: Float = 0f,
    val pressureProfile: PressureProfile = PressureProfile(),
    val touchFrequency: Float = 0f
)

data class NavigationPatternAnalysis(
    val totalTransitions: Int = 0,
    val uniqueTransitions: Int = 0,
    val mostCommonTransition: String = "",
    val transitionEntropy: Double = 0.0
)

data class TypingPatternAnalysis(
    val totalKeystrokes: Int = 0,
    val averageInterval: Long = 0L,
    val typingSpeed: Float = 0f,
    val intervalVariance: Double = 0.0
)

data class ScrollPatternAnalysis(
    val totalScrolls: Int = 0,
    val averageSpeed: Float = 0f,
    val speedVariance: Double = 0.0,
    val preferredDirection: String = ""
)

data class PressureProfile(
    val mean: Float = 0f,
    val standardDeviation: Float = 0f,
    val skewness: Float = 0f,
    val kurtosis: Float = 0f
)

data class VelocityProfile(
    val mean: Float = 0f,
    val standardDeviation: Float = 0f,
    val maxVelocity: Float = 0f,
    val velocityDistribution: Map<String, Int> = emptyMap()
)

data class PressureStatistics(
    val mean: Float = 0f,
    val standardDeviation: Float = 0f,
    val min: Float = 0f,
    val max: Float = 0f,
    val median: Float = 0f
)
