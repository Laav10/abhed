package com.example.bankingapp.ml

import android.util.Log
import com.example.bankingapp.data.models.TouchDataPoint
import kotlin.math.*

/**
 * Siamese Behavior Model for ABHED
 * 
 * Implements a Siamese Neural Network for behavioral biometric authentication.
 * Uses triplet loss to learn behavioral embeddings and enable few-shot learning
 * for passwordless authentication through behavior matching.
 * 
 * Features:
 * - Behavioral embedding generation
 * - Triplet loss-based similarity learning
 * - Few-shot adaptation for new users
 * - Real-time behavioral matching
 * - Privacy-preserving on-device processing
 */
class SiameseBehaviorModel {
    
    companion object {
        private const val TAG = "SiameseBehaviorModel"
        private const val EMBEDDING_SIZE = 128
        private const val MIN_TRAINING_SAMPLES = 10
        private const val SIMILARITY_THRESHOLD = 0.7f
        private const val TRIPLET_MARGIN = 0.2f
        private const val LEARNING_RATE = 0.001f
    }
    
    private var isTrained = false
    private val userEmbeddings = mutableMapOf<String, BehavioralEmbedding>()
    private val networkWeights = initializeNetworkWeights()
    private val trainingHistory = mutableListOf<TrainingExample>()
    
