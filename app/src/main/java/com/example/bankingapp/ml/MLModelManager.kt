package com.example.bankingapp.ml

import android.content.Context
import android.util.Log
import com.example.bankingapp.behavioral.BehavioralProfile
import com.example.bankingapp.behavioral.TouchPatternAnalysis
import com.example.bankingapp.behavioral.NavigationPatternAnalysis
import com.example.bankingapp.behavioral.TypingPatternAnalysis
import com.example.bankingapp.behavioral.ScrollPatternAnalysis
import com.example.bankingapp.behavioral.PressureProfile
import com.example.bankingapp.behavioral.VelocityProfile
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.room.TouchDataDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.*

/**
 * ABHED ML Model Manager
 * 
 * Coordinates all behavioral authentication models:
 * 1. Touch & Swipe Behavior Model (RBF-SVM + kNN)
 * 2. Markov Chain Navigation Model
 * 3. Geolocation Weight Adjustment Model
 * 4. Siamese Neural Network
 * 5. Temporal Convolutional Network
 */
class MLModelManager(
    private val context: Context,
    private val touchDataDao: TouchDataDao
) {
    
    companion object {
        private const val TAG = "MLModelManager"
        private const val MIN_TRAINING_DATA = 100 // Minimum data points for training
        private const val CONFIDENCE_THRESHOLD_LOW = 0.5f
        private const val CONFIDENCE_THRESHOLD_MEDIUM = 0.65f
        private const val CONFIDENCE_THRESHOLD_HIGH = 0.8f
    }
    
    // Model instances
    private var touchSwipeModel: TouchSwipeBehaviorModel? = null
    private var navigationModel: NavigationMarkovModel? = null
    private var geolocationModel: GeolocationWeightModel? = null
    private var siameseModel: SiameseBehaviorModel? = null
    private var temporalModel: TemporalConvolutionModel? = null
    
    // Model states
    private var isModelsTrained = false
    private var currentUserProfile: BehavioralProfile? = null
    
    /**
     * Initialize and train all models for a user
     */
    suspend fun initializeModels(userId: String, deviceUUID: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing ML models for user: $userId")
                
                // Check if we have enough training data
                val trainingDataCount = touchDataDao.getUserTouchDataCount(userId)
                if (trainingDataCount < MIN_TRAINING_DATA) {
                    Log.w(TAG, "Insufficient training data: $trainingDataCount < $MIN_TRAINING_DATA")
                    return@withContext false
                }
                
                // Train models one by one
                val touchModelTrained = trainTouchSwipeModel(userId, deviceUUID)
                val navModelTrained = trainNavigationModel(userId, deviceUUID)
                val geoModelTrained = trainGeolocationModel(userId, deviceUUID)
                val siameseModelTrained = trainSiameseModel(userId, deviceUUID)
                val temporalModelTrained = trainTemporalModel(userId, deviceUUID)
                
                // All models must be trained successfully
                isModelsTrained = touchModelTrained && navModelTrained && 
                                geoModelTrained && siameseModelTrained && temporalModelTrained
                
                if (isModelsTrained) {
                    Log.d(TAG, "All ML models trained successfully for user: $userId")
                    // Build user behavioral profile
                    currentUserProfile = buildUserProfile(userId)
                } else {
                    Log.e(TAG, "Failed to train all models for user: $userId")
                }
                
                isModelsTrained
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing models: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get authentication confidence score for current session
     */
    suspend fun getAuthenticationConfidence(
        userId: String,
        deviceUUID: String,
        recentTouchData: List<TouchDataPoint>
    ): Float {
        return withContext(Dispatchers.IO) {
            if (!isModelsTrained) {
                Log.w(TAG, "Models not trained yet, returning low confidence")
                return@withContext 0.1f
            }
            
            try {
                // Get confidence scores from each model
                val touchConfidence = touchSwipeModel?.getConfidence(recentTouchData) ?: 0.5f
                val navConfidence = navigationModel?.getConfidence(userId) ?: 0.5f
                val geoConfidence = geolocationModel?.getConfidence(userId, deviceUUID) ?: 0.5f
                val siameseConfidence = siameseModel?.getConfidence(recentTouchData) ?: 0.5f
                val temporalConfidence = temporalModel?.getConfidence(recentTouchData) ?: 0.5f
                
                // Calculate weighted confidence score
                val confidenceScore = calculateWeightedConfidence(
                    touchConfidence, navConfidence, geoConfidence, 
                    siameseConfidence, temporalConfidence, userId
                )
                
                Log.d(TAG, "Authentication confidence: $confidenceScore")
                confidenceScore
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating confidence: ${e.message}")
                0.1f
            }
        }
    }
    
    /**
     * Get security response based on confidence level
     */
    fun getSecurityResponse(confidence: Float): SecurityResponse {
        return when {
            confidence < CONFIDENCE_THRESHOLD_LOW -> {
                SecurityResponse.IMMEDIATE_LOCK
            }
            confidence < CONFIDENCE_THRESHOLD_MEDIUM -> {
                SecurityResponse.RESTRICT_FEATURES
            }
            confidence < CONFIDENCE_THRESHOLD_HIGH -> {
                SecurityResponse.ALERT_USER
            }
            else -> {
                SecurityResponse.NORMAL_OPERATION
            }
        }
    }
    
    /**
     * Update models with new behavioral data
     */
    suspend fun updateModels(userId: String, newData: List<TouchDataPoint>) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating models with ${newData.size} new data points")
                
                // Update each model incrementally
                touchSwipeModel?.updateModel(newData)
                navigationModel?.updateModel("screen_transition")
                geolocationModel?.updateModel(0.0, 0.0)
                siameseModel?.updateModel(newData)
                temporalModel?.updateModel(newData)
                
                // Update user profile
                currentUserProfile = buildUserProfile(userId)
                
                Log.d(TAG, "Models updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating models: ${e.message}")
            }
        }
    }
    
    /**
     * Get model training status
     */
    fun getModelStatus(): ModelStatus {
        return ModelStatus(
            isTrained = isModelsTrained,
            touchModelTrained = touchSwipeModel != null,
            navigationModelTrained = navigationModel != null,
            geolocationModelTrained = geolocationModel != null,
            siameseModelTrained = siameseModel != null,
            temporalModelTrained = temporalModel != null
        )
    }
    
    // Private training methods
    
    private suspend fun trainTouchSwipeModel(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Touch & Swipe Behavior Model")
            
            // Get training data
            val trainingData = touchDataDao.getBehavioralFeatures(userId, 0L).first()
            if (trainingData.size < MIN_TRAINING_DATA) {
                Log.w(TAG, "Insufficient touch data for training: ${trainingData.size}")
                return false
            }
            
            // Create and train the model
            touchSwipeModel = TouchSwipeBehaviorModel()
            val success = touchSwipeModel!!.train(trainingData)
            
            if (success) {
                Log.d(TAG, "Touch & Swipe model trained successfully")
            } else {
                Log.e(TAG, "Failed to train Touch & Swipe model")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error training Touch & Swipe model: ${e.message}")
            false
        }
    }
    
    private suspend fun trainNavigationModel(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Navigation Markov Model")
            
            // Get navigation patterns
            val navigationData = touchDataDao.getNavigationPattern(userId, 0L).first()
            if (navigationData.isEmpty()) {
                Log.w(TAG, "No navigation data for training")
                return false
            }
            
            // Create and train the model
            navigationModel = NavigationMarkovModel()
            val success = navigationModel!!.train(userId, deviceUUID)
            
            if (success) {
                Log.d(TAG, "Navigation model trained successfully")
            } else {
                Log.e(TAG, "Failed to train Navigation model")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error training Navigation model: ${e.message}")
            false
        }
    }
    
    private suspend fun trainGeolocationModel(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Geolocation Weight Model")
            
            // Get location data
            val locationData = touchDataDao.getSensorData(userId, 0L).first()
            if (locationData.isEmpty()) {
                Log.w(TAG, "No location data for training")
                return false
            }
            
            // Create and train the model
            geolocationModel = GeolocationWeightModel()
            val success = geolocationModel!!.train(userId, deviceUUID)
            
            if (success) {
                Log.d(TAG, "Geolocation model trained successfully")
            } else {
                Log.e(TAG, "Failed to train Geolocation model")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error training Geolocation model: ${e.message}")
            false
        }
    }
    
    private suspend fun trainSiameseModel(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Siamese Behavioral Model")
            
            // Get behavioral features
            val behavioralData = touchDataDao.getBehavioralFeatures(userId, 0L).first()
            if (behavioralData.size < MIN_TRAINING_DATA) {
                Log.w(TAG, "Insufficient behavioral data for training: ${behavioralData.size}")
                return false
            }
            
            // Create and train the model
            siameseModel = SiameseBehaviorModel()
            val success = siameseModel!!.train(userId, deviceUUID)
            
            if (success) {
                Log.d(TAG, "Siamese model trained successfully")
            } else {
                Log.e(TAG, "Failed to train Siamese model")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error training Siamese model: ${e.message}")
            false
        }
    }
    
    private suspend fun trainTemporalModel(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Temporal Convolution Model")
            
            // Get temporal data
            val temporalData = touchDataDao.getBehavioralFeatures(userId, 0L).first()
            if (temporalData.size < MIN_TRAINING_DATA) {
                Log.w(TAG, "Insufficient temporal data for training: ${temporalData.size}")
                return false
            }
            
            // Create and train the model
            temporalModel = TemporalConvolutionModel()
            val success = temporalModel!!.train(userId, deviceUUID)
            
            if (success) {
                Log.d(TAG, "Temporal model trained successfully")
            } else {
                Log.e(TAG, "Failed to train Temporal model")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error training Temporal model: ${e.message}")
            false
        }
    }
    
    private suspend fun buildUserProfile(userId: String): BehavioralProfile? {
        return try {
            val startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // Last 7 days
            val daoProfile = touchDataDao.getBehavioralProfile(userId, startTime)
            // Convert DAO profile to behavioral profile
            daoProfile?.let { profile ->
                BehavioralProfile(
                    userId = userId,
                    deviceUUID = "",
                    sessionId = "",
                    touchPatterns = TouchPatternAnalysis(),
                    navigationPatterns = NavigationPatternAnalysis(),
                    typingPatterns = TypingPatternAnalysis(),
                    scrollPatterns = ScrollPatternAnalysis(),
                    pressureProfile = PressureProfile(),
                    velocityProfile = VelocityProfile()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building user profile: ${e.message}")
            null
        }
    }
    
    private fun calculateWeightedConfidence(
        touchConfidence: Float,
        navConfidence: Float,
        geoConfidence: Float,
        siameseConfidence: Float,
        temporalConfidence: Float,
        userId: String
    ): Float {
        // Dynamic weight adjustment based on context
        val weights = geolocationModel?.getCurrentWeights() ?: defaultWeights()
        
        val weightedScore = (touchConfidence * weights.touchWeight +
                           navConfidence * weights.navigationWeight +
                           geoConfidence * weights.geolocationWeight +
                           siameseConfidence * weights.siameseWeight +
                           temporalConfidence * weights.temporalWeight)
        
        // Normalize to 0.0 - 1.0 range
        return weightedScore.coerceIn(0f, 1f)
    }
    
    private fun defaultWeights(): ModelWeights {
        return ModelWeights(
            touchWeight = 0.6f,
            navigationWeight = 0.4f,
            geolocationWeight = 0.3f,
            siameseWeight = 0.5f,
            temporalWeight = 0.4f
        )
    }
}

// Data classes

data class ModelStatus(
    val isTrained: Boolean,
    val touchModelTrained: Boolean,
    val navigationModelTrained: Boolean,
    val geolocationModelTrained: Boolean,
    val siameseModelTrained: Boolean,
    val temporalModelTrained: Boolean
)

data class ModelWeights(
    val touchWeight: Float,
    val navigationWeight: Float,
    val geolocationWeight: Float,
    val siameseWeight: Float,
    val temporalWeight: Float
)

enum class SecurityResponse {
    NORMAL_OPERATION,    // Confidence ≥ 0.8
    ALERT_USER,          // Confidence < 0.8
    RESTRICT_FEATURES,   // Confidence < 0.65
    IMMEDIATE_LOCK       // Confidence < 0.5
}
