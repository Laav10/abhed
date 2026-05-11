package com.example.bankingapp.touch

import android.content.Context
import android.util.Log
import com.example.bankingapp.data.room.BankingDatabase
import com.example.bankingapp.ml.BehavioralFeatureVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException

data class TouchEvent(
    val type: TouchEventType,
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val pressure: Float = 1.0f,
    val size: Float = 1.0f,
    val duration: Long = 0L,
    val metadata: String = ""
)

enum class TouchEventType {
    TAP,
    SCROLL,
    SWIPE,
    LONG_PRESS,
    PINCH_ZOOM
}

class TouchDataCollector(private val context: Context) {
    
    private val touchEvents = mutableListOf<TouchEvent>()
    private val database = BankingDatabase.getDatabase(context)
    
    companion object {
        private const val TAG = "TouchDataCollector"
        private const val MIN_EVENTS_FOR_TRAINING = 50
    }
    
    /**
     * Record a tap event
     */
    fun recordTap(x: Float, y: Float, timestamp: Long, pressure: Float = 1.0f, size: Float = 1.0f) {
        val event = TouchEvent(
            type = TouchEventType.TAP,
            x = x,
            y = y,
            timestamp = timestamp,
            pressure = pressure,
            size = size
        )
        touchEvents.add(event)
        Log.d(TAG, "Recorded tap at ($x, $y) at $timestamp")
    }
    
    /**
     * Record a scroll event
     */
    fun recordScroll(scrollDistance: Float, timestamp: Long) {
        val event = TouchEvent(
            type = TouchEventType.SCROLL,
            x = 0f,
            y = scrollDistance,
            timestamp = timestamp,
            metadata = "distance:$scrollDistance"
        )
        touchEvents.add(event)
        Log.d(TAG, "Recorded scroll distance $scrollDistance at $timestamp")
    }
    
    /**
     * Record a swipe event
     */
    fun recordSwipe(direction: String, timestamp: Long, startX: Float = 0f, startY: Float = 0f, endX: Float = 0f, endY: Float = 0f) {
        val event = TouchEvent(
            type = TouchEventType.SWIPE,
            x = startX,
            y = startY,
            timestamp = timestamp,
            metadata = "direction:$direction,endX:$endX,endY:$endY"
        )
        touchEvents.add(event)
        Log.d(TAG, "Recorded swipe $direction at $timestamp")
    }
    
    /**
     * Record a long press event
     */
    fun recordLongPress(x: Float, y: Float, duration: Long, timestamp: Long, pressure: Float = 1.0f) {
        val event = TouchEvent(
            type = TouchEventType.LONG_PRESS,
            x = x,
            y = y,
            timestamp = timestamp,
            pressure = pressure,
            duration = duration
        )
        touchEvents.add(event)
        Log.d(TAG, "Recorded long press at ($x, $y) for ${duration}ms at $timestamp")
    }
    
    /**
     * Record a pinch zoom event
     */
    fun recordPinchZoom(centerX: Float, centerY: Float, scaleFactor: Float, timestamp: Long) {
        val event = TouchEvent(
            type = TouchEventType.PINCH_ZOOM,
            x = centerX,
            y = centerY,
            timestamp = timestamp,
            metadata = "scale:$scaleFactor"
        )
        touchEvents.add(event)
        Log.d(TAG, "Recorded pinch zoom scale $scaleFactor at $timestamp")
    }
    
    /**
     * Get collected touch events
     */
    fun getCollectedEvents(): List<TouchEvent> = touchEvents.toList()
    
    /**
     * Get event count by type
     */
    fun getEventCountByType(): Map<TouchEventType, Int> {
        return touchEvents.groupBy { it.type }.mapValues { it.value.size }
    }
    
    /**
     * Check if enough data has been collected for training
     */
    fun hasEnoughDataForTraining(): Boolean {
        return touchEvents.size >= MIN_EVENTS_FOR_TRAINING &&
                touchEvents.groupBy { it.type }.size >= 3 // At least 3 different gesture types
    }
    
