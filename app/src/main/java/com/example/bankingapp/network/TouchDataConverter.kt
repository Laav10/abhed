package com.example.bankingapp.network

import android.view.MotionEvent
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Touch Data Converter for ABHED Server Integration
 * 
 * Converts Android touch data and TouchDataPoint objects into the format
 * expected by the ABHED Flask server for behavioral authentication.
 */
object TouchDataConverter {
    
    /**
     * Convert a list of TouchDataPoint objects into server-compatible stroke format
     * @param touchPoints List of TouchDataPoint objects
     * @param startTime Base timestamp for the stroke
     * @return List of TouchPoint objects for server submission
     */
    fun convertTouchDataPointsToStroke(
        touchPoints: List<TouchDataPoint>,
        startTime: Long = System.currentTimeMillis()
    ): List<TouchPoint> {
        if (touchPoints.isEmpty()) return emptyList()
        
        val stroke = mutableListOf<TouchPoint>()
        var currentTime = startTime
        
        // Sort touch points by timestamp
        val sortedPoints = touchPoints.sortedBy { it.timestamp }
        
        // Add ACTION_DOWN point
        val firstPoint = sortedPoints.first()
        stroke.add(
            TouchPoint(
                timeMs = currentTime,
                action = 0, // ACTION_DOWN
                x = firstPoint.x,
                y = firstPoint.y,
                pressure = firstPoint.pressure,
                area = 0.1f, // Default area if not available
                fingerOrientation = 0.0f // Default orientation
            )
        )
        
        // Add ACTION_MOVE points for intermediate points
        if (sortedPoints.size > 2) {
            for (i in 1 until sortedPoints.size - 1) {
                currentTime += 50 // 50ms intervals between points
                val point = sortedPoints[i]
                stroke.add(
                    TouchPoint(
                        timeMs = currentTime,
                        action = 2, // ACTION_MOVE
                        x = point.x,
                        y = point.y,
                        pressure = point.pressure,
                        area = 0.1f,
                        fingerOrientation = 0.0f
                    )
                )
            }
        }
        
        // Add ACTION_UP point
        currentTime += 50
        val lastPoint = sortedPoints.last()
        stroke.add(
            TouchPoint(
                timeMs = currentTime,
                action = 1, // ACTION_UP
                x = lastPoint.x,
                y = lastPoint.y,
                pressure = lastPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
        
        return stroke
    }
    
    /**
     * Convert a single TouchDataPoint into a simple stroke (tap gesture)
     * @param touchPoint Single TouchDataPoint
     * @param startTime Base timestamp
     * @param duration Stroke duration in milliseconds
     * @return List of TouchPoint objects representing a tap
     */
    fun convertSingleTouchToStroke(
        touchPoint: TouchDataPoint,
        startTime: Long = System.currentTimeMillis(),
        duration: Long = 100L
    ): List<TouchPoint> {
        return listOf(
            TouchPoint(
                timeMs = startTime,
                action = 0, // ACTION_DOWN
                x = touchPoint.x,
                y = touchPoint.y,
                pressure = touchPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            ),
            TouchPoint(
                timeMs = startTime + duration,
                action = 1, // ACTION_UP
                x = touchPoint.x,
                y = touchPoint.y,
                pressure = touchPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
    }
    
    /**
     * Convert swipe gesture data into server-compatible stroke format
     * @param startPoint Starting touch point
     * @param endPoint Ending touch point
     * @param intermediatePoints Optional intermediate points for smooth swipes
     * @param startTime Base timestamp
     * @param duration Total swipe duration
     * @return List of TouchPoint objects representing a swipe
     */
    fun convertSwipeToStroke(
        startPoint: TouchDataPoint,
        endPoint: TouchDataPoint,
        intermediatePoints: List<TouchDataPoint> = emptyList(),
        startTime: Long = System.currentTimeMillis(),
        duration: Long = 300L
    ): List<TouchPoint> {
        val stroke = mutableListOf<TouchPoint>()
        val totalPoints = 2 + intermediatePoints.size
        val timeInterval = duration / (totalPoints - 1)
        
        // Add ACTION_DOWN
        stroke.add(
            TouchPoint(
                timeMs = startTime,
                action = 0,
                x = startPoint.x,
                y = startPoint.y,
                pressure = startPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
        
        // Add intermediate ACTION_MOVE points
        intermediatePoints.forEachIndexed { index, point ->
            stroke.add(
                TouchPoint(
                    timeMs = startTime + (index + 1) * timeInterval,
                    action = 2,
                    x = point.x,
                    y = point.y,
                    pressure = point.pressure,
                    area = 0.1f,
                    fingerOrientation = 0.0f
                )
            )
        }
        
        // Add ACTION_UP
        stroke.add(
            TouchPoint(
                timeMs = startTime + duration,
                action = 1,
                x = endPoint.x,
                y = endPoint.y,
                pressure = endPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
        
        return stroke
    }
    
    /**
     * Convert long press gesture into server-compatible stroke format
     * @param touchPoint Touch point for long press
     * @param startTime Base timestamp
     * @param duration Long press duration (typically 2000ms)
     * @return List of TouchPoint objects representing a long press
     */
    fun convertLongPressToStroke(
        touchPoint: TouchDataPoint,
        startTime: Long = System.currentTimeMillis(),
        duration: Long = 2000L
    ): List<TouchPoint> {
        val stroke = mutableListOf<TouchPoint>()
        val numPoints = 10 // Create 10 intermediate points for smooth long press
        val timeInterval = duration / numPoints
        
        // Add ACTION_DOWN
        stroke.add(
            TouchPoint(
                timeMs = startTime,
                action = 0,
                x = touchPoint.x,
                y = touchPoint.y,
                pressure = touchPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
        
        // Add intermediate ACTION_MOVE points with slight pressure variation
        for (i in 1 until numPoints) {
            val pressureVariation = 0.1f * kotlin.math.sin(i * 0.5) // Slight pressure variation
            stroke.add(
                TouchPoint(
                    timeMs = startTime + i * timeInterval,
                    action = 2,
                    x = touchPoint.x + (kotlin.random.Random.nextFloat() * 2 - 1), // Slight position variation
                    y = touchPoint.y + (kotlin.random.Random.nextFloat() * 2 - 1),
                    pressure = (touchPoint.pressure + pressureVariation).toFloat().coerceIn(0.0f, 1.0f),
                    area = 0.1f,
                    fingerOrientation = 0.0f
                )
            )
        }
        
        // Add ACTION_UP
        stroke.add(
            TouchPoint(
                timeMs = startTime + duration,
                action = 1,
                x = touchPoint.x,
                y = touchPoint.y,
                pressure = touchPoint.pressure,
                area = 0.1f,
                fingerOrientation = 0.0f
            )
        )
        
        return stroke
    }
    
    /**
     * Convert Android MotionEvent into server-compatible stroke format
     * @param motionEvent Android MotionEvent
     * @param startTime Base timestamp
     * @return List of TouchPoint objects
     */
    fun convertMotionEventToStroke(
        motionEvent: MotionEvent,
        startTime: Long = System.currentTimeMillis()
    ): List<TouchPoint> {
        val stroke = mutableListOf<TouchPoint>()
        val pointerCount = motionEvent.pointerCount
        
        for (i in 0 until pointerCount) {
            val action = when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 1
                MotionEvent.ACTION_MOVE -> 2
                else -> 2
            }
            
            stroke.add(
                TouchPoint(
                    timeMs = startTime + (i * 10), // Small time offset for multiple pointers
                    action = action,
                    x = motionEvent.getX(i),
                    y = motionEvent.getY(i),
                    pressure = motionEvent.getPressure(i),
                    area = motionEvent.getSize(i),
                    fingerOrientation = 0.0f // Not available in MotionEvent
                )
            )
        }
        
        return stroke
    }
    
    /**
     * Create a stroke from gesture data with proper timing and pressure patterns
     * @param gestureType Type of gesture (TAP, SWIPE, LONG_PRESS, etc.)
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate (for swipes)
     * @param endY Ending Y coordinate (for swipes)
     * @param pressure Base pressure value
     * @param startTime Base timestamp
     * @return List of TouchPoint objects
     */
    fun createStrokeFromGesture(
        gestureType: TouchType,
        startX: Float,
        startY: Float,
        endX: Float = startX,
        endY: Float = startY,
        pressure: Float = 0.5f,
        startTime: Long = System.currentTimeMillis()
    ): List<TouchPoint> {
        return when (gestureType) {
            TouchType.TAP -> {
                listOf(
                    TouchPoint(startTime, 0, startX, startY, pressure, 0.1f, 0.0f),
                    TouchPoint(startTime + 100, 1, startX, startY, pressure, 0.1f, 0.0f)
                )
            }
            TouchType.LONG_PRESS -> {
                convertLongPressToStroke(
                    TouchDataPoint(startX, startY, pressure, startTime, TouchType.LONG_PRESS),
                    startTime,
                    2000L
                )
            }
            TouchType.SWIPE, TouchType.SWIPE_LEFT, TouchType.SWIPE_RIGHT, 
            TouchType.SWIPE_UP, TouchType.SWIPE_DOWN -> {
                val distance = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
                val duration = (distance * 2).toLong().coerceIn(100L, 500L) // Duration based on distance
                
                // Create intermediate points for smooth swipe
                val numPoints = 5
                val intermediatePoints = mutableListOf<TouchPoint>()
                
                for (i in 1 until numPoints) {
                    val ratio = i.toFloat() / numPoints
                    val x = startX + (endX - startX) * ratio
                    val y = startY + (endY - startY) * ratio
                    val time = startTime + (duration * ratio).toLong()
                    
                    intermediatePoints.add(
                        TouchPoint(time, 2, x, y, pressure, 0.1f, 0.0f)
                    )
                }
                
                listOf(
                    TouchPoint(startTime, 0, startX, startY, pressure, 0.1f, 0.0f)
                ) + intermediatePoints + listOf(
                    TouchPoint(startTime + duration, 1, endX, endY, pressure, 0.1f, 0.0f)
                )
            }
            else -> {
                // Default to tap for unknown gesture types
                listOf(
                    TouchPoint(startTime, 0, startX, startY, pressure, 0.1f, 0.0f),
                    TouchPoint(startTime + 100, 1, startX, startY, pressure, 0.1f, 0.0f)
                )
            }
        }
    }
    
    /**
     * Calculate swipe direction from coordinates
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @return TouchType representing the swipe direction
     */
    fun calculateSwipeDirection(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): TouchType {
        val deltaX = endX - startX
        val deltaY = endY - startY
        val angle = atan2(deltaY, deltaX)
        val degrees = Math.toDegrees(angle.toDouble())
        
        return when {
            degrees >= -45 && degrees < 45 -> TouchType.SWIPE_RIGHT
            degrees >= 45 && degrees < 135 -> TouchType.SWIPE_DOWN
            degrees >= 135 || degrees < -135 -> TouchType.SWIPE_LEFT
            degrees >= -135 && degrees < -45 -> TouchType.SWIPE_UP
            else -> TouchType.SWIPE
        }
    }
}
