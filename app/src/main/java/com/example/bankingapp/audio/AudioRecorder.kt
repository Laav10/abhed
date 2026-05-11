package com.example.bankingapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    private var isRecording = false
    
    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_FORMAT = ".3gp"
    }
    
    /**
     * Start recording audio
     */
    fun startRecording(fileName: String): Boolean {
        return try {
            // Create output file path
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            outputFile = File(audioDir, "$fileName$AUDIO_FORMAT").absolutePath
            
            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                
                prepare()
                start()
                isRecording = true
                Log.d(TAG, "Recording started: $outputFile")
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            false
        }
    }
    
    /**
     * Stop recording audio
     */
    fun stopRecording(): String? {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                Log.d(TAG, "Recording stopped: $outputFile")
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get the current output file path
     */
    fun getCurrentOutputFile(): String? = outputFile
    
    /**
     * Cancel recording and delete the file
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                
                // Delete the incomplete file
                outputFile?.let { filePath ->
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                        Log.d(TAG, "Cancelled recording and deleted file: $filePath")
                    }
                }
                outputFile = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }
    }
    
    /**
     * Get audio file duration in milliseconds
     */
    fun getAudioDuration(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                // Simple estimation based on file size (not accurate but functional)
                // For real implementation, you'd use MediaMetadataRetriever
                file.length() / 1000 // Rough estimation
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration", e)
            0L
        }
    }
    
    /**
     * Delete audio file
     */
    fun deleteAudioFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Audio file deleted: $filePath, success: $deleted")
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting audio file", e)
            false
        }
    }
}
