package com.example.bankingapp.ml

import android.util.Log
import com.example.bankingapp.data.models.TouchDataPoint
import kotlin.math.*

/**
 * Bayesian Trust Scoring Model for ABHED
 * 
 * Implements Bayesian inference for context-aware risk assessment and trust scoring.
 * Combines multiple behavioral signals with contextual factors to compute dynamic
 * trust scores and risk assessments for authentication decisions.
 * 
 * Features:
 * - Bayesian network for probabilistic reasoning
 * - Context-aware trust scoring (location, time, device state)
 * - Dynamic risk assessment with uncertainty quantification
 * - Prior belief updates based on user behavior history
 * - Multi-factor evidence integration
 * - Anomaly detection with confidence intervals
 */
class BayesianTrustModel {
    
    companion object {
        private const val TAG = "BayesianTrustModel"
        private const val MIN_EVIDENCE_COUNT = 5
        private const val TRUST_THRESHOLD_HIGH = 0.8f
        private const val TRUST_THRESHOLD_LOW = 0.3f
        private const val RISK_THRESHOLD_HIGH = 0.7f
        private const val RISK_THRESHOLD_LOW = 0.2f
    }
    
    private var isTrained = false
    private val userTrustProfiles = mutableMapOf<String, UserTrustProfile>()
    private val contextualFactors = mutableMapOf<String, ContextualFactor>()
    private val evidenceHistory = mutableListOf<EvidenceRecord>()
    
