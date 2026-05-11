package com.example.bankingapp.services

import android.content.Context
import android.util.Log
import com.example.bankingapp.behavioral.TouchDataManager
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.room.TouchDataDao
import com.example.bankingapp.ml.MLModelManager
import com.example.bankingapp.ml.SecurityResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Real-Time Behavioral Authentication Service for ABHED
 * 
 * Provides continuous, passive authentication by monitoring user behavior
 * and calculating real-time confidence scores. Triggers security responses
 * when confidence falls below defined thresholds.
 * 
 * Features:
 * - Continuous behavioral monitoring
 * - Real-time confidence calculation
 * - Tiered security response system
 * - Session anomaly detection
 * - Adaptive threshold management
 */
class BehavioralAuthenticationService(
    private val context: Context,
    private val touchDataDao: TouchDataDao,
    private val mlModelManager: MLModelManager
) {
    
    companion object {
        private const val TAG = "BehavioralAuthService"
        private const val CONFIDENCE_UPDATE_INTERVAL = 2000L // 2 seconds
        private const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes
        private const val MIN_DATA_POINTS = 5 // Minimum data points for confidence calculation
    }
    
    // Service state
    private var isMonitoring = false
    private var currentUserId = ""
    private var currentDeviceUUID = ""
    private var sessionStartTime = 0L
    private var lastActivityTime = 0L
    
    // Behavioral data
    private val touchDataManager = TouchDataManager(context, touchDataDao)
    private val recentTouchData = mutableListOf<TouchDataPoint>()
    private val confidenceHistory = mutableListOf<ConfidenceScore>()
    
    // Coroutine scope for background monitoring
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    
    // Confidence flow for UI updates
    private val _confidenceFlow = MutableStateFlow(ConfidenceScore())
    val confidenceFlow: StateFlow<ConfidenceScore> = _confidenceFlow.asStateFlow()
    
    // Security event flow
    private val _securityEventFlow = MutableSharedFlow<SecurityEvent>()
    val securityEventFlow: SharedFlow<SecurityEvent> = _securityEventFlow.asSharedFlow()
    
    /**
     * Start continuous behavioral monitoring
     */
    fun startMonitoring(userId: String, deviceUUID: String) {
        if (isMonitoring) {
            Log.w(TAG, "Monitoring already active")
            return
        }
        
        currentUserId = userId
        currentDeviceUUID = deviceUUID
        sessionStartTime = System.currentTimeMillis()
        lastActivityTime = sessionStartTime
        isMonitoring = true
        
        // Start touch data collection
        touchDataManager.startCollection(userId, deviceUUID)
        
        // Start background monitoring
        startBackgroundMonitoring()
        
        Log.d(TAG, "Started behavioral monitoring for user: $userId")
        
        // Emit initial confidence
        _confidenceFlow.value = ConfidenceScore(
            overall = 0.8f,
            status = AuthenticationStatus.LEARNING,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Stop behavioral monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        monitoringJob?.cancel()
        touchDataManager.stopCollection()
        
        Log.d(TAG, "Stopped behavioral monitoring")
        
        // Emit final confidence
        _confidenceFlow.value = ConfidenceScore(
            overall = 0.0f,
            status = AuthenticationStatus.INACTIVE,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Record user activity to update last activity time
     */
    fun recordActivity() {
        lastActivityTime = System.currentTimeMillis()
    }
    
    /**
     * Record touch event for behavioral analysis
     */
    fun recordTouchEvent(touchData: TouchDataPoint) {
        if (!isMonitoring) return
        
        // Add to recent data buffer
        recentTouchData.add(touchData)
        
        // Keep only recent data (sliding window)
        if (recentTouchData.size > 50) {
            recentTouchData.removeAt(0)
        }
        
        // Record activity
        recordActivity()
        
        // Pass to touch data manager
        touchDataManager.recordTouchEvent(
            x = touchData.x,
            y = touchData.y,
            pressure = touchData.pressure,
            type = touchData.type,
            additionalData = touchData.additionalData
        )
    }
    
    /**
     * Record navigation event
     */
    fun recordNavigation(screenName: String, action: String) {
        if (!isMonitoring) return
        
        touchDataManager.recordNavigation(screenName, action)
        recordActivity()
    }
    
    /**
     * Get current authentication status
     */
    fun getCurrentStatus(): AuthenticationStatus {
        return _confidenceFlow.value.status
    }
    
    /**
     * Get current confidence score
     */
    fun getCurrentConfidence(): Float {
        return _confidenceFlow.value.overall
    }
    
    /**
     * Force confidence recalculation
     */
    suspend fun recalculateConfidence(): Float {
        if (!isMonitoring) return 0.0f
        
        return calculateCurrentConfidence()
    }
    
    // Private methods
    
    private fun startBackgroundMonitoring() {
        monitoringJob = serviceScope.launch {
            while (isMonitoring) {
                try {
                    // Check session timeout
                    if (isSessionExpired()) {
                        handleSessionTimeout()
                        break
                    }
                    
                    // Calculate current confidence
                    val confidence = calculateCurrentConfidence()
                    
                    // Update confidence history
                    updateConfidenceHistory(confidence)
                    
                    // Determine authentication status
                    val status = determineAuthenticationStatus(confidence)
                    
                    // Create confidence score
                    val confidenceScore = ConfidenceScore(
                        overall = confidence,
                        touch = getModelConfidence("touch"),
                        navigation = getModelConfidence("navigation"),
                        geolocation = getModelConfidence("geolocation"),
                        siamese = getModelConfidence("siamese"),
                        temporal = getModelConfidence("temporal"),
                        status = status,
                        timestamp = System.currentTimeMillis(),
                        sessionDuration = System.currentTimeMillis() - sessionStartTime
                    )
                    
                    // Emit confidence update
                    _confidenceFlow.value = confidenceScore
                    
                    // Handle security response if needed
                    handleSecurityResponse(confidence, status)
                    
                    // Wait before next update
                    delay(CONFIDENCE_UPDATE_INTERVAL)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background monitoring: ${e.message}")
                    delay(CONFIDENCE_UPDATE_INTERVAL)
                }
            }
        }
    }
    
    private suspend fun calculateCurrentConfidence(): Float {
        if (recentTouchData.size < MIN_DATA_POINTS) {
            return 0.6f // Default confidence during learning phase
        }
        
        return try {
            mlModelManager.getAuthenticationConfidence(
                userId = currentUserId,
                deviceUUID = currentDeviceUUID,
                recentTouchData = recentTouchData.takeLast(20)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating confidence: ${e.message}")
            0.5f
        }
    }
    
    private fun getModelConfidence(modelType: String): Float {
        // This would get individual model confidences
        // For now, return simulated values
        return when (modelType) {
            "touch" -> 0.75f
            "navigation" -> 0.80f
            "geolocation" -> 0.70f
            "siamese" -> 0.65f
            "temporal" -> 0.72f
            else -> 0.5f
        }
    }
    
    private fun updateConfidenceHistory(confidence: Float) {
        val confidencePoint = ConfidenceScore(
            overall = confidence,
            timestamp = System.currentTimeMillis()
        )
        
        confidenceHistory.add(confidencePoint)
        
        // Keep only recent history (last 100 points)
        if (confidenceHistory.size > 100) {
            confidenceHistory.removeAt(0)
        }
    }
    
    private fun determineAuthenticationStatus(confidence: Float): AuthenticationStatus {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        
        return when {
            sessionDuration < 60000L -> AuthenticationStatus.LEARNING // First minute
            confidence >= 0.8f -> AuthenticationStatus.AUTHENTICATED
            confidence >= 0.65f -> AuthenticationStatus.MONITORING
            confidence >= 0.5f -> AuthenticationStatus.SUSPICIOUS
            else -> AuthenticationStatus.THREAT_DETECTED
        }
    }
    
    private suspend fun handleSecurityResponse(confidence: Float, status: AuthenticationStatus) {
        val securityResponse = mlModelManager.getSecurityResponse(confidence)
        
        when (securityResponse) {
            SecurityResponse.IMMEDIATE_LOCK -> {
                val event = SecurityEvent(
                    type = SecurityEventType.IMMEDIATE_LOCK,
                    confidence = confidence,
                    message = "Suspicious behavior detected. Session locked for security.",
                    timestamp = System.currentTimeMillis(),
                    requiresAction = true
                )
                _securityEventFlow.emit(event)
                Log.w(TAG, "SECURITY: Immediate lock triggered (confidence: $confidence)")
            }
            
            SecurityResponse.RESTRICT_FEATURES -> {
                val event = SecurityEvent(
                    type = SecurityEventType.RESTRICT_FEATURES,
                    confidence = confidence,
                    message = "Some features restricted. Please verify your identity.",
                    timestamp = System.currentTimeMillis(),
                    requiresAction = true
                )
                _securityEventFlow.emit(event)
                Log.w(TAG, "SECURITY: Features restricted (confidence: $confidence)")
            }
            
            SecurityResponse.ALERT_USER -> {
                val event = SecurityEvent(
                    type = SecurityEventType.ALERT_USER,
                    confidence = confidence,
                    message = "Unusual activity detected. Please confirm it's you.",
                    timestamp = System.currentTimeMillis(),
                    requiresAction = false
                )
                _securityEventFlow.emit(event)
                Log.i(TAG, "SECURITY: User alert (confidence: $confidence)")
            }
            
            SecurityResponse.NORMAL_OPERATION -> {
                // No action needed
            }
        }
    }
    
    private fun isSessionExpired(): Boolean {
        val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceLastActivity > SESSION_TIMEOUT
    }
    
    private suspend fun handleSessionTimeout() {
        val event = SecurityEvent(
            type = SecurityEventType.SESSION_TIMEOUT,
            confidence = 0.0f,
            message = "Session expired due to inactivity.",
            timestamp = System.currentTimeMillis(),
            requiresAction = true
        )
        _securityEventFlow.emit(event)
        stopMonitoring()
    }
    
    /**
     * Get behavioral analytics summary
     */
    fun getBehavioralSummary(): BehavioralSummary {
        val sessionDuration = if (isMonitoring) {
            System.currentTimeMillis() - sessionStartTime
        } else 0L
        
        val avgConfidence = if (confidenceHistory.isNotEmpty()) {
            confidenceHistory.map { it.overall }.average().toFloat()
        } else 0f
        
        val touchEvents = recentTouchData.size
        val navigationEvents = recentTouchData.count { it.type.name.contains("NAVIGATION") }
        
        return BehavioralSummary(
            isActive = isMonitoring,
            sessionDuration = sessionDuration,
            averageConfidence = avgConfidence,
            currentConfidence = getCurrentConfidence(),
            touchEvents = touchEvents,
            navigationEvents = navigationEvents,
            securityEvents = 0 // Would track security events
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopMonitoring()
        serviceScope.cancel()
    }
}

// Data classes

data class ConfidenceScore(
    val overall: Float = 0f,
    val touch: Float = 0f,
    val navigation: Float = 0f,
    val geolocation: Float = 0f,
    val siamese: Float = 0f,
    val temporal: Float = 0f,
    val status: AuthenticationStatus = AuthenticationStatus.INACTIVE,
    val timestamp: Long = 0L,
    val sessionDuration: Long = 0L
)

data class SecurityEvent(
    val type: SecurityEventType,
    val confidence: Float,
    val message: String,
    val timestamp: Long,
    val requiresAction: Boolean
)

data class BehavioralSummary(
    val isActive: Boolean,
    val sessionDuration: Long,
    val averageConfidence: Float,
    val currentConfidence: Float,
    val touchEvents: Int,
    val navigationEvents: Int,
    val securityEvents: Int
)

enum class AuthenticationStatus {
    INACTIVE,
    LEARNING,
    AUTHENTICATED,
    MONITORING,
    SUSPICIOUS,
    THREAT_DETECTED
}

enum class SecurityEventType {
    NORMAL_OPERATION,
    ALERT_USER,
    RESTRICT_FEATURES,
    IMMEDIATE_LOCK,
    SESSION_TIMEOUT,
    ANOMALY_DETECTED
}
