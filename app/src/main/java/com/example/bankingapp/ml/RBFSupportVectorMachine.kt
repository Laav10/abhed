package com.example.bankingapp.ml

import android.util.Log
import kotlin.math.*

/**
 * RBF (Radial Basis Function) Support Vector Machine
 * 
 * Simplified implementation for behavioral authentication
 * Uses RBF kernel for non-linear pattern recognition
 */
class RBFSupportVectorMachine(
    private val gamma: Float,
    private val C: Float
) {
    
    companion object {
        private const val TAG = "RBFSupportVectorMachine"
        private const val MIN_TRAINING_DATA = 20
        private const val MAX_ITERATIONS = 100
        private const val CONVERGENCE_THRESHOLD = 0.001f
    }
    
    // Model parameters
    private var supportVectors: List<BehavioralFeatureVector> = emptyList()
    private var alphaValues: FloatArray = floatArrayOf()
    private var bias: Float = 0f
    private var isTrained = false
    
    // Training data
    private var trainingFeatures: List<BehavioralFeatureVector> = emptyList()
    private var featureMeans: FloatArray = floatArrayOf()
    private var featureStds: FloatArray = floatArrayOf()
    
    /**
     * Train the RBF-SVM model
     */
    fun train(trainingData: List<BehavioralFeatureVector>): Boolean {
        return try {
            Log.d(TAG, "Training RBF-SVM with gamma=$gamma, C=$C, ${trainingData.size} samples")
            
            if (trainingData.size < MIN_TRAINING_DATA) {
                Log.w(TAG, "Insufficient training data: ${trainingData.size} < $MIN_TRAINING_DATA")
                return false
            }
            
            trainingFeatures = trainingData
            
            // Normalize features
            normalizeFeatures()
            
            // Train SVM using simplified SMO algorithm
            val trainingSuccess = trainSVM()
            
            if (trainingSuccess) {
                isTrained = true
                Log.d(TAG, "RBF-SVM trained successfully with ${supportVectors.size} support vectors")
            } else {
                Log.e(TAG, "Failed to train RBF-SVM")
            }
            
            trainingSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "Error training RBF-SVM: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score for a list of feature vectors
     */
    fun getConfidence(testFeatures: List<BehavioralFeatureVector>): Float {
        if (!isTrained || supportVectors.isEmpty()) {
            return 0.5f
        }
        
        try {
            // Normalize test features
            val normalizedFeatures = testFeatures.map { normalizeFeatureVector(it) }
            
            // Calculate confidence for each feature
            val confidences = normalizedFeatures.map { getSingleConfidence(it) }
            
            // Return average confidence
            val averageConfidence = confidences.average().toFloat()
            
            Log.d(TAG, "RBF-SVM confidence for ${testFeatures.size} features: $averageConfidence")
            return averageConfidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating RBF-SVM confidence: ${e.message}")
            return 0.5f
        }
    }
    
    /**
     * Update model with new data
     */
    fun updateModel(newData: List<BehavioralFeatureVector>) {
        if (!isTrained) return
        
        try {
            Log.d(TAG, "Updating RBF-SVM with ${newData.size} new data points")
            
            // Add new data to training set
            trainingFeatures = trainingFeatures + newData
            
            // Retrain the model
            normalizeFeatures()
            trainSVM()
            
            Log.d(TAG, "RBF-SVM updated successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating RBF-SVM: ${e.message}")
        }
    }
    
    /**
     * Normalize features using z-score normalization
     */
    private fun normalizeFeatures() {
        if (trainingFeatures.isEmpty()) return
        
        val featureCount = trainingFeatures[0].features.size
        
        // Calculate means and standard deviations
        featureMeans = FloatArray(featureCount)
        featureStds = FloatArray(featureCount)
        
        for (i in 0 until featureCount) {
            val values = trainingFeatures.map { it.features[i] }
            featureMeans[i] = values.average().toFloat()
            featureStds[i] = sqrt(values.map { (it - featureMeans[i]).pow(2) }.average()).toFloat()
        }
        
        // Normalize training features
        trainingFeatures = trainingFeatures.map { normalizeFeatureVector(it) }
    }
    
    /**
     * Normalize a single feature vector
     */
    private fun normalizeFeatureVector(vector: BehavioralFeatureVector): BehavioralFeatureVector {
        val normalizedFeatures = FloatArray(vector.features.size)
        
        for (i in vector.features.indices) {
            normalizedFeatures[i] = if (featureStds[i] > 0) {
                (vector.features[i] - featureMeans[i]) / featureStds[i]
            } else {
                vector.features[i] - featureMeans[i]
            }
        }
        
        return BehavioralFeatureVector(normalizedFeatures)
    }
    
    /**
     * Train SVM using simplified SMO (Sequential Minimal Optimization)
     */
    private fun trainSVM(): Boolean {
        try {
            val n = trainingFeatures.size
            
            // Initialize alpha values (Lagrange multipliers)
            alphaValues = FloatArray(n) { 0f }
            
            // Initialize bias
            bias = 0f
            
            // Simplified SMO training
            var iteration = 0
            var changed = true
            
            while (changed && iteration < MAX_ITERATIONS) {
                changed = false
                iteration++
                
                // Update alpha values
                for (i in 0 until n) {
                    val errorI = calculateError(i)
                    
                    if (shouldUpdateAlpha(errorI, i)) {
                        // Find second alpha to optimize
                        val j = selectSecondAlpha(i, errorI)
                        val errorJ = calculateError(j)
                        
                        // Update alpha values
                        val oldAlphaI = alphaValues[i]
                        val oldAlphaJ = alphaValues[j]
                        
                        val newAlphaI = calculateNewAlpha(i, j, errorI, errorJ)
                        val newAlphaJ = calculateNewAlpha(j, i, errorJ, errorI)
                        
                        // Apply constraints
                        val constrainedAlphaI = constrainAlpha(newAlphaI, i, j, oldAlphaJ)
                        val constrainedAlphaJ = constrainAlpha(newAlphaJ, j, i, oldAlphaI)
                        
                        // Update alpha values
                        alphaValues[i] = constrainedAlphaI
                        alphaValues[j] = constrainedAlphaJ
                        
                        // Update bias
                        updateBias(i, j, errorI, errorJ, oldAlphaI, oldAlphaJ, constrainedAlphaI, constrainedAlphaJ)
                        
                        changed = true
                    }
                }
                
                Log.v(TAG, "SMO iteration $iteration: changed=$changed")
            }
            
            // Extract support vectors (non-zero alpha values)
            extractSupportVectors()
            
            Log.d(TAG, "SVM training completed in $iteration iterations")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in SVM training: ${e.message}")
            return false
        }
    }
    
    /**
     * Calculate error for a training sample
     */
    private fun calculateError(index: Int): Float {
        val prediction = predict(trainingFeatures[index])
        val target = 1f // One-class SVM: all samples are positive
        return prediction - target
    }
    
    /**
     * Check if alpha should be updated
     */
    private fun shouldUpdateAlpha(error: Float, index: Int): Boolean {
        val alpha = alphaValues[index]
        val r = error
        
        return (r < -CONVERGENCE_THRESHOLD && alpha < C) ||
               (r > CONVERGENCE_THRESHOLD && alpha > 0f)
    }
    
    /**
     * Select second alpha for optimization
     */
    private fun selectSecondAlpha(firstIndex: Int, firstError: Float): Int {
        // Simple heuristic: find alpha with largest error difference
        var maxErrorDiff = 0f
        var secondIndex = (firstIndex + 1) % trainingFeatures.size
        
        for (i in trainingFeatures.indices) {
            if (i != firstIndex) {
                val errorDiff = abs(firstError - calculateError(i))
                if (errorDiff > maxErrorDiff) {
                    maxErrorDiff = errorDiff
                    secondIndex = i
                }
            }
        }
        
        return secondIndex
    }
    
    /**
     * Calculate new alpha value
     */
    private fun calculateNewAlpha(i: Int, j: Int, errorI: Float, errorJ: Float): Float {
        val kernelII = calculateRBFKernel(trainingFeatures[i], trainingFeatures[i])
        val kernelJJ = calculateRBFKernel(trainingFeatures[j], trainingFeatures[j])
        val kernelIJ = calculateRBFKernel(trainingFeatures[i], trainingFeatures[j])
        
        val eta = kernelII + kernelJJ - 2 * kernelIJ
        
        if (eta <= 0) return alphaValues[i]
        
        val newAlpha = alphaValues[i] + errorI / eta
        return newAlpha
    }
    
    /**
     * Apply alpha constraints
     */
    private fun constrainAlpha(alpha: Float, i: Int, j: Int, oldAlphaJ: Float): Float {
        val oldAlphaI = alphaValues[i]
        val sum = oldAlphaI + oldAlphaJ
        
        return alpha.coerceIn(0f, C)
    }
    
    /**
     * Update bias term
     */
    private fun updateBias(
        i: Int, j: Int,
        errorI: Float, errorJ: Float,
        oldAlphaI: Float, oldAlphaJ: Float,
        newAlphaI: Float, newAlphaJ: Float
    ) {
        val deltaAlphaI = newAlphaI - oldAlphaI
        val deltaAlphaJ = newAlphaJ - oldAlphaJ
        
        val kernelII = calculateRBFKernel(trainingFeatures[i], trainingFeatures[i])
        val kernelJJ = calculateRBFKernel(trainingFeatures[j], trainingFeatures[j])
        val kernelIJ = calculateRBFKernel(trainingFeatures[i], trainingFeatures[j])
        
        bias += deltaAlphaI * kernelII + deltaAlphaJ * kernelJJ + (deltaAlphaI + deltaAlphaJ) * kernelIJ
    }
    
    /**
     * Extract support vectors (samples with non-zero alpha values)
     */
    private fun extractSupportVectors() {
        val supportVectorIndices = alphaValues.indices.filter { alphaValues[it] > 0.001f }
        supportVectors = supportVectorIndices.map { trainingFeatures[it] }
        
        Log.d(TAG, "Extracted ${supportVectors.size} support vectors from ${trainingFeatures.size} training samples")
    }
    
    /**
     * Calculate RBF kernel
     */
    private fun calculateRBFKernel(
        vector1: BehavioralFeatureVector,
        vector2: BehavioralFeatureVector
    ): Float {
        val distance = calculateEuclideanDistance(vector1, vector2)
        return exp(-gamma * distance * distance).toFloat()
    }
    
    /**
     * Calculate Euclidean distance
     */
    private fun calculateEuclideanDistance(
        vector1: BehavioralFeatureVector,
        vector2: BehavioralFeatureVector
    ): Float {
        if (vector1.features.size != vector2.features.size) return Float.MAX_VALUE
        
        var sumSquaredDiff = 0f
        for (i in vector1.features.indices) {
            val diff = vector1.features[i] - vector2.features[i]
            sumSquaredDiff += diff * diff
        }
        
        return sqrt(sumSquaredDiff)
    }
    
    /**
     * Make prediction for a single feature vector
     */
    private fun predict(feature: BehavioralFeatureVector): Float {
        if (supportVectors.isEmpty()) return 0f
        
        var prediction = 0f
        
        for (i in supportVectors.indices) {
            val kernel = calculateRBFKernel(feature, supportVectors[i])
            prediction += alphaValues[i] * kernel
        }
        
        prediction += bias
        return prediction
    }
    
    /**
     * Get confidence for a single feature vector
     */
    private fun getSingleConfidence(feature: BehavioralFeatureVector): Float {
        val prediction = predict(feature)
        
        // Convert prediction to confidence (0-1 range)
        // For one-class SVM, higher prediction = higher confidence
        val confidence = (1.0 / (1.0 + exp(-prediction))).toFloat()
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Check if model is trained
     */
    fun isModelTrained(): Boolean = isTrained
    
    /**
     * Get number of support vectors
     */
    fun getSupportVectorCount(): Int = supportVectors.size
    
    /**
     * Get model parameters
     */
    fun getModelParameters(): SVMParameters {
        return SVMParameters(
            gamma = gamma,
            C = C,
            supportVectorCount = supportVectors.size,
            bias = bias
        )
    }
}

// Data classes

data class SVMParameters(
    val gamma: Float,
    val C: Float,
    val supportVectorCount: Int,
    val bias: Float
)




















