package com.example.bankingapp.ml

import android.util.Log
import kotlin.math.*

/**
 * Geolocation Weight Model for ABHED
 * 
 * Implements GPS clustering and dynamic weight adjustment based on location context.
 * Detects travel scenarios and adjusts trust in behavioral models accordingly.
 * 
 * Features:
 * - Home zone detection using DBSCAN clustering
 * - Travel detection (speed > 100 km/h or distance > 50km from home)
 * - Dynamic model weight adjustment based on location context
 * - Anomaly detection for impossible travel scenarios
 */
class GeolocationWeightModel {
    
    companion object {
        private const val TAG = "GeolocationWeightModel"
        private const val HOME_ZONE_RADIUS_KM = 5.0 // Home zone radius in kilometers
        private const val TRAVEL_DISTANCE_THRESHOLD_KM = 50.0 // Travel threshold
        private const val HIGH_SPEED_THRESHOLD_KMH = 100.0 // High speed travel threshold
        private const val MIN_LOCATION_POINTS = 10 // Minimum points for clustering
        private const val EARTH_RADIUS_KM = 6371.0 // Earth radius for distance calculations
    }
    
    private var isTrained = false
    private val locationHistory = mutableListOf<LocationPoint>()
    private var homeZone: LocationCluster? = null
    private val knownLocations = mutableListOf<LocationCluster>()
    private var currentLocation: LocationPoint? = null
    private var isInTransit = false
    private var currentSpeed = 0.0 // km/h
    
    /**
     * Train the geolocation model with historical location data
     */
    suspend fun train(userId: String, deviceUUID: String): Boolean {
        return try {
            Log.d(TAG, "Training geolocation model for user: $userId")
            
            // Generate training data (in production, this would come from database)
            val trainingData = generateLocationTrainingData()
            
            if (trainingData.size < MIN_LOCATION_POINTS) {
                Log.w(TAG, "Insufficient location data: ${trainingData.size} < $MIN_LOCATION_POINTS")
                return false
            }
            
            locationHistory.addAll(trainingData)
            
            // Perform location clustering to identify home zone and frequent locations
            performLocationClustering()
            
            // Identify home zone (most frequent cluster)
            identifyHomeZone()
            
            isTrained = true
            Log.d(TAG, "Geolocation model trained with ${trainingData.size} location points")
            Log.d(TAG, "Home zone: ${homeZone?.center}")
            Log.d(TAG, "Known locations: ${knownLocations.size}")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to train geolocation model: ${e.message}")
            false
        }
    }
    
    /**
     * Get confidence score for current location context
     */
    fun getConfidence(userId: String, deviceUUID: String): Float {
        if (!isTrained) {
            return 0.5f
        }
        
        return try {
            val location = currentLocation ?: return 0.5f
            
            // Calculate location familiarity
            val locationFamiliarity = calculateLocationFamiliarity(location)
            
            // Check for travel context
            val travelContext = analyzeTravelContext(location)
            
            // Calculate base confidence
            var confidence = locationFamiliarity
            
            // Adjust for travel context
            if (travelContext.isInTransit) {
                confidence *= 0.7f // Reduce confidence during travel
            }
            
            if (travelContext.isHighSpeed) {
                confidence *= 0.5f // Further reduce for high-speed travel
            }
            
            // Check for impossible travel (teleportation detection)
            if (detectImpossibleTravel(location)) {
                confidence = 0.1f // Very low confidence for impossible travel
            }
            
            Log.v(TAG, "Geolocation confidence: $confidence (familiarity: $locationFamiliarity, transit: ${travelContext.isInTransit})")
            confidence.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating geolocation confidence: ${e.message}")
            0.5f
        }
    }
    
    /**
     * Update model with new location data
     */
    fun updateModel(latitude: Double, longitude: Double) {
        try {
            val timestamp = System.currentTimeMillis()
            val newLocation = LocationPoint(latitude, longitude, timestamp)
            
            // Calculate speed if we have previous location
            currentLocation?.let { prevLocation ->
                val distance = calculateDistance(prevLocation, newLocation)
                val timeHours = (timestamp - prevLocation.timestamp) / (1000.0 * 3600.0)
                if (timeHours > 0) {
                    currentSpeed = distance / timeHours
                }
            }
            
            currentLocation = newLocation
            locationHistory.add(newLocation)
            
            // Keep only recent location history (sliding window)
            if (locationHistory.size > 1000) {
                locationHistory.removeAt(0)
            }
            
            // Update travel status
            updateTravelStatus(newLocation)
            
            // Incrementally update clusters if trained
            if (isTrained) {
                updateLocationClusters(newLocation)
            }
            
            Log.v(TAG, "Updated location: ($latitude, $longitude), speed: ${currentSpeed.roundToInt()} km/h")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating geolocation model: ${e.message}")
        }
    }
    
