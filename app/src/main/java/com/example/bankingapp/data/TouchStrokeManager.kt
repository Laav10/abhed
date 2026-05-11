package com.example.bankingapp.data

import android.content.Context
import android.util.Log
import com.example.bankingapp.network.MLModelService
import com.example.bankingapp.data.models.TouchType
import kotlinx.coroutines.*
import java.util.*

data class TouchStroke(
    val id: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val pressure: Float,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val velocity: Float,
    val type: TouchType,
    val sessionId: String,
    val userId: String?,
    val deviceUUID: String,
    val additionalData: Map<String, String>
)

class TouchStrokeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TouchStrokeManager"
        private const val STROKE_TIMEOUT_MS = 1000L // 1 second timeout for stroke completion
        private const val MIN_STROKE_DISTANCE = 2.0f // Minimum distance to be considered a stroke
    }
    
    private val mlModelService = MLModelService(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Active stroke tracking
    private var activeStroke: TouchStroke? = null
    private var strokeStartTime: Long = 0L
    private var strokeStartX: Float = 0f
    private var strokeStartY: Float = 0f
    private var strokeStartPressure: Float = 0f
    
    // Stroke statistics
    private var totalStrokes = 0
    private var totalPredictions = 0
    private var averageConfidence = 0f
    
    /**
     * Start a new touch stroke
     */
    fun startStroke(
        x: Float,
        y: Float,
        pressure: Float,
        type: TouchType,
        sessionId: String,
        userId: String?,
        deviceUUID: String,
        additionalData: Map<String, String>
    ) {
        // Complete previous stroke if exists
        activeStroke?.let { completeStroke(endX = x, endY = y, endPressure = pressure) }
        
        // Start new stroke
        strokeStartTime = System.currentTimeMillis()
        strokeStartX = x
        strokeStartY = y
        strokeStartPressure = pressure
        
        Log.d(TAG, "🎯 STROKE STARTED: ($x, $y) | Type: $type | Pressure: $pressure")
        Log.d(TAG, "📱 Context: ${additionalData["element"]} | State: ${additionalData["onboarding_state"]}")
        
        // Schedule stroke timeout
        scope.launch {
            delay(STROKE_TIMEOUT_MS)
            activeStroke?.let { 
                Log.w(TAG, "⏰ STROKE TIMEOUT: Auto-completing stroke after ${STROKE_TIMEOUT_MS}ms")
                completeStroke(endX = x, endY = y, endPressure = pressure)
            }
        }
    }
    
    /**
     * Update stroke during movement
     */
    fun updateStroke(x: Float, y: Float, pressure: Float) {
        activeStroke?.let { stroke ->
            Log.d(TAG, "🔄 STROKE UPDATE: ($x, $y) | Pressure: $pressure | Duration: ${System.currentTimeMillis() - strokeStartTime}ms")
        }
    }
    
    /**
     * Complete the current stroke
     */
    fun completeStroke(endX: Float, endY: Float, endPressure: Float) {
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - strokeStartTime
        
        // Calculate velocity
        val distance = calculateDistance(strokeStartX, strokeStartY, endX, endY)
        val velocity = if (duration > 0) distance / duration else 0f
        
        val stroke = TouchStroke(
            id = "stroke_${System.currentTimeMillis()}_${Random().nextInt(1000)}",
            startX = strokeStartX,
            startY = strokeStartY,
            endX = endX,
            endY = endY,
            pressure = strokeStartPressure,
            startTime = strokeStartTime,
            endTime = currentTime,
            duration = duration,
            velocity = velocity,
            type = TouchType.SWIPE, // Default to SWIPE for strokes
            sessionId = "session_${System.currentTimeMillis()}",
            userId = null, // Will be set when device activates
            deviceUUID = "device_${System.currentTimeMillis()}",
            additionalData = mapOf(
                "stroke_number" to (totalStrokes + 1).toString(),
                "stroke_duration" to duration.toString(),
                "stroke_distance" to distance.toString(),
                "stroke_velocity" to velocity.toString()
            )
        )
        
        Log.d(TAG, "✅ STROKE COMPLETED: ID=${stroke.id}")
        Log.d(TAG, "📊 Stroke Stats: Duration=${duration}ms | Distance=${String.format("%.2f", distance)}px | Velocity=${String.format("%.2f", velocity)}px/ms")
        
        // Send to ML model for prediction
        sendStrokeToMLModel(stroke)
        
        // Reset active stroke
        activeStroke = null
        totalStrokes++
    }
    
    /**
     * Handle tap gestures - convert to ML-compatible micro-stroke
     */
    fun handleTap(
        x: Float,
        y: Float,
        pressure: Float,
        sessionId: String,
        userId: String?,
        deviceUUID: String,
        additionalData: Map<String, String>
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Convert tap to micro-stroke for ML compatibility
        // ML model expects at least 2 points with ACTION_DOWN and ACTION_UP
        val microStroke = TouchStroke(
            id = "tap_${currentTime}_${Random().nextInt(1000)}",
            startX = x,
            startY = y,
            endX = x + 0.1f,  // Tiny movement for ML compatibility
            endY = y + 0.1f,  // Tiny movement for ML compatibility
            pressure = pressure,
            startTime = currentTime,
            endTime = currentTime + 50,  // 50ms duration
            duration = 50L,
            velocity = 0.002f,  // Very slow velocity
            type = TouchType.TAP,
            sessionId = sessionId,
            userId = userId,
            deviceUUID = deviceUUID,
            additionalData = additionalData + mapOf(
                "stroke_number" to (totalStrokes + 1).toString(),
                "gesture_type" to "TAP",
                "converted_to_stroke" to "true"
            )
        )
        
        Log.d(TAG, "👆 TAP CONVERTED TO MICRO-STROKE: ($x, $y) | Pressure: $pressure")
        Log.d(TAG, "📱 Context: ${additionalData["element"]} | Action: ${additionalData["action"]}")
        
        // Send to ML model for prediction
        sendStrokeToMLModel(microStroke)
        
        totalStrokes++
    }
    
    /**
     * Handle swipe gestures - already ML compatible
     */
    fun handleSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        pressure: Float,
        duration: Long,
        sessionId: String,
        userId: String?,
        deviceUUID: String,
        additionalData: Map<String, String>
    ) {
        val currentTime = System.currentTimeMillis()
        val distance = calculateDistance(startX, startY, endX, endY)
        val velocity = if (duration > 0) distance / duration else 0f
        
        val swipeStroke = TouchStroke(
            id = "swipe_${currentTime}_${Random().nextInt(1000)}",
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            pressure = pressure,
            startTime = currentTime,
            endTime = currentTime + duration,
            duration = duration,
            velocity = velocity,
            type = TouchType.SWIPE,
            sessionId = sessionId,
            userId = userId,
            deviceUUID = deviceUUID,
            additionalData = additionalData + mapOf(
                "stroke_number" to (totalStrokes + 1).toString(),
                "gesture_type" to "SWIPE",
                "stroke_distance" to distance.toString(),
                "stroke_velocity" to velocity.toString()
            )
        )
        
        Log.d(TAG, "🔄 SWIPE STROKE CREATED: Distance=${String.format("%.2f", distance)}px | Velocity=${String.format("%.2f", velocity)}px/ms")
        
        // Send to ML model for prediction
        sendStrokeToMLModel(swipeStroke)
        
        totalStrokes++
    }
    
    /**
     * Handle scroll gestures - convert to ML-compatible stroke
     */
    fun handleScroll(
        startY: Float,
        endY: Float,
        velocity: Float,
        duration: Long,
        sessionId: String,
        userId: String?,
        deviceUUID: String,
        additionalData: Map<String, String>
    ) {
        val currentTime = System.currentTimeMillis()
        val distance = kotlin.math.abs(endY - startY)
        
        val scrollStroke = TouchStroke(
            id = "scroll_${currentTime}_${Random().nextInt(1000)}",
            startX = 0f,  // Scroll is primarily vertical
            startY = startY,
            endX = 0f,
            endY = endY,
            pressure = 1.0f,  // Scroll typically uses full pressure
            startTime = currentTime,
            endTime = currentTime + duration,
            duration = duration,
            velocity = velocity,
            type = TouchType.SCROLL,
            sessionId = sessionId,
            userId = userId,
            deviceUUID = deviceUUID,
            additionalData = additionalData + mapOf(
                "stroke_number" to (totalStrokes + 1).toString(),
                "gesture_type" to "SCROLL",
                "scroll_distance" to distance.toString(),
                "scroll_velocity" to velocity.toString()
            )
        )
        
        Log.d(TAG, "📜 SCROLL STROKE CREATED: Distance=${String.format("%.2f", distance)}px | Velocity=${String.format("%.2f", velocity)}px/ms")
        
        // Send to ML model for prediction
        sendStrokeToMLModel(scrollStroke)
        
        totalStrokes++
    }
    
    /**
     * Convert TouchStroke to ML-compatible format
     */
    private fun convertToMLFormat(stroke: TouchStroke): MLCompatibleStroke {
        val points = mutableListOf<MLStrokePoint>()
        
        // Add ACTION_DOWN point
        points.add(MLStrokePoint(
            action = 0,  // ACTION_DOWN
            time_ms = stroke.startTime,
            x = stroke.startX,
            y = stroke.startY,
            pressure = stroke.pressure
        ))
        
        // Add ACTION_MOVE point if distance is significant
        val distance = calculateDistance(stroke.startX, stroke.startY, stroke.endX, stroke.endY)
        if (distance > MIN_STROKE_DISTANCE && stroke.duration > 50) {
            val midTime = stroke.startTime + (stroke.duration / 2)
            val midX = (stroke.startX + stroke.endX) / 2
            val midY = (stroke.startY + stroke.endY) / 2
            
            points.add(MLStrokePoint(
                action = 2,  // ACTION_MOVE
                time_ms = midTime,
                x = midX,
                y = midY,
                pressure = stroke.pressure
            ))
        }
        
        // Add ACTION_UP point
        points.add(MLStrokePoint(
            action = 1,  // ACTION_UP
            time_ms = stroke.endTime,
            x = stroke.endX,
            y = stroke.endY,
            pressure = stroke.pressure
        ))
        
        return MLCompatibleStroke(stroke = points)
    }
    
    /**
     * Send stroke to ML model for real-time prediction
     */
    private fun sendStrokeToMLModel(stroke: TouchStroke) {
        scope.launch {
            try {
                Log.d(TAG, "🤖 SENDING TO ML MODEL: Stroke ${stroke.id}")
                
                // Convert to ML-compatible format
                val mlStroke = convertToMLFormat(stroke)
                
                val startTime = System.currentTimeMillis()
                val prediction = mlModelService.predictStroke(mlStroke)
                val processingTime = System.currentTimeMillis() - startTime
                
                // Log prediction results
                Log.d(TAG, "🎯 ML PREDICTION RECEIVED:")
                Log.d(TAG, "   📊 Confidence: ${String.format("%.2f", prediction.confidence)}%")
                Log.d(TAG, "   🎯 Prediction: ${prediction.prediction}")
                Log.d(TAG, "   ⏱️ Processing Time: ${processingTime}ms")
                Log.d(TAG, "   🔄 Model Version: ${prediction.modelVersion}")
                
                // Update statistics
                totalPredictions++
                averageConfidence = ((averageConfidence * (totalPredictions - 1)) + prediction.confidence) / totalPredictions
                
                // Log running statistics
                Log.d(TAG, "📈 RUNNING STATS: Total Strokes: $totalStrokes | Predictions: $totalPredictions | Avg Confidence: ${String.format("%.2f", averageConfidence)}%")
                
                // Store prediction for later sync
                storePrediction(prediction)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ ML MODEL PREDICTION FAILED: ${e.message}", e)
                Log.d(TAG, "🔄 Will retry during device activation sync")
            }
        }
    }
    
    /**
     * Store prediction locally for later sync
     */
    private fun storePrediction(prediction: MLPrediction) {
        // Store in local storage for later backend sync
        // This will be implemented in TouchDataStorage
        Log.d(TAG, "💾 PREDICTION STORED LOCALLY: ${prediction.strokeId}")
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Get stroke statistics
     */
    fun getStrokeStats(): String {
        return """
            📊 TOUCH STROKE STATISTICS:
            🎯 Total Strokes: $totalStrokes
            🤖 Total Predictions: $totalPredictions
            📈 Average Confidence: ${String.format("%.2f", averageConfidence)}%
            ⏱️ Last Stroke: ${if (activeStroke != null) "IN PROGRESS" else "COMPLETED"}
        """.trimIndent()
    }
    
    /**
     * Reset stroke manager (for new sessions)
     */
    fun reset() {
        activeStroke = null
        totalStrokes = 0
        totalPredictions = 0
        averageConfidence = 0f
        Log.d(TAG, "🔄 STROKE MANAGER RESET")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        Log.d(TAG, "🧹 STROKE MANAGER CLEANUP COMPLETED")
    }
}
