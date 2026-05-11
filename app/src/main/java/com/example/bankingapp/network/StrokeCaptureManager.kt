package com.example.bankingapp.network

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class StrokePoint(
    val time_ms: Long,
    val action: Int, // 0 = ACTION_DOWN, 1 = ACTION_UP, 2 = ACTION_MOVE
    val x: Float,
    val y: Float,
    val pressure: Float,
    val area: Float,
    val finger_orientation: Float
)

@Serializable
data class StrokeData(
    val stroke: List<StrokePoint>,
    val dpi_x: Int,
    val dpi_y: Int,
    val phone_orientation: Int,
    val phone_id: Int
)

class StrokeCaptureManager(
    private val context: Context,
    private val serverClient: ABHEDServerClient
) {
    companion object {
        private const val TAG = "StrokeCaptureManager"
        
        // Android MotionEvent actions
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        
        // Device info
        const val DEFAULT_DPI = 400
        const val DEFAULT_PHONE_ID = 1
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Current stroke being captured
    private var currentStroke: MutableList<StrokePoint> = mutableListOf()
    private var isCapturing = false
    
    // State flows for UI updates
    private val _strokeCount = MutableStateFlow(0)
    val strokeCount: StateFlow<Int> = _strokeCount.asStateFlow()
    
    private val _isCapturing = MutableStateFlow(false)
    val isCapturingState: StateFlow<Boolean> = _isCapturing.asStateFlow()
    
    private val _lastSubmissionStatus = MutableStateFlow<String?>(null)
    val lastSubmissionStatus: StateFlow<String?> = _lastSubmissionStatus.asStateFlow()
    
    private val _totalPointsCaptured = MutableStateFlow(0)
    val totalPointsCaptured: StateFlow<Int> = _totalPointsCaptured.asStateFlow()
    
    // Device information
    private val dpiX: Int = context.resources.displayMetrics.densityDpi
    private val dpiY: Int = context.resources.displayMetrics.densityDpi
    private val phoneOrientation: Int = 0 // Portrait by default
    
    /**
     * Start capturing a new stroke
     */
    fun startStroke(x: Float, y: Float, pressure: Float, area: Float, fingerOrientation: Float = 0f) {
        if (isCapturing) {
            Log.w(TAG, "Already capturing a stroke, ignoring start request")
            return
        }
        
        currentStroke.clear()
        val startPoint = StrokePoint(
            time_ms = System.currentTimeMillis(),
            action = ACTION_DOWN,
            x = x,
            y = y,
            pressure = pressure,
            area = area,
            finger_orientation = fingerOrientation
        )
        currentStroke.add(startPoint)
        isCapturing = true
        _isCapturing.value = true
        
        Log.d(TAG, "Started stroke at ($x, $y)")
    }
    
    /**
     * Add a point to the current stroke (for ACTION_MOVE)
     */
    fun addStrokePoint(x: Float, y: Float, pressure: Float, area: Float, fingerOrientation: Float = 0f) {
        if (!isCapturing) {
            Log.w(TAG, "Not capturing a stroke, ignoring point")
            return
        }
        
        val movePoint = StrokePoint(
            time_ms = System.currentTimeMillis(),
            action = ACTION_MOVE,
            x = x,
            y = y,
            pressure = pressure,
            area = area,
            finger_orientation = fingerOrientation
        )
        currentStroke.add(movePoint)
        
        Log.d(TAG, "Added stroke point at ($x, $y)")
    }
    
    /**
     * End the current stroke and submit to server
     */
    fun endStroke(x: Float, y: Float, pressure: Float, area: Float, fingerOrientation: Float = 0f) {
        if (!isCapturing) {
            Log.w(TAG, "Not capturing a stroke, ignoring end request")
            return
        }
        
        val endPoint = StrokePoint(
            time_ms = System.currentTimeMillis(),
            action = ACTION_UP,
            x = x,
            y = y,
            pressure = pressure,
            area = area,
            finger_orientation = fingerOrientation
        )
        currentStroke.add(endPoint)
        
        // Validate stroke
        if (currentStroke.size < 2) {
            Log.w(TAG, "Stroke too short (${currentStroke.size} points), discarding")
            resetStroke()
            return
        }
        
        // Submit stroke to server
        submitStrokeToServer()
        
        // Reset for next stroke
        resetStroke()
    }
    
    /**
     * Cancel current stroke without submitting
     */
    fun cancelStroke() {
        if (isCapturing) {
            Log.d(TAG, "Cancelling stroke")
            resetStroke()
        }
    }
    
    /**
     * Reset stroke state
     */
    private fun resetStroke() {
        currentStroke.clear()
        isCapturing = false
        _isCapturing.value = false
    }
    
    /**
     * Submit the completed stroke to the server
     */
    private fun submitStrokeToServer() {
        if (currentStroke.isEmpty()) {
            Log.w(TAG, "No stroke to submit")
            return
        }
        
        val strokeData = StrokeData(
            stroke = currentStroke.toList(),
            dpi_x = dpiX,
            dpi_y = dpiY,
            phone_orientation = phoneOrientation,
            phone_id = DEFAULT_PHONE_ID
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = serverClient.submitRawStroke(strokeData)
                when (result) {
                    is StrokeResult.Success -> {
                        Log.d(TAG, "Stroke submitted successfully")
                        _strokeCount.value = _strokeCount.value + 1
                        _totalPointsCaptured.value = _totalPointsCaptured.value + currentStroke.size
                        _lastSubmissionStatus.value = "Stroke submitted successfully"
                    }
                    is StrokeResult.Error -> {
                        Log.e(TAG, "Failed to submit stroke: ${result.message}")
                        _lastSubmissionStatus.value = "Error: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception submitting stroke", e)
                _lastSubmissionStatus.value = "Exception: ${e.message}"
            }
        }
    }
    
    /**
     * Get current stroke statistics
     */
    fun getStrokeStats(): String {
        return "Strokes: ${_strokeCount.value}, Points: ${_totalPointsCaptured.value}"
    }
    
    /**
     * Clear all statistics
     */
    fun clearStats() {
        _strokeCount.value = 0
        _totalPointsCaptured.value = 0
        _lastSubmissionStatus.value = null
    }
}
