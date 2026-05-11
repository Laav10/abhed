package com.example.bankingapp.ml

import android.util.Log
import kotlin.math.*

/**
 * Navigation Markov Model for ABHED
 * 
 * Implements a first-order Markov Chain to model user navigation patterns
 * and detect anomalous screen transitions and app usage flows.
 * 
 * Based on research: "User Behavioral Analysis Using Markov Chain and Steady-State"
 */
class NavigationMarkovModel {
    
    companion object {
        private const val TAG = "NavigationMarkovModel"
        private const val MIN_TRANSITIONS = 20 // Minimum transitions for training
        private const val SMOOTHING_FACTOR = 0.01 // Laplace smoothing
        private const val ANOMALY_THRESHOLD = -5.0 // Log-likelihood threshold
    }
    
    private var isTrained = false
    private val transitionMatrix = mutableMapOf<String, MutableMap<String, Double>>()
    private val stateFrequency = mutableMapOf<String, Int>()
    private val navigationHistory = mutableListOf<String>()
    private var totalTransitions = 0
    
    // Common navigation states in banking app
    private val validStates = setOf(
        "LANDING", "REGISTRATION", "BIOMETRIC_AUTH", "MAIN_DASHBOARD",
        "SEND_MONEY", "TRANSACTION_HISTORY", "PROFILE", "SETTINGS",
        "BALANCE_CHECK", "QUICK_TRANSFER", "BILL_PAY", "LOGOUT"
    )
    
    /**
     * Train the navigation model with historical transition data
     */
    suspend fun train(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training navigation model for user: $userId")
            
            // Simulate training data - in production, this would come from database
            val trainingSequence = generateTrainingData()
            
            if (trainingSequence.size < MIN_TRANSITIONS) {
                Log.w(TAG, "Insufficient navigation data: ${trainingSequence.size} < $MIN_TRANSITIONS")
                return false
            }
            
            // Build transition matrix
            buildTransitionMatrix(trainingSequence)
            
            // Calculate steady-state probabilities
            calculateSteadyState()
            
            isTrained = true
            Log.d(TAG, "Navigation model trained with ${trainingSequence.size} transitions")
            Log.d(TAG, "Transition matrix size: ${transitionMatrix.size} states")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to train navigation model: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score for current navigation sequence
     */
    fun getConfidence(userId: String): Float {
        return getConfidence(navigationHistory.takeLast(5))
    }
    
    /**
     * Get confidence score for a navigation sequence
     */
    fun getConfidence(sequence: List<String>): Float {
        if (!isTrained || sequence.size < 2) {
            return 0.5f
        }
        
        return try {
            val logLikelihood = calculateLogLikelihood(sequence)
            val confidence = sigmoid(logLikelihood / sequence.size)
            
            Log.v(TAG, "Navigation confidence: $confidence (log-likelihood: $logLikelihood)")
            confidence.toFloat().coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating navigation confidence: ${e.message}")
            0.5f
        }
    }
    
    /**
     * Update model with new screen transition
     */
    fun updateModel(screenTransition: String) {
        if (!validStates.contains(screenTransition)) {
            Log.w(TAG, "Unknown screen state: $screenTransition")
            return
        }
        
        try {
            // Add to navigation history
            navigationHistory.add(screenTransition)
            
            // Keep only recent history (sliding window)
            if (navigationHistory.size > 100) {
                navigationHistory.removeAt(0)
            }
            
            // Update transition matrix if trained
            if (isTrained && navigationHistory.size >= 2) {
                val fromState = navigationHistory[navigationHistory.size - 2]
                val toState = screenTransition
                
                updateTransitionProbability(fromState, toState)
            }
            
            Log.v(TAG, "Updated navigation: $screenTransition")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating navigation model: ${e.message}")
        }
    }
    
    /**
     * Detect anomalous navigation pattern
     */
    fun detectAnomaly(sequence: List<String>): Boolean {
        if (!isTrained || sequence.size < 2) return false
        
        val logLikelihood = calculateLogLikelihood(sequence)
        val isAnomalous = logLikelihood < ANOMALY_THRESHOLD
        
        if (isAnomalous) {
            Log.w(TAG, "Anomalous navigation detected: $sequence (log-likelihood: $logLikelihood)")
        }
        
        return isAnomalous
    }
    
    /**
     * Get most likely next state
     */
    fun predictNextState(currentState: String): String? {
        if (!isTrained || !transitionMatrix.containsKey(currentState)) {
            return null
        }
        
        return transitionMatrix[currentState]?.maxByOrNull { it.value }?.key
    }
    
    /**
     * Get navigation entropy (measure of predictability)
     */
    fun getNavigationEntropy(): Double {
        if (!isTrained) return 0.0
        
        var entropy = 0.0
        for ((fromState, transitions) in transitionMatrix) {
            for ((_, probability) in transitions) {
                if (probability > 0) {
                    entropy -= probability * ln(probability)
                }
            }
        }
        
        return entropy
    }
    
    // Private helper methods
    
    private fun buildTransitionMatrix(sequence: List<String>) {
        transitionMatrix.clear()
        stateFrequency.clear()
        totalTransitions = 0
        
        // Count transitions
        for (i in 0 until sequence.size - 1) {
            val fromState = sequence[i]
            val toState = sequence[i + 1]
            
            // Initialize maps if needed
            if (!transitionMatrix.containsKey(fromState)) {
                transitionMatrix[fromState] = mutableMapOf()
            }
            
            // Count transition
            val currentCount = transitionMatrix[fromState]!![toState] ?: 0.0
            transitionMatrix[fromState]!![toState] = currentCount + 1.0
            
            // Count state frequency
            stateFrequency[fromState] = stateFrequency.getOrDefault(fromState, 0) + 1
            totalTransitions++
        }
        
        // Convert counts to probabilities with Laplace smoothing
        for ((fromState, transitions) in transitionMatrix) {
            val totalFromState = stateFrequency[fromState] ?: 0
            val vocabularySize = validStates.size
            
            for (toState in validStates) {
                val count = transitions[toState] ?: 0.0
                val smoothedProbability = (count + SMOOTHING_FACTOR) / (totalFromState + SMOOTHING_FACTOR * vocabularySize)
                transitions[toState] = smoothedProbability
            }
        }
    }
    
    private fun calculateLogLikelihood(sequence: List<String>): Double {
        var logLikelihood = 0.0
        
        for (i in 0 until sequence.size - 1) {
            val fromState = sequence[i]
            val toState = sequence[i + 1]
            
            val probability = transitionMatrix[fromState]?.get(toState) ?: SMOOTHING_FACTOR
            logLikelihood += ln(probability)
        }
        
        return logLikelihood
    }
    
    private fun updateTransitionProbability(fromState: String, toState: String) {
        // Incremental update with exponential decay
        val decayFactor = 0.95
        val learningRate = 0.05
        
        if (!transitionMatrix.containsKey(fromState)) {
            transitionMatrix[fromState] = mutableMapOf()
        }
        
        val transitions = transitionMatrix[fromState]!!
        
        // Decay existing probabilities
        for ((state, prob) in transitions) {
            transitions[state] = prob * decayFactor
        }
        
        // Increase probability for observed transition
        val currentProb = transitions[toState] ?: 0.0
        transitions[toState] = currentProb + learningRate
        
        // Normalize probabilities
        val total = transitions.values.sum()
        if (total > 0) {
            for ((state, prob) in transitions) {
                transitions[state] = prob / total
            }
        }
    }
    
    private fun calculateSteadyState() {
        // Calculate steady-state probabilities using power iteration
        // This helps understand long-term navigation patterns
        
        val states = transitionMatrix.keys.toList()
        if (states.isEmpty()) return
        
        var steadyState = states.associateWith { 1.0 / states.size }.toMutableMap()
        
        // Power iteration
        repeat(100) {
            val newSteadyState = mutableMapOf<String, Double>()
            
            for (state in states) {
                newSteadyState[state] = 0.0
                for (prevState in states) {
                    val transitionProb = transitionMatrix[prevState]?.get(state) ?: 0.0
                    val prevSteadyProb = steadyState[prevState] ?: 0.0
                    newSteadyState[state] = newSteadyState[state]!! + transitionProb * prevSteadyProb
                }
            }
            
            steadyState = newSteadyState
        }
        
        Log.d(TAG, "Steady-state probabilities calculated: $steadyState")
    }
    
    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }
    