    /**
     * Get current dynamic model weights based on location context
     */
    fun getCurrentWeights(): ModelWeights {
        val baseWeights = ModelWeights(
            touchWeight = 0.6f,
            navigationWeight = 0.4f,
            geolocationWeight = 0.3f,
            siameseWeight = 0.5f,
            temporalWeight = 0.4f
        )
        
        if (!isTrained) return baseWeights
        
        // Adjust weights based on location context
        return when {
            isInTransit && currentSpeed > HIGH_SPEED_THRESHOLD_KMH -> {
                // High-speed travel: reduce touch and temporal weights
                baseWeights.copy(
                    touchWeight = 0.3f,
                    navigationWeight = 0.6f,
                    geolocationWeight = 0.7f,
                    siameseWeight = 0.4f,
                    temporalWeight = 0.2f
                )
            }
            isInTransit -> {
                // Normal travel: slightly reduce touch weight
                baseWeights.copy(
                    touchWeight = 0.5f,
                    navigationWeight = 0.5f,
                    geolocationWeight = 0.5f,
                    siameseWeight = 0.5f,
                    temporalWeight = 0.3f
                )
            }
            isInHomeZone() -> {
                // At home: increase touch and behavioral weights
                baseWeights.copy(
                    touchWeight = 0.7f,
                    navigationWeight = 0.4f,
                    geolocationWeight = 0.2f,
                    siameseWeight = 0.6f,
                    temporalWeight = 0.5f
                )
            }
            else -> baseWeights
        }
    }
    
    /**
     * Check if user is currently in home zone
     */
    fun isInHomeZone(): Boolean {
        val location = currentLocation ?: return false
        val home = homeZone ?: return false
        
        return calculateDistance(location, home.center) <= HOME_ZONE_RADIUS_KM
    }
    
    /**
     * Get travel status information
     */
    fun getTravelStatus(): TravelStatus {
        return TravelStatus(
            isInTransit = isInTransit,
            currentSpeed = currentSpeed,
            isInHomeZone = isInHomeZone(),
            distanceFromHome = homeZone?.let { home ->
                currentLocation?.let { location ->
                    calculateDistance(location, home.center)
                }
            } ?: 0.0
        )
    }
    
    // Private helper methods
    
    private fun performLocationClustering() {
        // Simple DBSCAN-like clustering for location points
        val clusters = mutableListOf<LocationCluster>()
        val visited = mutableSetOf<Int>()
        
        for (i in locationHistory.indices) {
            if (i in visited) continue
            
            val neighbors = findNeighbors(i, HOME_ZONE_RADIUS_KM)
            if (neighbors.size >= 3) { // Minimum points for a cluster
                val cluster = createCluster(neighbors)
                clusters.add(cluster)
                visited.addAll(neighbors)
            }
        }
        
        knownLocations.clear()
        knownLocations.addAll(clusters)
    }
    
    private fun findNeighbors(pointIndex: Int, radiusKm: Double): List<Int> {
        val neighbors = mutableListOf<Int>()
        val centerPoint = locationHistory[pointIndex]
        
        for (i in locationHistory.indices) {
            if (calculateDistance(centerPoint, locationHistory[i]) <= radiusKm) {
                neighbors.add(i)
            }
        }
        
        return neighbors
    }
    
    private fun createCluster(pointIndices: List<Int>): LocationCluster {
        val points = pointIndices.map { locationHistory[it] }
        val centerLat = points.map { it.latitude }.average()
        val centerLon = points.map { it.longitude }.average()
        val frequency = points.size
        
        return LocationCluster(
            center = LocationPoint(centerLat, centerLon, 0),
            radius = HOME_ZONE_RADIUS_KM,
            frequency = frequency,
            lastVisited = points.maxOfOrNull { it.timestamp } ?: 0L
        )
    }
    
    private fun identifyHomeZone() {
        // Home zone is the most frequently visited cluster
        homeZone = knownLocations.maxByOrNull { it.frequency }
    }
    
    private fun calculateLocationFamiliarity(location: LocationPoint): Float {
        if (knownLocations.isEmpty()) return 0.5f
        
        // Find the closest known location
        val closestCluster = knownLocations.minByOrNull { cluster ->
            calculateDistance(location, cluster.center)
        } ?: return 0.3f
        
        val distance = calculateDistance(location, closestCluster.center)
        
        return when {
            distance <= closestCluster.radius -> {
                // Inside known location
                val familiarity = (closestCluster.frequency / 100.0f).coerceAtMost(1.0f)
                0.7f + familiarity * 0.3f
            }
            distance <= closestCluster.radius * 2 -> {
                // Near known location
                0.6f
            }
            distance <= 10.0 -> {
                // Within 10km of known location
                0.4f
            }
            else -> {
                // Unknown location
                0.2f
            }
        }
    }
    
    private fun analyzeTravelContext(location: LocationPoint): TravelContext {
        val distanceFromHome = homeZone?.let { home ->
            calculateDistance(location, home.center)
        } ?: 0.0
        
        val isInTransit = distanceFromHome > TRAVEL_DISTANCE_THRESHOLD_KM
        val isHighSpeed = currentSpeed > HIGH_SPEED_THRESHOLD_KMH
        
        return TravelContext(
            isInTransit = isInTransit,
            isHighSpeed = isHighSpeed,
            distanceFromHome = distanceFromHome,
            currentSpeed = currentSpeed
        )
    }
    