    /**
     * Train the Siamese model with user behavioral data
     */
    suspend fun train(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training Siamese model for user: $userId")
            
            // Generate training data from behavioral patterns
            val trainingData = generateBehavioralTrainingData(userId)
            
            if (trainingData.size < MIN_TRAINING_SAMPLES) {
                Log.w(TAG, "Insufficient training data: ${trainingData.size} < $MIN_TRAINING_SAMPLES")
                return false
            }
            
            // Create triplets for training
            val triplets = createTriplets(trainingData, userId)
            
            // Train the network using triplet loss
            trainWithTripletLoss(triplets)
            
            // Generate user embedding
            val userEmbedding = generateUserEmbedding(trainingData, userId)
            userEmbeddings[userId] = userEmbedding
            
            isTrained = true
            Log.d(TAG, "Siamese model trained with ${trainingData.size} samples")
            Log.d(TAG, "Generated embedding for user: $userId")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to train Siamese model: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score by comparing current behavior with stored embeddings
     */
    fun getConfidence(recentTouchData: List<TouchDataPoint>): Float {
        if (!isTrained || recentTouchData.isEmpty()) {
            return 0.5f
        }
        
        return try {
            // Extract behavioral features from recent touch data
            val currentFeatures = extractBehavioralFeatures(recentTouchData)
            
            // Generate embedding for current behavior
            val currentEmbedding = generateEmbedding(currentFeatures)
            
            // Find best matching user embedding
            val bestMatch = findBestMatch(currentEmbedding)
            
            val confidence = if (bestMatch != null) {
                calculateSimilarity(currentEmbedding, bestMatch.embedding)
            } else {
                0.3f // Low confidence if no match found
            }
            
            Log.v(TAG, "Siamese confidence: $confidence")
            confidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating Siamese confidence: ${e.message}")
            0.5f
        }
    }
    
    /**
     * Update model with new behavioral data (incremental learning)
     */
    fun updateModel(behaviorData: List<TouchDataPoint>) {
        if (!isTrained || behaviorData.isEmpty()) return
        
        try {
            // Extract features from new data
            val newFeatures = extractBehavioralFeatures(behaviorData)
            
            // Generate embedding for new behavior
            val newEmbedding = generateEmbedding(newFeatures)
            
            // Update user embeddings with exponential moving average
            userEmbeddings.values.forEach { userEmbedding ->
                val similarity = calculateSimilarity(newEmbedding, userEmbedding.embedding)
                if (similarity > SIMILARITY_THRESHOLD) {
                    updateUserEmbedding(userEmbedding, newEmbedding)
                }
            }
            
            Log.v(TAG, "Updated Siamese model with ${behaviorData.size} new data points")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Siamese model: ${e.message}")
        }
    }
    
    /**
     * Perform few-shot learning for new user
     */
    fun performFewShotLearning(userId: String, samples: List<TouchDataPoint>): Boolean {
        if (samples.size < 3) {
            Log.w(TAG, "Insufficient samples for few-shot learning: ${samples.size}")
            return false
        }
        
        return try {
            // Extract features from few samples
            val features = extractBehavioralFeatures(samples)
            
            // Generate initial embedding
            val initialEmbedding = generateEmbedding(features)
            
            // Create user embedding with low confidence initially
            val userEmbedding = BehavioralEmbedding(
                userId = userId,
                embedding = initialEmbedding,
                confidence = 0.6f,
                sampleCount = samples.size,
                lastUpdated = System.currentTimeMillis()
            )
            
            userEmbeddings[userId] = userEmbedding
            
            Log.d(TAG, "Few-shot learning completed for user: $userId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in few-shot learning: ${e.message}")
            false
        }
    }
    
    /**
     * Verify user identity using behavioral embedding
     */
    fun verifyIdentity(userId: String, behaviorSample: List<TouchDataPoint>): VerificationResult {
        val userEmbedding = userEmbeddings[userId]
        if (userEmbedding == null) {
            return VerificationResult(false, 0f, "User embedding not found")
        }
        
        return try {
            val sampleFeatures = extractBehavioralFeatures(behaviorSample)
            val sampleEmbedding = generateEmbedding(sampleFeatures)
            
            val similarity = calculateSimilarity(sampleEmbedding, userEmbedding.embedding)
            val isVerified = similarity > SIMILARITY_THRESHOLD
            
            VerificationResult(
                isVerified = isVerified,
                confidence = similarity,
                message = if (isVerified) "Identity verified" else "Identity verification failed"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying identity: ${e.message}")
            VerificationResult(false, 0f, "Verification error: ${e.message}")
        }
    }
    
    // Private helper methods
    
    private fun extractBehavioralFeatures(touchData: List<TouchDataPoint>): FloatArray {
        if (touchData.isEmpty()) return FloatArray(20) { 0f }
        
        // Extract comprehensive behavioral features
        val features = mutableListOf<Float>()
        
        // Pressure statistics
        val pressures = touchData.map { it.pressure }
        features.add(pressures.average().toFloat())
        features.add(pressures.maxOrNull() ?: 0f)
        features.add(pressures.minOrNull() ?: 0f)
        features.add(calculateStandardDeviation(pressures))
        
        // Timing patterns
        val timingData = touchData.zipWithNext { a, b -> b.timestamp - a.timestamp }
        if (timingData.isNotEmpty()) {
            val intervals = timingData.map { it.toFloat() }
            features.add(intervals.average().toFloat())
            features.add(intervals.maxOrNull() ?: 0f)
            features.add(intervals.minOrNull() ?: 0f)
            features.add(calculateStandardDeviation(intervals).toFloat())
        }
        
        // Velocity statistics
        val velocities = touchData.mapNotNull { it.velocity }
        if (velocities.isNotEmpty()) {
            features.add(velocities.average().toFloat())
            features.add(velocities.maxOrNull() ?: 0f)
            features.add(calculateStandardDeviation(velocities))
        } else {
            features.addAll(listOf(0f, 0f, 0f))
        }
        
        // Timing patterns
        val timestamps = touchData.map { it.timestamp }
        if (timestamps.size > 1) {
            val intervals = timestamps.zipWithNext { a, b -> (b - a).toFloat() }
            features.add(intervals.average().toFloat())
            features.add(calculateStandardDeviation(intervals))
        } else {
            features.addAll(listOf(0f, 0f))
        }
        
        // Spatial patterns
        val xCoords = touchData.map { it.x }
        val yCoords = touchData.map { it.y }
        features.add(xCoords.average().toFloat())
        features.add(yCoords.average().toFloat())
        features.add(calculateStandardDeviation(xCoords))
        features.add(calculateStandardDeviation(yCoords))
        
        // Touch type distribution
        val touchTypes = touchData.groupBy { it.type }.mapValues { it.value.size.toFloat() }
        features.add(touchTypes.values.maxOrNull() ?: 0f)
        features.add(touchTypes.size.toFloat())

        // Movement patterns
        if (touchData.size > 1) {
            val movements = touchData.zipWithNext { a, b ->
                // compute as Double then convert to Float
                val dx = (b.x - a.x).toDouble()
                val dy = (b.y - a.y).toDouble()
                kotlin.math.sqrt(dx * dx + dy * dy).toFloat()
            }
            // movements is List<Float>, so average() returns Double -> convert
            features.add(movements.average().toFloat())
            features.add(movements.maxOrNull() ?: 0f)
        } else {
            features.addAll(listOf(0f, 0f))
        }


        // Pad or truncate to fixed size
        return features.take(20).toFloatArray().let { array ->
            if (array.size < 20) {
                array + FloatArray(20 - array.size) { 0f }
            } else array
        }
    }
    
    private fun generateEmbedding(features: FloatArray): FloatArray {
        // Simplified neural network forward pass
        // In production, this would use TensorFlow Lite
        
        var layer1 = features
        
        // First hidden layer (20 -> 64)
        layer1 = applyLayer(layer1, networkWeights.layer1Weights, networkWeights.layer1Bias)
        layer1 = applyReLU(layer1)
        
        // Second hidden layer (64 -> 128)
        var layer2 = applyLayer(layer1, networkWeights.layer2Weights, networkWeights.layer2Bias)
        layer2 = applyReLU(layer2)
        
        // Output layer (128 -> EMBEDDING_SIZE)
        var embedding = applyLayer(layer2, networkWeights.outputWeights, networkWeights.outputBias)
        
        // L2 normalization
        embedding = normalizeL2(embedding)
        
        return embedding
    }
    
    private fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f
        
        // Cosine similarity
        val dotProduct = embedding1.zip(embedding2) { a, b -> a * b }.sum()
        val norm1 = sqrt(embedding1.map { it * it }.sum())
        val norm2 = sqrt(embedding2.map { it * it }.sum())
        
        return if (norm1 > 0 && norm2 > 0) {
            (dotProduct / (norm1 * norm2)).coerceIn(-1f, 1f)
        } else 0f
    }
    
    private fun createTriplets(trainingData: List<BehavioralSample>, userId: String): List<Triplet> {
        val triplets = mutableListOf<Triplet>()
        val userSamples = trainingData.filter { it.userId == userId }
        val otherSamples = trainingData.filter { it.userId != userId }
        
        // Create positive and negative pairs
        for (i in userSamples.indices) {
            for (j in i + 1 until userSamples.size) {
                for (negative in otherSamples.take(3)) { // Limit negatives
                    triplets.add(
                        Triplet(
                            anchor = userSamples[i],
                            positive = userSamples[j],
                            negative = negative
                        )
                    )
                }
            }
        }
        
        return triplets
    }
    
    private fun trainWithTripletLoss(triplets: List<Triplet>) {
        // Simplified triplet loss training
        // In production, this would use proper gradient descent
        
        for (triplet in triplets) {
            val anchorEmbedding = generateEmbedding(triplet.anchor.features)
            val positiveEmbedding = generateEmbedding(triplet.positive.features)
            val negativeEmbedding = generateEmbedding(triplet.negative.features)
            
            val positiveDistance = calculateDistance(anchorEmbedding, positiveEmbedding)
            val negativeDistance = calculateDistance(anchorEmbedding, negativeEmbedding)
            
            val loss = maxOf(0f, positiveDistance - negativeDistance + TRIPLET_MARGIN)
            
            // Store training example for analysis
            trainingHistory.add(
                TrainingExample(
                    loss = loss,
                    positiveDistance = positiveDistance,
                    negativeDistance = negativeDistance,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun generateUserEmbedding(trainingData: List<BehavioralSample>, userId: String): BehavioralEmbedding {
        val userSamples = trainingData.filter { it.userId == userId }
        val embeddings = userSamples.map { generateEmbedding(it.features) }
        
        // Average embeddings
        val avgEmbedding = FloatArray(EMBEDDING_SIZE) { i ->
            embeddings.map { it[i] }.average().toFloat()
        }
        
        return BehavioralEmbedding(
            userId = userId,
            embedding = normalizeL2(avgEmbedding),
            confidence = 0.8f,
            sampleCount = userSamples.size,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun findBestMatch(embedding: FloatArray): BehavioralEmbedding? {
        return userEmbeddings.values.maxByOrNull { userEmbedding ->
            calculateSimilarity(embedding, userEmbedding.embedding)
        }
    }
    
    private fun updateUserEmbedding(userEmbedding: BehavioralEmbedding, newEmbedding: FloatArray) {
        val alpha = 0.1f // Learning rate for exponential moving average
        
        for (i in userEmbedding.embedding.indices) {
            userEmbedding.embedding[i] = (1 - alpha) * userEmbedding.embedding[i] + alpha * newEmbedding[i]
        }
        
        userEmbedding.lastUpdated = System.currentTimeMillis()
        userEmbedding.sampleCount++
    }
    
    private fun generateBehavioralTrainingData(userId: String): List<BehavioralSample> {
        // Simulate training data generation
        // In production, this would come from actual user data
        val samples = mutableListOf<BehavioralSample>()
        
        repeat(20) {
            val features = FloatArray(20) { (Math.random() * 2 - 1).toFloat() }
            samples.add(BehavioralSample(userId, features))
        }
        
        // Add some negative samples
        repeat(10) {
            val features = FloatArray(20) { (Math.random() * 2 - 1).toFloat() }
            samples.add(BehavioralSample("other_user_$it", features))
        }
        
        return samples
    }
    
    // Utility functions
    
    private fun calculateStandardDeviation(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
    
    private fun calculateDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        return sqrt(embedding1.zip(embedding2) { a, b -> (a - b).pow(2) }.sum())
    }
    
    private fun applyLayer(input: FloatArray, weights: Array<FloatArray>, bias: FloatArray): FloatArray {
        val output = FloatArray(bias.size)
        for (i in output.indices) {
            output[i] = bias[i]
            for (j in input.indices) {
                output[i] += input[j] * weights[j][i]
            }
        }
        return output
    }
    
    private fun applyReLU(input: FloatArray): FloatArray {
        return input.map { maxOf(0f, it) }.toFloatArray()
    }
    
    private fun normalizeL2(input: FloatArray): FloatArray {
        val norm = sqrt(input.map { it * it }.sum())
        return if (norm > 0) input.map { it / norm }.toFloatArray() else input
    }
    
    private fun initializeNetworkWeights(): NetworkWeights {
        return NetworkWeights(
            layer1Weights = Array(20) { FloatArray(64) { (Math.random() * 2 - 1).toFloat() * 0.1f } },
            layer1Bias = FloatArray(64) { 0f },
            layer2Weights = Array(64) { FloatArray(128) { (Math.random() * 2 - 1).toFloat() * 0.1f } },
            layer2Bias = FloatArray(128) { 0f },
            outputWeights = Array(128) { FloatArray(EMBEDDING_SIZE) { (Math.random() * 2 - 1).toFloat() * 0.1f } },
            outputBias = FloatArray(EMBEDDING_SIZE) { 0f }
        )
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): SiameseModelStats {
        return SiameseModelStats(
            isTrained = isTrained,
            userCount = userEmbeddings.size,
            trainingExamples = trainingHistory.size,
            averageLoss = if (trainingHistory.isNotEmpty()) {
                trainingHistory.map { it.loss }.average().toFloat()
            } else 0f,
            embeddingSize = EMBEDDING_SIZE
        )
    }
    
    /**
     * Reset model state
     */
    fun reset() {
        isTrained = false
        userEmbeddings.clear()
        trainingHistory.clear()
    }
}

// Data classes

data class BehavioralEmbedding(
    val userId: String,
    val embedding: FloatArray,
    var confidence: Float,
    var sampleCount: Int,
    var lastUpdated: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BehavioralEmbedding
        return userId == other.userId
    }
    
    override fun hashCode(): Int = userId.hashCode()
}

data class BehavioralSample(
    val userId: String,
    val features: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BehavioralSample
        return userId == other.userId && features.contentEquals(other.features)
    }
    
    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + features.contentHashCode()
        return result
    }
}

data class Triplet(
    val anchor: BehavioralSample,
    val positive: BehavioralSample,
    val negative: BehavioralSample
)

data class TrainingExample(
    val loss: Float,
    val positiveDistance: Float,
    val negativeDistance: Float,
    val timestamp: Long
)

data class VerificationResult(
    val isVerified: Boolean,
    val confidence: Float,
    val message: String
)

data class NetworkWeights(
    val layer1Weights: Array<FloatArray>,
    val layer1Bias: FloatArray,
    val layer2Weights: Array<FloatArray>,
    val layer2Bias: FloatArray,
    val outputWeights: Array<FloatArray>,
    val outputBias: FloatArray
)

data class SiameseModelStats(
    val isTrained: Boolean,
    val userCount: Int,
    val trainingExamples: Int,
    val averageLoss: Float,
    val embeddingSize: Int
)