    /**
     * Train the Bayesian trust model with user behavioral and contextual data
     */
    suspend fun train(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Bayesian trust model for user: $userId")
            
            // Initialize user trust profile
            val trustProfile = initializeUserTrustProfile(userId, deviceUUID)
            userTrustProfiles[userId] = trustProfile
            
            // Initialize contextual factors
            initializeContextualFactors()
            
            // Generate training evidence
            val trainingEvidence = generateTrainingEvidence(userId)
            
            if (trainingEvidence.size < MIN_EVIDENCE_COUNT) {
                Log.w(TAG, "Insufficient training evidence: ${trainingEvidence.size}")
                return false
            }
            
            // Update Bayesian network with training data
            updateBayesianNetwork(trainingEvidence, userId)
            
            // Calculate initial trust priors
            calculateTrustPriors(userId)
            
            isTrained = true
            Log.d(TAG, "Bayesian trust model trained with ${trainingEvidence.size} evidence records")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to train Bayesian trust model: ${e.message}")
            false
        }
    }
    
    /**
     * Calculate trust score based on current behavioral and contextual evidence
     */
    fun getTrustScore(
        userId: String,
        recentTouchData: List<TouchDataPoint>,
        contextData: ContextData
    ): TrustScore {
        if (!isTrained || !userTrustProfiles.containsKey(userId)) {
            return TrustScore(0.5f, 0.5f, TrustLevel.MEDIUM, "Model not trained")
        }
        
        return try {
            // Extract behavioral evidence
            val behavioralEvidence = extractBehavioralEvidence(recentTouchData)
            
            // Extract contextual evidence
            val contextualEvidence = extractContextualEvidence(contextData)
            
            // Combine all evidence
            val combinedEvidence = combineEvidence(behavioralEvidence, contextualEvidence)
            
            // Apply Bayesian inference
            val trustPosterior = calculateTrustPosterior(userId, combinedEvidence)
            
            // Calculate risk score
            val riskScore = calculateRiskScore(trustPosterior, contextData)
            
            // Determine trust level
            val trustLevel = determineTrustLevel(trustPosterior.trustScore, riskScore)
            
            // Generate explanation
            val explanation = generateTrustExplanation(trustPosterior, riskScore, contextData)
            
            TrustScore(
                trustScore = trustPosterior.trustScore,
                riskScore = riskScore,
                trustLevel = trustLevel,
                explanation = explanation,
                confidence = trustPosterior.confidence,
                evidenceCount = combinedEvidence.size,
                contextualFactors = contextData.getFactorSummary()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating trust score: ${e.message}")
            TrustScore(0.5f, 0.5f, TrustLevel.MEDIUM, "Error: ${e.message}")
        }
    }
    
    /**
     * Update model with new behavioral and contextual evidence
     */
    fun updateModel(
        userId: String,
        touchData: List<TouchDataPoint>,
        contextData: ContextData,
        actualOutcome: AuthenticationOutcome
    ) {
        if (!isTrained || !userTrustProfiles.containsKey(userId)) return
        
        try {
            // Create evidence record
            val evidenceRecord = EvidenceRecord(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                behavioralEvidence = extractBehavioralEvidence(touchData),
                contextualEvidence = extractContextualEvidence(contextData),
                actualOutcome = actualOutcome
            )
            
            // Add to evidence history
            evidenceHistory.add(evidenceRecord)
            
            // Keep history manageable
            if (evidenceHistory.size > 1000) {
                evidenceHistory.subList(0, evidenceHistory.size - 500).clear()
            }
            
            // Update Bayesian network
            updateBayesianNetwork(listOf(evidenceRecord), userId)
            
            // Update user trust profile
            updateUserTrustProfile(userId, evidenceRecord)
            
            Log.v(TAG, "Updated Bayesian trust model for user: $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Bayesian trust model: ${e.message}")
        }
    }
    
    /**
     * Detect trust anomalies and risk patterns
     */
    fun detectTrustAnomaly(
        userId: String,
        recentData: List<TouchDataPoint>,
        contextData: ContextData
    ): TrustAnomalyResult {
        if (!isTrained || !userTrustProfiles.containsKey(userId)) {
            return TrustAnomalyResult(false, 0.5f, "Model not trained")
        }
        
        return try {
            val trustScore = getTrustScore(userId, recentData, contextData)
            
            // Check for trust anomalies
            val trustAnomaly = trustScore.trustScore < TRUST_THRESHOLD_LOW
            val riskAnomaly = trustScore.riskScore > RISK_THRESHOLD_HIGH
            
            val isAnomalous = trustAnomaly || riskAnomaly
            val anomalyScore = maxOf(1f - trustScore.trustScore, trustScore.riskScore)
            
            val description = when {
                trustAnomaly && riskAnomaly -> "High risk and low trust detected"
                trustAnomaly -> "Low trust score anomaly"
                riskAnomaly -> "High risk score anomaly"
                else -> "Normal trust pattern"
            }
            
            TrustAnomalyResult(
                isAnomalous = isAnomalous,
                anomalyScore = anomalyScore,
                description = description,
                trustScore = trustScore.trustScore,
                riskScore = trustScore.riskScore,
                contextualRisk = assessContextualRisk(contextData)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting trust anomaly: ${e.message}")
            TrustAnomalyResult(false, 0.5f, "Error: ${e.message}")
        }
    }
    
    // Private helper methods
    
    private fun initializeUserTrustProfile(userId: String, deviceUUID: String): UserTrustProfile {
        return UserTrustProfile(
            userId = userId,
            deviceUUID = deviceUUID,
            trustPrior = 0.7f, // Initial trust assumption
            riskPrior = 0.3f, // Initial risk assumption
            behavioralPriors = mutableMapOf(
                "touch_consistency" to 0.8f,
                "navigation_pattern" to 0.7f,
                "temporal_pattern" to 0.6f,
                "location_familiarity" to 0.8f
            ),
            contextualWeights = mutableMapOf(
                "location_risk" to 0.3f,
                "time_risk" to 0.2f,
                "device_risk" to 0.4f,
                "network_risk" to 0.1f
            ),
            evidenceCount = 0,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun initializeContextualFactors() {
        contextualFactors["location_familiarity"] = ContextualFactor(
            name = "location_familiarity",
            weight = 0.3f,
            riskMapping = mapOf(
                "home" to 0.1f,
                "work" to 0.2f,
                "familiar" to 0.3f,
                "unfamiliar" to 0.7f,
                "high_risk" to 0.9f
            )
        )
        
        contextualFactors["time_pattern"] = ContextualFactor(
            name = "time_pattern",
            weight = 0.2f,
            riskMapping = mapOf(
                "usual_hours" to 0.1f,
                "extended_hours" to 0.4f,
                "unusual_hours" to 0.8f
            )
        )
        
        contextualFactors["device_state"] = ContextualFactor(
            name = "device_state",
            weight = 0.4f,
            riskMapping = mapOf(
                "secure" to 0.1f,
                "normal" to 0.3f,
                "suspicious" to 0.7f,
                "compromised" to 0.9f
            )
        )
        
        contextualFactors["network_security"] = ContextualFactor(
            name = "network_security",
            weight = 0.1f,
            riskMapping = mapOf(
                "secure" to 0.1f,
                "public" to 0.5f,
                "unsecured" to 0.8f
            )
        )
    }
    
    private fun extractBehavioralEvidence(touchData: List<TouchDataPoint>): Map<String, Float> {
        if (touchData.isEmpty()) return emptyMap()
        
        val evidence = mutableMapOf<String, Float>()
        
        // Touch consistency evidence
        val pressureVariance = calculatePressureVariance(touchData)
        evidence["touch_consistency"] = 1f - (pressureVariance / 0.5f).coerceIn(0f, 1f)
        
        // Timing consistency evidence
        val timingVariance = calculateTimingVariance(touchData)
        evidence["timing_consistency"] = 1f - (timingVariance / 1000f).coerceIn(0f, 1f)
        
        // Movement pattern evidence
        val movementSmoothness = calculateMovementSmoothness(touchData)
        evidence["movement_smoothness"] = movementSmoothness
        
        // Velocity consistency evidence
        val velocityConsistency = calculateVelocityConsistency(touchData)
        evidence["velocity_consistency"] = velocityConsistency
        
        return evidence
    }
    
    private fun extractContextualEvidence(contextData: ContextData): Map<String, Float> {
        val evidence = mutableMapOf<String, Float>()
        
        // Location evidence
        evidence["location_familiarity"] = when (contextData.locationContext) {
            "home" -> 0.9f
            "work" -> 0.8f
            "familiar" -> 0.6f
            "unfamiliar" -> 0.3f
            else -> 0.5f
        }
        
        // Time pattern evidence
        evidence["time_pattern"] = when (contextData.timeContext) {
            "usual_hours" -> 0.9f
            "extended_hours" -> 0.6f
            "unusual_hours" -> 0.2f
            else -> 0.5f
        }
        
        // Device state evidence
        evidence["device_security"] = when (contextData.deviceContext) {
            "secure" -> 0.9f
            "normal" -> 0.7f
            "suspicious" -> 0.3f
            "compromised" -> 0.1f
            else -> 0.5f
        }
        
        // Network security evidence
        evidence["network_security"] = when (contextData.networkContext) {
            "secure" -> 0.9f
            "public" -> 0.5f
            "unsecured" -> 0.2f
            else -> 0.5f
        }
        
        return evidence
    }
    
    private fun combineEvidence(
        behavioral: Map<String, Float>,
        contextual: Map<String, Float>
    ): Map<String, Float> {
        val combined = mutableMapOf<String, Float>()
        combined.putAll(behavioral)
        combined.putAll(contextual)
        return combined
    }
    
    private fun calculateTrustPosterior(userId: String, evidence: Map<String, Float>): TrustPosterior {
        val userProfile = userTrustProfiles[userId] ?: return TrustPosterior(0.5f, 0.5f)
        
        // Apply Bayesian inference
        var trustPosterior = userProfile.trustPrior
        var totalWeight = 0f
        
        for ((evidenceType, evidenceValue) in evidence) {
            val prior = userProfile.behavioralPriors[evidenceType] ?: 0.5f
            val weight = 1f / (1f + abs(evidenceValue - prior))
            
            trustPosterior += weight * evidenceValue
            totalWeight += weight
        }
        
        if (totalWeight > 0) {
            trustPosterior /= (totalWeight + 1f) // +1 for prior
        }
        
        // Calculate confidence based on evidence consistency
        val evidenceVariance = evidence.values.map { (it - trustPosterior).pow(2) }.average()
        val confidence = 1f - sqrt(evidenceVariance.toFloat()).coerceIn(0f, 1f)
        
        return TrustPosterior(
            trustScore = trustPosterior.coerceIn(0f, 1f),
            confidence = confidence.coerceIn(0f, 1f)
        )
    }
    
    private fun calculateRiskScore(trustPosterior: TrustPosterior, contextData: ContextData): Float {
        var riskScore = 1f - trustPosterior.trustScore
        
        // Apply contextual risk factors
        val locationRisk = contextualFactors["location_familiarity"]?.riskMapping?.get(contextData.locationContext) ?: 0.5f
        val timeRisk = contextualFactors["time_pattern"]?.riskMapping?.get(contextData.timeContext) ?: 0.5f
        val deviceRisk = contextualFactors["device_state"]?.riskMapping?.get(contextData.deviceContext) ?: 0.5f
        val networkRisk = contextualFactors["network_security"]?.riskMapping?.get(contextData.networkContext) ?: 0.5f
        
        val contextualRisk = (locationRisk * 0.3f + timeRisk * 0.2f + deviceRisk * 0.4f + networkRisk * 0.1f)
        
        // Combine behavioral and contextual risk
        riskScore = (riskScore * 0.7f + contextualRisk * 0.3f).coerceIn(0f, 1f)
        
        return riskScore
    }
    
    private fun determineTrustLevel(trustScore: Float, riskScore: Float): TrustLevel {
        return when {
            trustScore >= TRUST_THRESHOLD_HIGH && riskScore <= RISK_THRESHOLD_LOW -> TrustLevel.HIGH
            trustScore >= TRUST_THRESHOLD_LOW && riskScore <= RISK_THRESHOLD_HIGH -> TrustLevel.MEDIUM
            else -> TrustLevel.LOW
        }
    }
    
    private fun generateTrustExplanation(
        trustPosterior: TrustPosterior,
        riskScore: Float,
        contextData: ContextData
    ): String {
        val explanations = mutableListOf<String>()
        
        when {
            trustPosterior.trustScore >= TRUST_THRESHOLD_HIGH -> 
                explanations.add("High behavioral consistency")
            trustPosterior.trustScore <= TRUST_THRESHOLD_LOW -> 
                explanations.add("Low behavioral consistency")
        }
        
        when {
            riskScore >= RISK_THRESHOLD_HIGH -> 
                explanations.add("High contextual risk")
            riskScore <= RISK_THRESHOLD_LOW -> 
                explanations.add("Low contextual risk")
        }
        
        if (contextData.locationContext == "unfamiliar") {
            explanations.add("Unfamiliar location")
        }
        
        if (contextData.timeContext == "unusual_hours") {
            explanations.add("Unusual time pattern")
        }
        
        return if (explanations.isNotEmpty()) {
            explanations.joinToString(", ")
        } else {
            "Normal behavioral and contextual patterns"
        }
    }
    
    private fun assessContextualRisk(contextData: ContextData): Float {
        val locationRisk = contextualFactors["location_familiarity"]?.riskMapping?.get(contextData.locationContext) ?: 0.5f
        val timeRisk = contextualFactors["time_pattern"]?.riskMapping?.get(contextData.timeContext) ?: 0.5f
        val deviceRisk = contextualFactors["device_state"]?.riskMapping?.get(contextData.deviceContext) ?: 0.5f
        val networkRisk = contextualFactors["network_security"]?.riskMapping?.get(contextData.networkContext) ?: 0.5f
        
        return (locationRisk * 0.3f + timeRisk * 0.2f + deviceRisk * 0.4f + networkRisk * 0.1f)
    }
    
    private fun updateBayesianNetwork(evidenceRecords: List<EvidenceRecord>, userId: String) {
        val userProfile = userTrustProfiles[userId] ?: return
        
        for (record in evidenceRecords) {
            // Update behavioral priors based on actual outcomes
            val learningRate = 0.1f
            val outcome = if (record.actualOutcome == AuthenticationOutcome.SUCCESS) 1f else 0f
            
            for ((evidenceType, evidenceValue) in record.behavioralEvidence) {
                val currentPrior = userProfile.behavioralPriors[evidenceType] ?: 0.5f
                val updatedPrior = currentPrior + learningRate * (outcome - currentPrior) * evidenceValue
                userProfile.behavioralPriors[evidenceType] = updatedPrior.coerceIn(0f, 1f)
            }
            
            userProfile.evidenceCount++
        }
        
        userProfile.lastUpdated = System.currentTimeMillis()
    }
    
    private fun updateUserTrustProfile(userId: String, evidenceRecord: EvidenceRecord) {
        val userProfile = userTrustProfiles[userId] ?: return
        
        // Update trust and risk priors with exponential moving average
        val alpha = 0.1f
        val outcome = if (evidenceRecord.actualOutcome == AuthenticationOutcome.SUCCESS) 1f else 0f
        
        userProfile.trustPrior = (1 - alpha) * userProfile.trustPrior + alpha * outcome
        userProfile.riskPrior = (1 - alpha) * userProfile.riskPrior + alpha * (1f - outcome)
    }
    
    private fun calculatePressureVariance(touchData: List<TouchDataPoint>): Float {
        if (touchData.size < 2) return 0f
        
        val pressures = touchData.map { it.pressure }
        val mean = pressures.average().toFloat()
        val variance = pressures.map { (it - mean).pow(2) }.average().toFloat()
        
        return sqrt(variance)
    }
    
    private fun calculateTimingVariance(touchData: List<TouchDataPoint>): Float {
        if (touchData.size < 2) return 0f
        
        val intervals = touchData.zipWithNext { a, b -> b.timestamp - a.timestamp }
        val mean = intervals.average().toFloat()
        val variance = intervals.map { (it - mean).pow(2) }.average().toFloat()
        
        return sqrt(variance)
    }
    
    private fun calculateMovementSmoothness(touchData: List<TouchDataPoint>): Float {
        if (touchData.size < 3) return 0.5f
        
        val accelerations = mutableListOf<Float>()
        
        for (i in 1 until touchData.size - 1) {
            val prev = touchData[i - 1]
            val curr = touchData[i]
            val next = touchData[i + 1]
            
            val v1x = curr.x - prev.x
            val v1y = curr.y - prev.y
            val v2x = next.x - curr.x
            val v2y = next.y - curr.y
            
            val acceleration = sqrt((v2x - v1x).pow(2) + (v2y - v1y).pow(2))
            accelerations.add(acceleration)
        }
        
        if (accelerations.isEmpty()) return 0.5f
        
        val meanAcceleration = accelerations.average().toFloat()
        val smoothness = 1f / (1f + meanAcceleration / 100f)
        
        return smoothness.coerceIn(0f, 1f)
    }
    
    private fun calculateVelocityConsistency(touchData: List<TouchDataPoint>): Float {
        if (touchData.size < 2) return 0.5f
        
        val velocities = touchData.mapNotNull { it.velocity }
        if (velocities.isEmpty()) return 0.5f
        
        val mean = velocities.average().toFloat()
        val variance = velocities.map { (it - mean).pow(2) }.average().toFloat()
        val consistency = 1f / (1f + sqrt(variance))
        
        return consistency.coerceIn(0f, 1f)
    }
    
    private fun generateTrainingEvidence(userId: String): List<EvidenceRecord> {
        val evidenceRecords = mutableListOf<EvidenceRecord>()
        
        repeat(20) {
            val behavioralEvidence = mapOf(
                "touch_consistency" to (0.6f + Math.random().toFloat() * 0.3f),
                "timing_consistency" to (0.5f + Math.random().toFloat() * 0.4f),
                "movement_smoothness" to (0.7f + Math.random().toFloat() * 0.2f),
                "velocity_consistency" to (0.6f + Math.random().toFloat() * 0.3f)
            )
            
            val contextualEvidence = mapOf(
                "location_familiarity" to (0.8f + Math.random().toFloat() * 0.2f),
                "time_pattern" to (0.7f + Math.random().toFloat() * 0.3f),
                "device_security" to (0.9f + Math.random().toFloat() * 0.1f),
                "network_security" to (0.6f + Math.random().toFloat() * 0.4f)
            )
            
            val outcome = if (Math.random() > 0.1) AuthenticationOutcome.SUCCESS else AuthenticationOutcome.FAILURE
            
            evidenceRecords.add(
                EvidenceRecord(
                    userId = userId,
                    timestamp = System.currentTimeMillis() - (it * 1000L),
                    behavioralEvidence = behavioralEvidence,
                    contextualEvidence = contextualEvidence,
                    actualOutcome = outcome
                )
            )
        }
        
        return evidenceRecords
    }
    
    private fun calculateTrustPriors(userId: String) {
        val userProfile = userTrustProfiles[userId] ?: return
        
        // Calculate initial priors based on training evidence
        val userEvidence = evidenceHistory.filter { it.userId == userId }
        if (userEvidence.isNotEmpty()) {
            val successRate = userEvidence.count { it.actualOutcome == AuthenticationOutcome.SUCCESS }.toFloat() / userEvidence.size
            userProfile.trustPrior = successRate
            userProfile.riskPrior = 1f - successRate
        }
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): BayesianModelStats {
        return BayesianModelStats(
            isTrained = isTrained,
            userProfileCount = userTrustProfiles.size,
            contextualFactorCount = contextualFactors.size,
            evidenceHistorySize = evidenceHistory.size,
            avgTrustPrior = userTrustProfiles.values.map { it.trustPrior }.average().toFloat(),
            avgRiskPrior = userTrustProfiles.values.map { it.riskPrior }.average().toFloat()
        )
    }
    
    /**
     * Reset model state
     */
    fun reset() {
        isTrained = false
        userTrustProfiles.clear()
        contextualFactors.clear()
        evidenceHistory.clear()
    }
}

// Data classes

data class UserTrustProfile(
    val userId: String,
    val deviceUUID: String,
    var trustPrior: Float,
    var riskPrior: Float,
    val behavioralPriors: MutableMap<String, Float>,
    val contextualWeights: MutableMap<String, Float>,
    var evidenceCount: Int,
    var lastUpdated: Long
)

data class ContextualFactor(
    val name: String,
    val weight: Float,
    val riskMapping: Map<String, Float>
)

data class EvidenceRecord(
    val userId: String,
    val timestamp: Long,
    val behavioralEvidence: Map<String, Float>,
    val contextualEvidence: Map<String, Float>,
    val actualOutcome: AuthenticationOutcome
)

data class TrustPosterior(
    val trustScore: Float,
    val confidence: Float
)

data class ContextData(
    val locationContext: String, // "home", "work", "familiar", "unfamiliar", "high_risk"
    val timeContext: String, // "usual_hours", "extended_hours", "unusual_hours"
    val deviceContext: String, // "secure", "normal", "suspicious", "compromised"
    val networkContext: String, // "secure", "public", "unsecured"
    val additionalFactors: Map<String, Any> = emptyMap()
) {
    fun getFactorSummary(): String {
        return "Location: $locationContext, Time: $timeContext, Device: $deviceContext, Network: $networkContext"
    }
}

data class TrustScore(
    val trustScore: Float,
    val riskScore: Float,
    val trustLevel: TrustLevel,
    val explanation: String,
    val confidence: Float = 0.5f,
    val evidenceCount: Int = 0,
    val contextualFactors: String = ""
)

data class TrustAnomalyResult(
    val isAnomalous: Boolean,
    val anomalyScore: Float,
    val description: String,
    val trustScore: Float = 0.5f,
    val riskScore: Float = 0.5f,
    val contextualRisk: Float = 0.5f
)

data class BayesianModelStats(
    val isTrained: Boolean,
    val userProfileCount: Int,
    val contextualFactorCount: Int,
    val evidenceHistorySize: Int,
    val avgTrustPrior: Float,
    val avgRiskPrior: Float
)

enum class TrustLevel {
    HIGH, MEDIUM, LOW
}

enum class AuthenticationOutcome {
    SUCCESS, FAILURE, PARTIAL
}
