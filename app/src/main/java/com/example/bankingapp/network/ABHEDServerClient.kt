package com.example.bankingapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * ABHED Server Client for touch-based behavioral authentication
 * 
 * This client handles communication with the ABHED Flask server for:
 * - User authentication and session management
 * - Touch stroke data submission
 * - Confidence score retrieval
 */
class ABHEDServerClient(
    private val baseUrl: String = "http://192.168.27.81:3343", // Current ip for server
    private val timeoutSeconds: Long = 30
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()
    
    private var authToken: String? = null
    private var currentUserId: String? = null
    
    companion object {
        private const val TAG = "ABHEDServerClient"
    }
    
    /**
     * Login user and create session
     * @param userId User identifier
     * @param debitCard Optional debit card number
     * @return LoginResult with token and status
     */
    suspend fun loginUser(userId: String, debitCard: String? = null): LoginResult = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("user_id", userId)
                if (debitCard != null) {
                    put("debit_card", debitCard)
                }
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/userlogin")
                .post(requestBody.toRequestBody(mediaType))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
                val token = (jsonResponse["token"] as? JsonPrimitive)?.content
                val status = (jsonResponse["status"] as? JsonPrimitive)?.content
                
                if (token != null && status == "ok") {
                    authToken = token
                    currentUserId = userId
                    Log.d(TAG, "Login successful for user: $userId")
                    LoginResult.Success(token)
                } else {
                    Log.e(TAG, "Login failed: Invalid response format")
                    LoginResult.Error("Invalid response format")
                }
            } else {
                Log.e(TAG, "Login failed: ${response.code} - $responseBody")
                LoginResult.Error("Login failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            LoginResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Submit touch stroke data to server (legacy method)
     * @param strokeData List of touch points in server format
     * @param dpiX Screen DPI X
     * @param dpiY Screen DPI Y
     * @param phoneOrientation Device orientation
     * @param phoneId Device identifier
     * @return StrokeResult with status and inter-stroke time
     */
    suspend fun submitStroke(
        strokeData: List<TouchPoint>,
        dpiX: Int = 400,
        dpiY: Int = 400,
        phoneOrientation: Int = 0,
        phoneId: Int = 1
    ): StrokeResult = withContext(Dispatchers.IO) {
        try {
            if (authToken == null) {
                return@withContext StrokeResult.Error("Not authenticated")
            }
            
            val requestBody = buildJsonObject {
                putJsonArray("stroke") {
                    strokeData.forEach { point ->
                        add(buildJsonObject {
                            put("time_ms", point.timeMs)
                            put("action", point.action)
                            put("x", point.x)
                            put("y", point.y)
                            put("pressure", point.pressure)
                            put("area", point.area)
                            put("finger_orientation", point.fingerOrientation)
                        })
                    }
                }
                put("dpi_x", dpiX)
                put("dpi_y", dpiY)
                put("phone_orientation", phoneOrientation)
                put("phone_id", phoneId)
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/raw_stroke")
                .post(requestBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
                val status = (jsonResponse["status"] as? JsonPrimitive)?.content
                val stored = (jsonResponse["stored"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
                val interStrokeTime = (jsonResponse["inter_stroke_time"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                
                if (status == "ok" && stored == true) {
                    Log.d(TAG, "Stroke submitted successfully")
                    StrokeResult.Success(interStrokeTime)
                } else {
                    Log.e(TAG, "Stroke submission failed: $responseBody")
                    StrokeResult.Error("Submission failed: $status")
                }
            } else {
                Log.e(TAG, "Stroke submission failed: ${response.code} - $responseBody")
                StrokeResult.Error("Submission failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stroke submission error", e)
            StrokeResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Submit raw stroke data to server (new method for StrokeData)
     * @param strokeData StrokeData object with stroke points and device info
     * @return StrokeResult with status and inter-stroke time
     */
    suspend fun submitRawStroke(strokeData: StrokeData): StrokeResult = withContext(Dispatchers.IO) {
        try {
            if (authToken == null) {
                return@withContext StrokeResult.Error("Not authenticated")
            }
            
            val requestBody = buildJsonObject {
                putJsonArray("stroke") {
                    strokeData.stroke.forEach { point ->
                        add(buildJsonObject {
                            put("time_ms", point.time_ms)
                            put("action", point.action)
                            put("x", point.x)
                            put("y", point.y)
                            put("pressure", point.pressure)
                            put("area", point.area)
                            put("finger_orientation", point.finger_orientation)
                        })
                    }
                }
                put("dpi_x", strokeData.dpi_x)
                put("dpi_y", strokeData.dpi_y)
                put("phone_orientation", strokeData.phone_orientation)
                put("phone_id", strokeData.phone_id)
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/raw_stroke")
                .post(requestBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
                val status = (jsonResponse["status"] as? JsonPrimitive)?.content
                val stored = (jsonResponse["stored"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
                val interStrokeTime = (jsonResponse["inter_stroke_time"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                
                if (status == "ok" && stored == true) {
                    Log.d(TAG, "Stroke submitted successfully")
                    StrokeResult.Success(interStrokeTime)
                } else {
                    Log.e(TAG, "Stroke submission failed: $responseBody")
                    StrokeResult.Error("Submission failed: $status")
                }
            } else {
                Log.e(TAG, "Stroke submission failed: ${response.code} - $responseBody")
                StrokeResult.Error("Submission failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stroke submission error", e)
            StrokeResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Get authentication confidence score
     * @param nRecent Number of recent strokes to consider (default: 10)
     * @return ConfidenceResult with score and status
     */
    suspend fun getConfidence(nRecent: Int = 10): ConfidenceResult = withContext(Dispatchers.IO) {
        try {
            if (authToken == null) {
                return@withContext ConfidenceResult.Error("Not authenticated")
            }
            
            val request = Request.Builder()
                .url("$baseUrl/confidence?n_recent=$nRecent")
                .get()
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
                val status = (jsonResponse["status"] as? JsonPrimitive)?.content
                
                when (status) {
                    "ok" -> {
                        val confidence = (jsonResponse["confidence"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                        val rawScore = (jsonResponse["raw_score"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                        
                        if (confidence != null) {
                            Log.d(TAG, "Confidence score: $confidence")
                            ConfidenceResult.Success(confidence, rawScore)
                        } else {
                            Log.e(TAG, "Invalid confidence response: $responseBody")
                            ConfidenceResult.Error("Invalid response format")
                        }
                    }
                    "collecting_data" -> {
                        val have = (jsonResponse["have"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        val need = (jsonResponse["need"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 100
                        Log.d(TAG, "Collecting data: $have/$need strokes")
                        ConfidenceResult.CollectingData(have, need)
                    }
                    else -> {
                        Log.e(TAG, "Confidence check failed: $responseBody")
                        ConfidenceResult.Error("Confidence check failed: $status")
                    }
                }
            } else {
                Log.e(TAG, "Confidence check failed: ${response.code} - $responseBody")
                ConfidenceResult.Error("Confidence check failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Confidence check error", e)
            ConfidenceResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Logout user and end session
     * @return LogoutResult with status
     */
    suspend fun logoutUser(): LogoutResult = withContext(Dispatchers.IO) {
        try {
            if (authToken == null) {
                return@withContext LogoutResult.Success
            }
            
            val request = Request.Builder()
                .url("$baseUrl/userlogout")
                .post("{}".toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
                val status = (jsonResponse["status"] as? JsonPrimitive)?.content
                
                if (status == "ok") {
                    Log.d(TAG, "Logout successful")
                    authToken = null
                    currentUserId = null
                    LogoutResult.Success
                } else {
                    Log.e(TAG, "Logout failed: $responseBody")
                    LogoutResult.Error("Logout failed: $status")
                }
            } else {
                Log.e(TAG, "Logout failed: ${response.code} - $responseBody")
                LogoutResult.Error("Logout failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            LogoutResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Check if user is currently authenticated
     */
    fun isAuthenticated(): Boolean = authToken != null
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = currentUserId
}

/**
 * Touch point data structure matching server format
 */
@Serializable
data class TouchPoint(
    val timeMs: Long,
    val action: Int, // 0=ACTION_DOWN, 1=ACTION_UP, 2=ACTION_MOVE
    val x: Float,
    val y: Float,
    val pressure: Float,
    val area: Float,
    val fingerOrientation: Float
)

/**
 * Login result sealed class
 */
sealed class LoginResult {
    data class Success(val token: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * Stroke submission result sealed class
 */
sealed class StrokeResult {
    data class Success(val interStrokeTime: Double?) : StrokeResult()
    data class Error(val message: String) : StrokeResult()
}

/**
 * Confidence result sealed class
 */
sealed class ConfidenceResult {
    data class Success(val confidence: Double, val rawScore: Double?) : ConfidenceResult()
    data class CollectingData(val have: Int, val need: Int) : ConfidenceResult()
    data class Error(val message: String) : ConfidenceResult()
}

/**
 * Logout result sealed class
 */
sealed class LogoutResult {
    object Success : LogoutResult()
    data class Error(val message: String) : LogoutResult()
}
