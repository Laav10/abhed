package com.example.bankingapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Touch Data
 * 
 * Provides database operations for ABHED behavioral authentication:
 * - Insert touch data points
 * - Query behavioral patterns
 * - Analyze user behavior
 * - Support ML model training
 */
@Dao
interface TouchDataDao {
    
    // Basic CRUD operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTouchData(touchData: TouchDataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTouchDataList(touchDataList: List<TouchDataEntity>)
    
    @Update
    suspend fun updateTouchData(touchData: TouchDataEntity)
    
    @Delete
    suspend fun deleteTouchData(touchData: TouchDataEntity)
    
    @Query("DELETE FROM touch_data WHERE id = :id")
    suspend fun deleteTouchDataById(id: Long)
    
    // Query operations
    
    @Query("SELECT * FROM touch_data WHERE id = :id")
    suspend fun getTouchDataById(id: Long): TouchDataEntity?
    
    @Query("SELECT * FROM touch_data ORDER BY timestamp DESC")
    fun getAllTouchData(): Flow<List<TouchDataEntity>>
    
    @Query("SELECT * FROM touch_data WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTouchDataByUser(userId: String): Flow<List<TouchDataEntity>>
    
    @Query("SELECT * FROM touch_data WHERE deviceUUID = :deviceUUID ORDER BY timestamp DESC")
    fun getTouchDataByDevice(deviceUUID: String): Flow<List<TouchDataEntity>>
    
    @Query("SELECT * FROM touch_data WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getTouchDataBySession(sessionId: String): Flow<List<TouchDataEntity>>
    
    @Query("SELECT * FROM touch_data WHERE type = :type ORDER BY timestamp DESC")
    fun getTouchDataByType(type: String): Flow<List<TouchDataEntity>>
    
    // Behavioral analysis queries
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime 
        AND timestamp <= :endTime 
        ORDER BY timestamp ASC
    """)
    fun getTouchDataInTimeRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TouchDataEntity>>
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND type = :type
        AND timestamp >= :startTime 
        AND timestamp <= :endTime 
        ORDER BY timestamp ASC
    """)
    fun getTouchDataByTypeInTimeRange(
        userId: String,
        type: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TouchDataEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = :type
        AND timestamp >= :startTime
    """)
    suspend fun getTouchCountByType(
        userId: String,
        type: String,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT AVG(pressure) FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
    """)
    suspend fun getAveragePressure(
        userId: String,
        startTime: Long
    ): Float?
    
    @Query("""
        SELECT AVG(velocity) FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
    """)
    suspend fun getAverageVelocity(
        userId: String,
        startTime: Long
    ): Float?
    
    // Navigation pattern analysis
    
    @Query("""
        SELECT additionalData FROM touch_data 
        WHERE userId = :userId 
        AND type = 'NAVIGATION'
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getNavigationPattern(
        userId: String,
        startTime: Long
    ): Flow<List<String>>
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'NAVIGATION'
        AND additionalData LIKE '%' || :screenName || '%'
        AND timestamp >= :startTime
    """)
    suspend fun getScreenVisitCount(
        userId: String,
        screenName: String,
        startTime: Long
    ): Int
    
    // Keystroke dynamics analysis
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND type = 'KEYSTROKE'
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getKeystrokeData(
        userId: String,
        startTime: Long
    ): Flow<List<TouchDataEntity>>
    
    @Query("""
        SELECT AVG(CAST(SUBSTR(additionalData, INSTR(additionalData, 'interKeyDelay=') + 15, 
                          INSTR(SUBSTR(additionalData, INSTR(additionalData, 'interKeyDelay=') + 15), ';') - 1) AS INTEGER))
        FROM touch_data 
        WHERE userId = :userId 
        AND type = 'KEYSTROKE'
        AND timestamp >= :startTime
    """)
    suspend fun getAverageInterKeyDelay(
        userId: String,
        startTime: Long
    ): Long?
    
    // Scroll behavior analysis
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SCROLL'
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getScrollData(
        userId: String,
        startTime: Long
    ): Flow<List<TouchDataEntity>>
    
    @Query("""
        SELECT AVG(velocity) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SCROLL'
        AND timestamp >= :startTime
    """)
    suspend fun getAverageScrollVelocity(
        userId: String,
        startTime: Long
    ): Float?
    
    // Swipe gesture analysis
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SWIPE'
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getSwipeData(
        userId: String,
        startTime: Long
    ): Flow<List<TouchDataEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SWIPE'
        AND additionalData LIKE '%direction=UP%'
        AND timestamp >= :startTime
    """)
    suspend fun getUpSwipeCount(
        userId: String,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SWIPE'
        AND additionalData LIKE '%direction=DOWN%'
        AND timestamp >= :startTime
    """)
    suspend fun getDownSwipeCount(
        userId: String,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SWIPE'
        AND additionalData LIKE '%direction=LEFT%'
        AND timestamp >= :startTime
    """)
    suspend fun getLeftSwipeCount(
        userId: String,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND type = 'SWIPE'
        AND additionalData LIKE '%direction=RIGHT%'
        AND timestamp >= :startTime
    """)
    suspend fun getRightSwipeCount(
        userId: String,
        startTime: Long
    ): Int
    
