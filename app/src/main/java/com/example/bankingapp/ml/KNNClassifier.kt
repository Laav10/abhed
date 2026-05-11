package com.example.bankingapp.ml

import android.util.Log
import kotlin.math.*

/**
 * k-Nearest Neighbors Classifier
 * 
 * Simple but effective classifier for behavioral authentication
 * Uses Euclidean distance to find similar behavioral patterns
 */
class KNNClassifier(private val k: Int) {
    
    companion object {
        private const val TAG = "KNNClassifier"
        private const val MIN_TRAINING_DATA = 10
    }
    
    // Training data
    private var trainingFeatures: List<BehavioralFeatureVector> = emptyList()
    private var isTrained = false
    
    /**
     * Train the kNN classifier
     */
    fun train(trainingData: List<BehavioralFeatureVector>): Boolean {
        return try {
            Log.d(TAG, "Training kNN classifier with k=$k and ${trainingData.size} training samples")
            
            if (trainingData.size < MIN_TRAINING_DATA) {
                Log.w(TAG, "Insufficient training data: ${trainingData.size} < $MIN_TRAINING_DATA")
                return false
            }
            
            trainingFeatures = trainingData
            isTrained = true
            
            Log.d(TAG, "kNN classifier trained successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error training kNN classifier: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score for a list of feature vectors
     */
    fun getConfidence(testFeatures: List<BehavioralFeatureVector>): Float {
        if (!isTrained || trainingFeatures.isEmpty() || testFeatures.isEmpty()) {
            return 0.5f
        }
        
        try {
            // Calculate confidence for each test feature
            val confidences = testFeatures.map { getSingleConfidence(it) }
            
            // Return average confidence
            val averageConfidence = confidences.average().toFloat()
            
            Log.d(TAG, "kNN confidence for ${testFeatures.size} features: $averageConfidence")
            return averageConfidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating kNN confidence: ${e.message}")
            return 0.5f
        }
    }
    
    /**
     * Get confidence score for a single feature vector
     */
    private fun getSingleConfidence(testFeature: BehavioralFeatureVector): Float {
        if (trainingFeatures.isEmpty()) return 0.5f
        
        try {
            // Calculate distances to all training samples
            val distances = trainingFeatures.map { trainingFeature ->
                val distance = calculateEuclideanDistance(testFeature, trainingFeature)
                DistancePair(distance, trainingFeature)
            }
            
            // Sort by distance (ascending)
            val sortedDistances = distances.sortedBy { it.distance }
            
            // Get k nearest neighbors
            val kNearest = sortedDistances.take(k)
            
            // Calculate confidence based on average distance
            val averageDistance = kNearest.map { it.distance }.average()
            
            // Convert distance to confidence (closer = higher confidence)
            // Normalize distance to 0-1 range and invert
            val maxDistance = sortedDistances.last().distance
            val normalizedDistance = if (maxDistance > 0) averageDistance / maxDistance else 0.0
            val confidence = (1.0 - normalizedDistance).toFloat()
            
            Log.v(TAG, "kNN: avg distance=$averageDistance, confidence=$confidence")
            
            return confidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating single kNN confidence: ${e.message}")
            return 0.5f
        }
    }
    
    /**
     * Calculate Euclidean distance between two feature vectors
     */
    private fun calculateEuclideanDistance(
        vector1: BehavioralFeatureVector,
        vector2: BehavioralFeatureVector
    ): Double {
        if (vector1.features.size != vector2.features.size) {
            Log.w(TAG, "Feature vector size mismatch: ${vector1.features.size} vs ${vector2.features.size}")
            return Double.MAX_VALUE
        }
        
        var sumSquaredDiff = 0.0
        for (i in vector1.features.indices) {
            val diff = vector1.features[i] - vector2.features[i]
            sumSquaredDiff += diff * diff
        }
        
        return sqrt(sumSquaredDiff)
    }
    
    /**
     * Calculate Manhattan distance (alternative distance metric)
     */
    private fun calculateManhattanDistance(
        vector1: BehavioralFeatureVector,
        vector2: BehavioralFeatureVector
    ): Double {
        if (vector1.features.size != vector2.features.size) {
            return Double.MAX_VALUE
        }
        
        var sumAbsDiff = 0.0
        for (i in vector1.features.indices) {
            sumAbsDiff += abs(vector1.features[i] - vector2.features[i])
        }
        
        return sumAbsDiff
    }
    
    /**
     * Calculate cosine similarity (alternative similarity metric)
     */
    private fun calculateCosineSimilarity(
        vector1: BehavioralFeatureVector,
        vector2: BehavioralFeatureVector
    ): Double {
        if (vector1.features.size != vector2.features.size) {
            return 0.0
        }
        
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in vector1.features.indices) {
            dotProduct += vector1.features[i] * vector2.features[i]
            norm1 += vector1.features[i] * vector1.features[i]
            norm2 += vector2.features[i] * vector2.features[i]
        }
        
        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }
    
    /**
     * Get k nearest neighbors with distances
     */
    fun getKNearestNeighbors(
        testFeature: BehavioralFeatureVector,
        k: Int = this.k
    ): List<NeighborResult> {
        if (!isTrained || trainingFeatures.isEmpty()) {
            return emptyList()
        }
        
        try {
            // Calculate distances to all training samples
            val distances = trainingFeatures.map { trainingFeature ->
                val distance = calculateEuclideanDistance(testFeature, trainingFeature)
                NeighborResult(trainingFeature, distance)
            }
            
            // Sort by distance and return k nearest
            return distances.sortedBy { it.distance }.take(k)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting k nearest neighbors: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Get confidence using different distance metrics
     */
    fun getConfidenceWithMetrics(testFeature: BehavioralFeatureVector): ConfidenceMetrics {
        if (!isTrained || trainingFeatures.isEmpty()) {
            return ConfidenceMetrics(0.5f, 0.5f, 0.5f)
        }
        
        try {
            // Get k nearest neighbors
            val kNearest = getKNearestNeighbors(testFeature)
            
            // Calculate confidence using different metrics
            val euclideanConfidence = calculateDistanceBasedConfidence(kNearest.map { it.distance })
            val manhattanConfidence = calculateManhattanBasedConfidence(testFeature)
            val cosineConfidence = calculateCosineBasedConfidence(testFeature)
            
            return ConfidenceMetrics(euclideanConfidence, manhattanConfidence, cosineConfidence)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating confidence with metrics: ${e.message}")
            return ConfidenceMetrics(0.5f, 0.5f, 0.5f)
        }
    }
    
    /**
     * Calculate confidence based on Euclidean distances
     */
    private fun calculateDistanceBasedConfidence(distances: List<Double>): Float {
        if (distances.isEmpty()) return 0.5f
        
        val averageDistance = distances.average()
        val maxDistance = distances.maxOrNull() ?: 1.0
        
        val normalizedDistance = if (maxDistance > 0) averageDistance / maxDistance else 0.0
        val confidence = (1.0 - normalizedDistance).toFloat()
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate confidence using Manhattan distance
     */
    private fun calculateManhattanBasedConfidence(testFeature: BehavioralFeatureVector): Float {
        val kNearest = getKNearestNeighbors(testFeature)
        val manhattanDistances = kNearest.map { neighbor ->
            calculateManhattanDistance(testFeature, neighbor.feature)
        }
        
        return calculateDistanceBasedConfidence(manhattanDistances)
    }
    
    /**
     * Calculate confidence using cosine similarity
     */
    private fun calculateCosineBasedConfidence(testFeature: BehavioralFeatureVector): Float {
        val kNearest = getKNearestNeighbors(testFeature)
        val similarities = kNearest.map { neighbor ->
            calculateCosineSimilarity(testFeature, neighbor.feature)
        }
        
        if (similarities.isEmpty()) return 0.5f
        
        // Convert similarity to confidence (similarity is already 0-1)
        val averageSimilarity = similarities.average()
        return averageSimilarity.toFloat().coerceIn(0f, 1f)
    }
    
    /**
     * Check if classifier is trained
     */
    fun isModelTrained(): Boolean = isTrained
    
    /**
     * Get training data size
     */
    fun getTrainingDataSize(): Int = trainingFeatures.size
    
    /**
     * Get k value
     */
    fun getK(): Int = k
}

// Data classes

data class DistancePair(
    val distance: Double,
    val feature: BehavioralFeatureVector
)

data class NeighborResult(
    val feature: BehavioralFeatureVector,
    val distance: Double
)

data class ConfidenceMetrics(
    val euclideanConfidence: Float,
    val manhattanConfidence: Float,
    val cosineConfidence: Float
)
