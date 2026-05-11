package com.example.bankingapp.ml

import android.util.Log
import com.example.bankingapp.data.models.TouchDataPoint
import kotlin.math.*

/**
 * Temporal Convolutional Network (TCN) for ABHED
 * 
 * Implements 1D causal convolutions with dilation for temporal sequence modeling.
 * Analyzes behavioral time-series data to detect patterns and anomalies in
 * keystroke timing, scroll sequences, and tap intervals.
 * 
 * Features:
 * - Causal convolutions (no future leakage)
 * - Dilated convolutions for long-range dependencies
 * - Residual connections for deep networks
 * - Real-time temporal pattern analysis
 * - Adaptive sequence length handling
 */
class TemporalConvolutionModel {
    
    companion object {
        private const val TAG = "TemporalConvolutionModel"
        private const val SEQUENCE_LENGTH = 32 // Input sequence length
        private const val FEATURE_SIZE = 8 // Features per time step
        private const val KERNEL_SIZE = 3 // Convolution kernel size
        private const val NUM_LAYERS = 4 // Number of TCN layers
        private const val HIDDEN_SIZE = 64 // Hidden layer size
        private const val MIN_SEQUENCE_LENGTH = 5 // Minimum sequence for analysis
    }
    
    private var isTrained = false
    private val convolutionLayers = mutableListOf<ConvolutionLayer>()
    private val temporalBuffer = mutableListOf<TemporalFeature>()
    private val userPatterns = mutableMapOf<String, TemporalPattern>()
    