    private fun detectImpossibleTravel(newLocation: LocationPoint): Boolean {
        val prevLocation = locationHistory.lastOrNull { it.timestamp < newLocation.timestamp }
            ?: return false
        
        val distance = calculateDistance(prevLocation, newLocation)
        val timeHours = (newLocation.timestamp - prevLocation.timestamp) / (1000.0 * 3600.0)
        
        if (timeHours <= 0) return false
        
        val speed = distance / timeHours
        
        // Flag as impossible if speed > 1000 km/h (faster than commercial aircraft)
        return speed > 1000.0
    }
    
    private fun updateTravelStatus(location: LocationPoint) {
        val distanceFromHome = homeZone?.let { home ->
            calculateDistance(location, home.center)
        } ?: 0.0
        
        isInTransit = distanceFromHome > TRAVEL_DISTANCE_THRESHOLD_KM || 
                     currentSpeed > HIGH_SPEED_THRESHOLD_KMH
    }
    
    private fun updateLocationClusters(newLocation: LocationPoint) {
        // Check if location belongs to existing cluster
        val nearbyCluster = knownLocations.find { cluster ->
            calculateDistance(newLocation, cluster.center) <= cluster.radius
        }
        
        if (nearbyCluster != null) {
            // Update cluster frequency and last visited time
            nearbyCluster.frequency++
            nearbyCluster.lastVisited = newLocation.timestamp
        } else if (locationHistory.count { 
            calculateDistance(it, newLocation) <= HOME_ZONE_RADIUS_KM 
        } >= 3) {
            // Create new cluster if we have enough nearby points
            val newCluster = LocationCluster(
                center = newLocation,
                radius = HOME_ZONE_RADIUS_KM,
                frequency = 1,
                lastVisited = newLocation.timestamp
            )
            knownLocations.add(newCluster)
        }
    }
    
    private fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLonRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
    
    private fun generateLocationTrainingData(): List<LocationPoint> {
        // Simulate typical location patterns for training
        // In production, this would come from actual GPS data
        val baseTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days ago
        val locations = mutableListOf<LocationPoint>()
        
        // Home location (most frequent)
        val homeLat = 12.9716
        val homeLon = 77.5946
        repeat(50) {
            locations.add(LocationPoint(
                latitude = homeLat + (Math.random() - 0.5) * 0.01,
                longitude = homeLon + (Math.random() - 0.5) * 0.01,
                timestamp = baseTime + it * 24 * 60 * 60 * 1000L
            ))
        }
        
        // Work location
        val workLat = 12.9352
        val workLon = 77.6245
        repeat(30) {
            locations.add(LocationPoint(
                latitude = workLat + (Math.random() - 0.5) * 0.005,
                longitude = workLon + (Math.random() - 0.5) * 0.005,
                timestamp = baseTime + it * 24 * 60 * 60 * 1000L + 8 * 60 * 60 * 1000L
            ))
        }
        
        // Occasional other locations
        repeat(10) {
            locations.add(LocationPoint(
                latitude = 12.9 + (Math.random() - 0.5) * 0.2,
                longitude = 77.6 + (Math.random() - 0.5) * 0.2,
                timestamp = baseTime + it * 3 * 24 * 60 * 60 * 1000L
            ))
        }
        
        return locations.sortedBy { it.timestamp }
    }
    
    /**
     * Reset model state
     */
    fun reset() {
        isTrained = false
        locationHistory.clear()
        knownLocations.clear()
        homeZone = null
        currentLocation = null
        isInTransit = false
        currentSpeed = 0.0
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): GeolocationModelStats {
        return GeolocationModelStats(
            isTrained = isTrained,
            locationHistorySize = locationHistory.size,
            knownLocationsCount = knownLocations.size,
            hasHomeZone = homeZone != null,
            isInTransit = isInTransit,
            currentSpeed = currentSpeed,
            isInHomeZone = isInHomeZone()
        )
    }
}

// Data classes

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class LocationCluster(
    val center: LocationPoint,
    val radius: Double,
    var frequency: Int,
    var lastVisited: Long
)

data class TravelContext(
    val isInTransit: Boolean,
    val isHighSpeed: Boolean,
    val distanceFromHome: Double,
    val currentSpeed: Double
)

data class TravelStatus(
    val isInTransit: Boolean,
    val currentSpeed: Double,
    val isInHomeZone: Boolean,
    val distanceFromHome: Double
)

data class GeolocationModelStats(
    val isTrained: Boolean,
    val locationHistorySize: Int,
    val knownLocationsCount: Int,
    val hasHomeZone: Boolean,
    val isInTransit: Boolean,
    val currentSpeed: Double,
    val isInHomeZone: Boolean
)

