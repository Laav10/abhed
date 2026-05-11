package com.example.bankingapp.network

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Touch Data Manager for ABHED Server Integration
 * 
 * Manages touch data collection, conversion, and submission to the ABHED server.
 * Provides real-time confidence scoring and authentication status.
 */
class TouchDataManager(
    private val context: Context,
    private val serverUrl: String = "http://10.0.2.2:3343"
) : ViewModel() {
    
    private val serverClient = ABHEDServerClient(serverUrl)
    private val touchDataConverter = TouchDataConverter
    
    // State flows for UI observation
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _confidenceScore = MutableStateFlow(0.0)
    val confidenceScore: StateFlow<Double> = _confidenceScore.asStateFlow()
    
    private val _isCollectingData = MutableStateFlow(true)
    val isCollectingData: StateFlow<Boolean> = _isCollectingData.asStateFlow()
    
    private val _dataCollectionProgress = MutableStateFlow(0 to 100)
    val dataCollectionProgress: StateFlow<Pair<Int, Int>> = _dataCollectionProgress.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Touch data collection state
    private var currentStroke = mutableListOf<TouchDataPoint>()
    private var strokeStartTime: Long = 0L
    private var isStrokeInProgress = false
    
    companion object {
        private const val TAG = "TouchDataManager"
        private const val CONFIDENCE_CHECK_INTERVAL = 5000L // 5 seconds
        private const val MIN_STROKES_FOR_TRAINING = 100
    }
    
    /**
     * Initialize the touch data manager and login user
     * @param userId User identifier
     * @param debitCard Optional debit card number
     */
    fun initialize(userId: String, debitCard: String? = null) {
        viewModelScope.launch {
            try {
                _lastError.value = null
                val loginResult = serverClient.loginUser(userId, debitCard)
                
                when (loginResult) {
                    is LoginResult.Success -> {
                        _isAuthenticated.value = true
                        _isConnected.value = true
                        Log.d(TAG, "Successfully authenticated user: $userId")
                        
                        // Start periodic confidence checking
                        startConfidenceMonitoring()
                    }
                    is LoginResult.Error -> {
                        _lastError.value = "Login failed: ${loginResult.message}"
                        _isAuthenticated.value = false
                        _isConnected.value = false
                        Log.e(TAG, "Login failed: ${loginResult.message}")
                    }
                }
            } catch (e: Exception) {
                _lastError.value = "Initialization error: ${e.message}"
                _isAuthenticated.value = false
                _isConnected.value = false
                Log.e(TAG, "Initialization error", e)
            }
        }
    }
    
    /**
     * Start a new touch stroke
     * @param x Initial X coordinate
     * @param y Initial Y coordinate
     * @param pressure Initial pressure
     */
    fun startStroke(x: Float, y: Float, pressure: Float = 0.5f) {
        if (!_isAuthenticated.value) return
        
        currentStroke.clear()
        strokeStartTime = System.currentTimeMillis()
        isStrokeInProgress = true
        
        val startPoint = TouchDataPoint(
            x = x,
            y = y,
            pressure = pressure,
            timestamp = strokeStartTime,
            type = TouchType.TAP
        )
        currentStroke.add(startPoint)
        
        Log.d(TAG, "Started stroke at ($x, $y)")
    }
    
    /**
     * Add a touch point to the current stroke
     * @param x X coordinate
     * @param y Y coordinate
     * @param pressure Pressure value
     * @param type Touch type
     */
    fun addTouchPoint(x: Float, y: Float, pressure: Float = 0.5f, type: TouchType = TouchType.TAP) {
        if (!_isAuthenticated.value || !isStrokeInProgress) return
        
        val touchPoint = TouchDataPoint(
            x = x,
            y = y,
            pressure = pressure,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        currentStroke.add(touchPoint)
    }
    
    /**
     * End the current stroke and submit to server
     * @param x Final X coordinate
     * @param y Final Y coordinate
     * @param pressure Final pressure
     * @param type Final touch type
     */
    fun endStroke(x: Float, y: Float, pressure: Float = 0.5f, type: TouchType = TouchType.TAP) {
        if (!_isAuthenticated.value || !isStrokeInProgress) return
        
        val endPoint = TouchDataPoint(
            x = x,
            y = y,
            pressure = pressure,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        currentStroke.add(endPoint)
        
        // Submit stroke to server
        submitStroke()
        
        isStrokeInProgress = false
        Log.d(TAG, "Ended stroke with ${currentStroke.size} points")
    }
    
    /**
     * Submit a single touch point as a tap gesture
     * @param touchPoint TouchDataPoint to submit
     */
    fun submitTouchPoint(touchPoint: TouchDataPoint) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                val stroke = touchDataConverter.convertSingleTouchToStroke(touchPoint)
                val result = serverClient.submitStroke(stroke)
                
                when (result) {
                    is StrokeResult.Success -> {
                        Log.d(TAG, "Touch point submitted successfully")
                        // Update confidence score
                        checkConfidence()
                    }
                    is StrokeResult.Error -> {
                        _lastError.value = "Submission failed: ${result.message}"
                        Log.e(TAG, "Touch point submission failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _lastError.value = "Submission error: ${e.message}"
                Log.e(TAG, "Touch point submission error", e)
            }
        }
    }
    
    /**
     * Submit a gesture (swipe, long press, etc.) to the server
     * @param gestureType Type of gesture
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param pressure Pressure value
     */
    fun submitGesture(
        gestureType: TouchType,
        startX: Float,
        startY: Float,
        endX: Float = startX,
        endY: Float = startY,
        pressure: Float = 0.5f
    ) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                val stroke = touchDataConverter.createStrokeFromGesture(
                    gestureType, startX, startY, endX, endY, pressure
                )
                val result = serverClient.submitStroke(stroke)
                
                when (result) {
                    is StrokeResult.Success -> {
                        Log.d(TAG, "Gesture submitted successfully: $gestureType")
                        // Update confidence score
                        checkConfidence()
                    }
                    is StrokeResult.Error -> {
                        _lastError.value = "Gesture submission failed: ${result.message}"
                        Log.e(TAG, "Gesture submission failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _lastError.value = "Gesture submission error: ${e.message}"
                Log.e(TAG, "Gesture submission error", e)
            }
        }
    }
    
    /**
     * Submit the current stroke to the server
     */
    private fun submitStroke() {
        if (currentStroke.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val stroke = touchDataConverter.convertTouchDataPointsToStroke(currentStroke, strokeStartTime)
                val result = serverClient.submitStroke(stroke)
                
                when (result) {
                    is StrokeResult.Success -> {
                        Log.d(TAG, "Stroke submitted successfully with ${stroke.size} points")
                        // Update confidence score
                        checkConfidence()
                    }
                    is StrokeResult.Error -> {
                        _lastError.value = "Stroke submission failed: ${result.message}"
                        Log.e(TAG, "Stroke submission failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _lastError.value = "Stroke submission error: ${e.message}"
                Log.e(TAG, "Stroke submission error", e)
            }
        }
    }
    
    /**
     * Check confidence score from server
     */
    fun checkConfidence() {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                val result = serverClient.getConfidence()
                
                when (result) {
                    is ConfidenceResult.Success -> {
                        _confidenceScore.value = result.confidence
                        _isCollectingData.value = false
                        Log.d(TAG, "Confidence score: ${result.confidence}")
                    }
                    is ConfidenceResult.CollectingData -> {
                        _isCollectingData.value = true
                        _dataCollectionProgress.value = result.have to result.need
                        Log.d(TAG, "Collecting data: ${result.have}/${result.need}")
                    }
                    is ConfidenceResult.Error -> {
                        _lastError.value = "Confidence check failed: ${result.message}"
                        Log.e(TAG, "Confidence check failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _lastError.value = "Confidence check error: ${e.message}"
                Log.e(TAG, "Confidence check error", e)
            }
        }
    }
    
    /**
     * Start periodic confidence monitoring
     */
    private fun startConfidenceMonitoring() {
        viewModelScope.launch {
            while (_isAuthenticated.value) {
                checkConfidence()
                delay(CONFIDENCE_CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * Get current authentication status
     */
    fun getAuthenticationStatus(): AuthenticationStatus {
        return when {
            !_isConnected.value -> AuthenticationStatus.Disconnected
            !_isAuthenticated.value -> AuthenticationStatus.NotAuthenticated
            _isCollectingData.value -> AuthenticationStatus.CollectingData
            _confidenceScore.value >= 0.75 -> AuthenticationStatus.Authenticated
            _confidenceScore.value >= 0.5 -> AuthenticationStatus.LowConfidence
            else -> AuthenticationStatus.Suspicious
        }
    }
    
    /**
     * Logout user and cleanup
     */
    fun logout() {
        viewModelScope.launch {
            try {
                val result = serverClient.logoutUser()
                when (result) {
                    is LogoutResult.Success -> {
                        Log.d(TAG, "Logout successful")
                    }
                    is LogoutResult.Error -> {
                        Log.e(TAG, "Logout failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logout error", e)
            } finally {
                _isAuthenticated.value = false
                _isConnected.value = false
                _confidenceScore.value = 0.0
                _isCollectingData.value = true
                _dataCollectionProgress.value = 0 to 100
                _lastError.value = null
            }
        }
    }
    
    /**
     * Clear last error
     */
    fun clearError() {
        _lastError.value = null
    }
}

/**
 * Authentication status enum
 */
enum class AuthenticationStatus {
    Disconnected,
    NotAuthenticated,
    CollectingData,
    LowConfidence,
    Authenticated,
    Suspicious
}