    /**
     * Train the temporal convolution model with user behavioral sequences
     */
    suspend fun train(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training temporal convolution model for user: $userId")
            
            // Generate training sequences
            val trainingSequences = generateTemporalTrainingData(userId)
            
            if (trainingSequences.size < 10) {
                Log.w(TAG, "Insufficient temporal sequences: ${trainingSequences.size}")
                return false
            }
            
            // Initialize convolution layers
            initializeConvolutionLayers()
            
            // Train the network
            trainTCN(trainingSequences, userId)
            
            // Extract user temporal patterns
            val userPattern = extractUserPattern(trainingSequences, userId)
            userPatterns[userId] = userPattern
            
            isTrained = true
            Log.d(TAG, "TCN trained with ${trainingSequences.size} sequences")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to train temporal convolution model: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score for current temporal sequence
     */
    fun getConfidence(recentTouchData: List<TouchDataPoint>): Float {
        if (!isTrained || recentTouchData.size < MIN_SEQUENCE_LENGTH) {
            return 0.5f
        }
        
        return try {
            // Convert touch data to temporal features
            val temporalFeatures = extractTemporalFeatures(recentTouchData)
            
            // Create sequence for analysis
            val sequence = createSequence(temporalFeatures)
            
            // Forward pass through TCN
            val output = forwardPass(sequence)
            
            // Calculate confidence based on pattern similarity
            val confidence = calculateTemporalConfidence(output)
            
            Log.v(TAG, "Temporal confidence: $confidence")
            confidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating temporal confidence: ${e.message}")
            0.5f
        }
    }
    
    /**
     * Update model with new temporal data
     */
    fun updateModel(temporalData: List<TouchDataPoint>) {
        if (!isTrained || temporalData.isEmpty()) return
        
        try {
            // Extract temporal features
            val newFeatures = extractTemporalFeatures(temporalData)
            
            // Add to temporal buffer
            temporalBuffer.addAll(newFeatures)
            
            // Keep buffer size manageable
            if (temporalBuffer.size > 1000) {
                temporalBuffer.subList(0, temporalBuffer.size - 500).clear()
            }
            
            // Update user patterns incrementally
            updateUserPatterns(newFeatures)
            
            Log.v(TAG, "Updated TCN with ${temporalData.size} new data points")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating temporal convolution model: ${e.message}")
        }
    }
    
    /**
     * Detect temporal anomalies in behavior sequence
     */
    fun detectTemporalAnomaly(sequence: List<TouchDataPoint>): AnomalyResult {
        if (!isTrained || sequence.size < MIN_SEQUENCE_LENGTH) {
            return AnomalyResult(false, 0.5f, "Insufficient data")
        }
        
        return try {
            val features = extractTemporalFeatures(sequence)
            val tcnSequence = createSequence(features)
            val output = forwardPass(tcnSequence)
            
            val anomalyScore = calculateAnomalyScore(output)
            val isAnomalous = anomalyScore > 0.7f
            
            AnomalyResult(
                isAnomalous = isAnomalous,
                score = anomalyScore,
                description = if (isAnomalous) "Temporal anomaly detected" else "Normal temporal pattern"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting temporal anomaly: ${e.message}")
            AnomalyResult(false, 0.5f, "Error: ${e.message}")
        }
    }
    
    // Private helper methods
    
    private fun extractTemporalFeatures(touchData: List<TouchDataPoint>): List<TemporalFeature> {
        val features = mutableListOf<TemporalFeature>()
        
        for (i in touchData.indices) {
            val touch = touchData[i]
            
            // Calculate inter-touch interval
            val interval = if (i > 0) {
                (touch.timestamp - touchData[i - 1].timestamp).toFloat()
            } else 0f
            
            // Calculate velocity if available
            val velocity = touch.velocity ?: 0f
            
            // Calculate pressure change
            val pressureChange = if (i > 0) {
                touch.pressure - touchData[i - 1].pressure
            } else 0f
            
            // Calculate movement distance
            val movement = if (i > 0) {
                val prev = touchData[i - 1]
                sqrt((touch.x - prev.x).pow(2) + (touch.y - prev.y).pow(2).toDouble()).toFloat()
            } else 0f
            
            // Create temporal feature vector
            val featureVector = floatArrayOf(
                interval / 1000f, // Normalize to seconds
                velocity,
                touch.pressure,
                pressureChange,
                movement,
                touch.x / 1000f, // Normalize coordinates
                touch.y / 1000f,
                touch.type.ordinal.toFloat()
            )
            
            features.add(
                TemporalFeature(
                    timestamp = touch.timestamp,
                    features = featureVector
                )
            )
        }
        
        return features
    }
    
    private fun createSequence(features: List<TemporalFeature>): Array<FloatArray> {
        val sequence = Array(SEQUENCE_LENGTH) { FloatArray(FEATURE_SIZE) }
        
        // Pad or truncate to fixed sequence length
        val startIndex = maxOf(0, features.size - SEQUENCE_LENGTH)
        
        for (i in 0 until SEQUENCE_LENGTH) {
            val featureIndex = startIndex + i
            if (featureIndex < features.size) {
                val feature = features[featureIndex]
                for (j in 0 until minOf(FEATURE_SIZE, feature.features.size)) {
                    sequence[i][j] = feature.features[j]
                }
            }
            // Remaining values stay as 0 (padding)
        }
        
        return sequence
    }
    
    private fun forwardPass(sequence: Array<FloatArray>): FloatArray {
        var currentSequence = sequence
        
        // Pass through each convolution layer
        for (layer in convolutionLayers) {
            currentSequence = applyConvolutionLayer(currentSequence, layer)
        }
        
        // Global average pooling
        val output = FloatArray(HIDDEN_SIZE)
        for (i in output.indices) {
            output[i] = currentSequence.map { it.getOrElse(i) { 0f } }.average().toFloat()
        }
        
        return output
    }
    
    private fun applyConvolutionLayer(input: Array<FloatArray>, layer: ConvolutionLayer): Array<FloatArray> {
        val output = Array(input.size) { FloatArray(layer.outputSize) }
        
        for (t in input.indices) {
            for (o in 0 until layer.outputSize) {
                var sum = layer.bias[o]
                
                // Apply causal convolution with dilation
                for (k in 0 until KERNEL_SIZE) {
                    val inputTime = t - k * layer.dilation
                    if (inputTime >= 0) {
                        for (i in 0 until layer.inputSize) {
                            sum += input[inputTime][i] * layer.weights[k][i][o]
                        }
                    }
                }
                
                // Apply activation (ReLU)
                output[t][o] = maxOf(0f, sum)
            }
        }
        
        // Add residual connection if dimensions match
        if (input[0].size == output[0].size) {
            for (t in output.indices) {
                for (i in output[t].indices) {
                    output[t][i] += input[t][i]
                }
            }
        }
        
        return output
    }
    
    private fun calculateTemporalConfidence(output: FloatArray): Float {
        // Compare with stored user patterns
        val bestMatch = userPatterns.values.minByOrNull { pattern ->
            calculateDistance(output, pattern.representation)
        }
        
        return if (bestMatch != null) {
            val distance = calculateDistance(output, bestMatch.representation)
            val maxDistance = sqrt(output.size.toDouble()).toFloat()
            1f - (distance / maxDistance).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }
    
    private fun calculateAnomalyScore(output: FloatArray): Float {
        // Calculate how different this output is from normal patterns
        val avgPattern = userPatterns.values.map { it.representation }
            .fold(FloatArray(output.size)) { acc, pattern ->
                for (i in acc.indices) {
                    acc[i] += pattern.getOrElse(i) { 0f }
                }
                acc
            }.map { it / userPatterns.size }.toFloatArray()
        
        val distance = calculateDistance(output, avgPattern)
        val maxDistance = sqrt(output.size.toDouble()).toFloat()
        
        return (distance / maxDistance).coerceIn(0f, 1f)
    }
    
    private fun calculateDistance(a: FloatArray, b: FloatArray): Float {
        return sqrt(a.zip(b) { x, y -> (x - y).pow(2) }.sum().toDouble()).toFloat()
    }
    
    private fun initializeConvolutionLayers() {
        convolutionLayers.clear()
        
        var inputSize = FEATURE_SIZE
        var dilation = 1
        
        for (i in 0 until NUM_LAYERS) {
            val outputSize = if (i == NUM_LAYERS - 1) HIDDEN_SIZE else inputSize * 2
            
            val layer = ConvolutionLayer(
                inputSize = inputSize,
                outputSize = outputSize,
                dilation = dilation,
                weights = Array(KERNEL_SIZE) { 
                    Array(inputSize) { 
                        FloatArray(outputSize) { (Math.random() * 2 - 1).toFloat() * 0.1f }
                    }
                },
                bias = FloatArray(outputSize) { 0f }
            )
            
            convolutionLayers.add(layer)
            inputSize = outputSize
            dilation *= 2 // Exponential dilation
        }
    }
    
    private fun trainTCN(sequences: List<TemporalSequence>, userId: String) {
        // Simplified training - in production would use proper gradient descent
        for (sequence in sequences) {
            val tcnSequence = createSequence(sequence.features)
            val output = forwardPass(tcnSequence)
            
            // Store training example for pattern extraction
            temporalBuffer.addAll(sequence.features)
        }
    }
    
    private fun extractUserPattern(sequences: List<TemporalSequence>, userId: String): TemporalPattern {
        val userSequences = sequences.filter { it.userId == userId }
        val outputs = userSequences.map { sequence ->
            val tcnSequence = createSequence(sequence.features)
            forwardPass(tcnSequence)
        }
        
        // Average the outputs to create user representation
        val avgOutput = FloatArray(HIDDEN_SIZE) { i ->
            outputs.map { it[i] }.average().toFloat()
        }
        
        return TemporalPattern(
            userId = userId,
            representation = avgOutput,
            confidence = 0.8f,
            sequenceCount = userSequences.size,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun updateUserPatterns(newFeatures: List<TemporalFeature>) {
        // Update existing patterns with exponential moving average
        val alpha = 0.1f
        
        if (newFeatures.size >= MIN_SEQUENCE_LENGTH) {
            val sequence = createSequence(newFeatures)
            val output = forwardPass(sequence)
            
            userPatterns.values.forEach { pattern ->
                for (i in pattern.representation.indices) {
                    pattern.representation[i] = (1 - alpha) * pattern.representation[i] + alpha * output[i]
                }
                pattern.lastUpdated = System.currentTimeMillis()
            }
        }
    }
    
    private fun generateTemporalTrainingData(userId: String): List<TemporalSequence> {
        // Simulate temporal sequences for training
        val sequences = mutableListOf<TemporalSequence>()
        
        repeat(20) {
            val features = mutableListOf<TemporalFeature>()
            var timestamp = System.currentTimeMillis() - (it * 1000L)
            
            repeat(SEQUENCE_LENGTH) { j ->
                val featureVector = floatArrayOf(
                    (50 + Math.random() * 100).toFloat(), // Interval
                    (Math.random() * 2).toFloat(), // Velocity
                    (0.3 + Math.random() * 0.4).toFloat(), // Pressure
                    (Math.random() * 0.2 - 0.1).toFloat(), // Pressure change
                    (Math.random() * 50).toFloat(), // Movement
                    (Math.random() * 1000).toFloat(), // X coordinate
                    (Math.random() * 1000).toFloat(), // Y coordinate
                    (Math.random() * 3).toFloat() // Touch type
                )
                
                features.add(TemporalFeature(timestamp, featureVector))
                timestamp += (50 + Math.random() * 100).toLong()
            }
            
            sequences.add(TemporalSequence(userId, features))
        }
        
        return sequences
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): TemporalModelStats {
        return TemporalModelStats(
            isTrained = isTrained,
            layerCount = convolutionLayers.size,
            sequenceLength = SEQUENCE_LENGTH,
            featureSize = FEATURE_SIZE,
            userPatternCount = userPatterns.size,
            bufferSize = temporalBuffer.size
        )
    }
    
    /**
     * Reset model state
     */
    fun reset() {
        isTrained = false
        convolutionLayers.clear()
        temporalBuffer.clear()
        userPatterns.clear()
    }
}

// Data classes

data class TemporalFeature(
    val timestamp: Long,
    val features: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TemporalFeature
        return timestamp == other.timestamp && features.contentEquals(other.features)
    }
    
    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + features.contentHashCode()
        return result
    }
}

data class TemporalSequence(
    val userId: String,
    val features: List<TemporalFeature>
)

data class ConvolutionLayer(
    val inputSize: Int,
    val outputSize: Int,
    val dilation: Int,
    val weights: Array<Array<FloatArray>>, // [kernel][input][output]
    val bias: FloatArray
)

data class TemporalPattern(
    val userId: String,
    val representation: FloatArray,
    var confidence: Float,
    var sequenceCount: Int,
    var lastUpdated: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TemporalPattern
        return userId == other.userId
    }
    
    override fun hashCode(): Int = userId.hashCode()
}

data class AnomalyResult(
    val isAnomalous: Boolean,
    val score: Float,
    val description: String
)

data class TemporalModelStats(
    val isTrained: Boolean,
    val layerCount: Int,
    val sequenceLength: Int,
    val featureSize: Int,
    val userPatternCount: Int,
    val bufferSize: Int
)