    // Sensor data analysis
    
    @Query("""
        SELECT * FROM touch_data 
        WHERE userId = :userId 
        AND type IN ('ACCELEROMETER', 'GYROSCOPE', 'MAGNETOMETER')
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getSensorData(
        userId: String,
        startTime: Long
    ): Flow<List<TouchDataEntity>>
    
    // Spatial analysis
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND x >= :minX AND x <= :maxX 
        AND y >= :minY AND y <= :maxY
        AND timestamp >= :startTime
    """)
    suspend fun getTouchCountInRegion(
        userId: String,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT AVG(x) as avgX, AVG(y) as avgY, 
               COUNT(*) as touchCount
        FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
    """)
    suspend fun getTouchSpatialStats(
        userId: String,
        startTime: Long
    ): TouchSpatialStats?
    
    // Time-based analysis
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
        GROUP BY CAST(timestamp / (24 * 60 * 60 * 1000) AS INTEGER)
        ORDER BY CAST(timestamp / (24 * 60 * 60 * 1000) AS INTEGER)
    """)
    fun getDailyTouchCounts(
        userId: String,
        startTime: Long
    ): Flow<List<Int>>
    
    @Query("""
        SELECT COUNT(*) FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
        GROUP BY CAST(timestamp / (60 * 60 * 1000) AS INTEGER)
        ORDER BY CAST(timestamp / (60 * 60 * 1000) AS INTEGER)
    """)
    fun getHourlyTouchCounts(
        userId: String,
        startTime: Long
    ): Flow<List<Int>>
    
    // Session analysis
    
    @Query("""
        SELECT COUNT(DISTINCT sessionId) FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
    """)
    suspend fun getSessionCount(
        userId: String,
        startTime: Long
    ): Int
    
    @Query("""
        SELECT sessionId, COUNT(*) as touchCount, 
               MIN(timestamp) as startTime, 
               MAX(timestamp) as endTime
        FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
        GROUP BY sessionId
        ORDER BY startTime DESC
    """)
    fun getSessionStats(
        userId: String,
        startTime: Long
    ): Flow<List<SessionStats>>
    
    // Behavioral pattern queries for ML models
    
    @Query("""
        SELECT x, y, pressure, velocity, timestamp, type
        FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
        ORDER BY timestamp ASC
    """)
    fun getBehavioralFeatures(
        userId: String,
        startTime: Long
    ): Flow<List<BehavioralFeatures>>
    
    @Query("""
        SELECT 
            AVG(pressure) as avgPressure,
            AVG(velocity) as avgVelocity,
            COUNT(*) as totalTouches,
            COUNT(DISTINCT type) as uniqueTypes
        FROM touch_data 
        WHERE userId = :userId 
        AND timestamp >= :startTime
    """)
    suspend fun getBehavioralProfile(
        userId: String,
        startTime: Long
    ): BehavioralProfile?
    
    // Cleanup operations
    
    @Query("DELETE FROM touch_data WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTouchData(cutoffTime: Long)
    
    @Query("DELETE FROM touch_data WHERE userId = :userId")
    suspend fun deleteAllTouchDataForUser(userId: String)
    
    @Query("DELETE FROM touch_data WHERE sessionId = :sessionId")
    suspend fun deleteTouchDataForSession(sessionId: String)
    
    // Statistics and metrics
    
    @Query("SELECT COUNT(*) FROM touch_data")
    suspend fun getTotalTouchDataCount(): Int
    
    @Query("SELECT COUNT(*) FROM touch_data WHERE userId = :userId")
    suspend fun getUserTouchDataCount(userId: String): Int
    
    @Query("SELECT COUNT(DISTINCT userId) FROM touch_data")
    suspend fun getUniqueUserCount(): Int
    
    @Query("SELECT COUNT(DISTINCT deviceUUID) FROM touch_data")
    suspend fun getUniqueDeviceCount(): Int
    
    @Query("SELECT COUNT(DISTINCT sessionId) FROM touch_data")
    suspend fun getTotalSessionCount(): Int
}

// Data classes for query results

data class TouchSpatialStats(
    val avgX: Float,
    val avgY: Float,
    val touchCount: Int
)

data class SessionStats(
    val sessionId: String,
    val touchCount: Int,
    val startTime: Long,
    val endTime: Long
)

data class BehavioralFeatures(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val velocity: Float,
    val timestamp: Long,
    val type: String
)

data class BehavioralProfile(
    val avgPressure: Float,
    val avgVelocity: Float,
    val totalTouches: Int,
    val uniqueTypes: Int
)