    private fun generateTrainingData(): List<String> {
        // Simulate typical banking app navigation patterns
        // In production, this would come from actual user data
        return listOf(
            "LANDING", "REGISTRATION", "BIOMETRIC_AUTH", "MAIN_DASHBOARD",
            "BALANCE_CHECK", "MAIN_DASHBOARD", "SEND_MONEY", "MAIN_DASHBOARD",
            "TRANSACTION_HISTORY", "MAIN_DASHBOARD", "PROFILE", "SETTINGS",
            "MAIN_DASHBOARD", "QUICK_TRANSFER", "MAIN_DASHBOARD", "BILL_PAY",
            "MAIN_DASHBOARD", "TRANSACTION_HISTORY", "MAIN_DASHBOARD", "LOGOUT",
            "LANDING", "BIOMETRIC_AUTH", "MAIN_DASHBOARD", "SEND_MONEY",
            "MAIN_DASHBOARD", "BALANCE_CHECK", "MAIN_DASHBOARD", "LOGOUT"
        )
    }
    
    /**
     * Reset model state
     */
    fun reset() {
        isTrained = false
        transitionMatrix.clear()
        stateFrequency.clear()
        navigationHistory.clear()
        totalTransitions = 0
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): NavigationModelStats {
        return NavigationModelStats(
            isTrained = isTrained,
            totalStates = transitionMatrix.size,
            totalTransitions = totalTransitions,
            navigationEntropy = getNavigationEntropy(),
            historySize = navigationHistory.size
        )
    }
}

data class NavigationModelStats(
    val isTrained: Boolean,
    val totalStates: Int,
    val totalTransitions: Int,
    val navigationEntropy: Double,
    val historySize: Int
)