    /**
     * Generate behavioral features from collected touch data
     */
    fun generateBehavioralFeatures(): BehavioralFeatureVector? {
        if (!hasEnoughDataForTraining()) {
            Log.w(TAG, "Not enough touch data for feature generation")
            return null
        }
        
        val tapEvents = touchEvents.filter { it.type == TouchEventType.TAP }
        val scrollEvents = touchEvents.filter { it.type == TouchEventType.SCROLL }
        val swipeEvents = touchEvents.filter { it.type == TouchEventType.SWIPE }
        val longPressEvents = touchEvents.filter { it.type == TouchEventType.LONG_PRESS }
        
        // Calculate behavioral metrics
        val avgTapPressure = tapEvents.map { it.pressure }.average().toFloat()
        val avgTapSize = tapEvents.map { it.size }.average().toFloat()
        val avgLongPressDuration = longPressEvents.map { it.duration }.average().toFloat()
        
        // Calculate timing patterns
        val tapIntervals = if (tapEvents.size > 1) {
            tapEvents.zipWithNext { a, b -> b.timestamp - a.timestamp }.map { it.toFloat() }
        } else emptyList()
        val avgTapInterval = if (tapIntervals.isNotEmpty()) tapIntervals.average().toFloat() else 0f
        
        // Calculate scroll patterns
        val scrollSpeeds = scrollEvents.map { 
            kotlin.math.abs(it.y) / 100f // Normalize scroll distance
        }
        val avgScrollSpeed = if (scrollSpeeds.isNotEmpty()) scrollSpeeds.average().toFloat() else 0f
        
        // Create feature vector (simplified version)
        val features = FloatArray(34) { 0f }
        
        // Basic touch metrics (indices 0-9)
        features[0] = avgTapPressure
        features[1] = avgTapSize
        features[2] = avgTapInterval
        features[3] = avgLongPressDuration
        features[4] = avgScrollSpeed
        features[5] = tapEvents.size.toFloat()
        features[6] = scrollEvents.size.toFloat()
        features[7] = swipeEvents.size.toFloat()
        features[8] = longPressEvents.size.toFloat()
        features[9] = touchEvents.size.toFloat()
        
        // Spatial patterns (indices 10-19)
        val tapPositions = tapEvents.map { Pair(it.x, it.y) }
        if (tapPositions.isNotEmpty()) {
            features[10] = tapPositions.map { it.first }.average().toFloat() // avg X
            features[11] = tapPositions.map { it.second }.average().toFloat() // avg Y
            
            // Calculate variance in tap positions
            val avgX = features[10]
            val avgY = features[11]
            features[12] = tapPositions.map { (it.first - avgX) * (it.first - avgX) }.average().toFloat() // var X
            features[13] = tapPositions.map { (it.second - avgY) * (it.second - avgY) }.average().toFloat() // var Y
        }
        
        // Temporal patterns (indices 20-29)
        if (tapIntervals.isNotEmpty()) {
            features[20] = tapIntervals.minOrNull() ?: 0f
            features[21] = tapIntervals.maxOrNull() ?: 0f
            val avgInterval = features[2]
            features[22] = tapIntervals.map { (it - avgInterval) * (it - avgInterval) }.average().toFloat() // variance
        }
        
        // Gesture complexity (indices 30-33)
        features[30] = getEventCountByType().size.toFloat() // Number of different gesture types
        val maxTimestamp = touchEvents.maxOfOrNull { it.timestamp } ?: 0L
        val minTimestamp = touchEvents.minOfOrNull { it.timestamp } ?: 0L
        features[31] = (maxTimestamp - minTimestamp).toFloat() // Total session duration
        features[32] = touchEvents.size.toFloat() / ((features[31] / 1000f).coerceAtLeast(1f)) // Events per second
        features[33] = 1.0f // Confidence score
        
        return BehavioralFeatureVector(features)
    }
    
    /**
     * Save collected touch data to database and file
     */
    suspend fun saveCollectedData() {
        try {
            // Generate behavioral features
            val behavioralFeatures = generateBehavioralFeatures()
            
            if (behavioralFeatures != null) {
                // Save to database (you'll need to create appropriate DAO methods)
                Log.i(TAG, "Saving behavioral features to database")
                
                // Also save raw data to file for debugging
                saveRawDataToFile()
                
                Log.i(TAG, "Touch data collection completed. Saved ${touchEvents.size} events")
            } else {
                Log.w(TAG, "Could not generate behavioral features from collected data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving touch data", e)
        }
    }
    
    /**
     * Save raw touch data to file for analysis
     */
    private fun saveRawDataToFile() {
        try {
            val touchDataDir = File(context.filesDir, "touch_data")
            if (!touchDataDir.exists()) {
                touchDataDir.mkdirs()
            }
            
            val fileName = "onboarding_touch_data_${System.currentTimeMillis()}.csv"
            val file = File(touchDataDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.write("type,x,y,timestamp,pressure,size,duration,metadata\n")
                
                // Write events
                touchEvents.forEach { event ->
                    writer.write("${event.type},${event.x},${event.y},${event.timestamp},${event.pressure},${event.size},${event.duration},${event.metadata}\n")
                }
            }
            
            Log.d(TAG, "Raw touch data saved to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error saving raw touch data to file", e)
        }
    }
    
    /**
     * Clear collected data
     */
    fun clearData() {
        touchEvents.clear()
        Log.d(TAG, "Touch data cleared")
    }
    
    /**
     * Get summary statistics
     */
    fun getSummaryStats(): String {
        val eventCounts = getEventCountByType()
        return buildString {
            appendLine("Touch Data Collection Summary:")
            appendLine("Total Events: ${touchEvents.size}")
            eventCounts.forEach { (type, count) ->
                appendLine("$type: $count")
            }
            appendLine("Ready for Training: ${hasEnoughDataForTraining()}")
        }
    }
}
