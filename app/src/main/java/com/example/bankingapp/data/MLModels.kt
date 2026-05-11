package com.example.bankingapp.data

/**
 * ML Model Compatible Stroke Format
 */
data class MLCompatibleStroke(
    val stroke: List<MLStrokePoint>,
    val dpi_x: Int = 400,
    val dpi_y: Int = 400,
    val phone_orientation: Int = 0,
    val phone_id: Int = 1
)

data class MLStrokePoint(
    val action: Int,           // 0=DOWN, 1=UP, 2=MOVE
    val time_ms: Long,         // Timestamp in milliseconds
    val x: Float,              // X coordinate
    val y: Float,              // Y coordinate
    val pressure: Float,       // Touch pressure (0.0 to 1.0)
    val area: Float = 0.0f,   // Touch area (set to 0.0)
    val orientation: Float = 0.0f  // Finger orientation (set to 0.0)
)

data class MLPrediction(
    val strokeId: String,
    val confidence: Float,
    val prediction: String,
    val timestamp: Long,
    val modelVersion: String,
    val processingTime: Long
)
