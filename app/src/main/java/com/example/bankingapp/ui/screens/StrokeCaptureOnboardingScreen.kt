package com.example.bankingapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.network.ABHEDServerClient
import com.example.bankingapp.network.StrokeCaptureManager
import com.example.bankingapp.network.LoginResult
import com.example.bankingapp.network.ConfidenceResult
import com.example.bankingapp.ui.components.ConfidenceScoreDisplay
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun StrokeCaptureOnboardingScreen(
    userId: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize server client and stroke capture manager
    val serverClient = remember { ABHEDServerClient() }
    val strokeManager = remember { StrokeCaptureManager(context, serverClient) }
    
    // State variables
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf(StrokeCapturePhase.WELCOME) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var strokePaths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var instructionText by remember { mutableStateOf("Welcome! Let's train your touch behavior.") }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Collect state flows
    val strokeCount by strokeManager.strokeCount.collectAsState()
    val isCapturing by strokeManager.isCapturingState.collectAsState()
    val lastStatus by strokeManager.lastSubmissionStatus.collectAsState()
    val totalPoints by strokeManager.totalPointsCaptured.collectAsState()
    
    // Animation values
    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Login to server when component starts
    LaunchedEffect(Unit) {
        try {
            val result = serverClient.loginUser(userId)
            when (result) {
                is LoginResult.Success -> {
                    isLoggedIn = true
                    instructionText = "Great! Now let's capture some touch strokes."
                    currentPhase = StrokeCapturePhase.CAPTURE
                }
                is LoginResult.Error -> {
                    instructionText = "Failed to connect to server: ${result.message}"
                    currentPhase = StrokeCapturePhase.ERROR
                }
            }
        } catch (e: Exception) {
            instructionText = "Network error: ${e.message}"
            currentPhase = StrokeCapturePhase.ERROR
        }
    }
    
    // Check confidence score periodically
    LaunchedEffect(strokeCount) {
        if (strokeCount > 0 && strokeCount % 10 == 0) {
            try {
                val confidence = serverClient.getConfidence()
                            when (confidence) {
                is ConfidenceResult.Success -> {
                    instructionText = "Confidence: ${(confidence.confidence * 100).toInt()}% - Keep going!"
                }
                is ConfidenceResult.CollectingData -> {
                    instructionText = "Collecting data: ${confidence.have}/${confidence.need} strokes"
                }
                is ConfidenceResult.Error -> {
                    instructionText = "Error checking confidence: ${confidence.message}"
                }
            }
            } catch (e: Exception) {
                // Ignore confidence check errors during training
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Touch Behavior Training",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.DeftBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Status display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = ABHEDColors.Lavender.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusItem("Strokes", strokeCount.toString(), ABHEDColors.DeftBlue)
                    StatusItem("Points", totalPoints.toString(), ABHEDColors.Success)
                    StatusItem("Status", if (isCapturing) "Capturing" else "Ready", 
                             if (isCapturing) ABHEDColors.Warning else ABHEDColors.Success)
                }
                
                lastStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.startsWith("Error")) Color.Red else ABHEDColors.Success,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Canvas for stroke capture
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(bottom = 16.dp)
                .border(
                    width = if (isCapturing) 3.dp else 1.dp,
                    color = if (isCapturing) ABHEDColors.Warning else ABHEDColors.DeftBlue,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Canvas for drawing strokes
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (currentPhase == StrokeCapturePhase.CAPTURE && !isProcessing) {
                                        currentStroke = listOf(offset)
                                        strokeManager.startStroke(
                                            x = offset.x,
                                            y = offset.y,
                                            pressure = 0.5f,
                                            area = 100f
                                        )
                                    }
                                },
                                onDrag = { _, dragAmount ->
                                    if (currentPhase == StrokeCapturePhase.CAPTURE && !isProcessing) {
                                        val newPoint = currentStroke.last() + dragAmount
                                        currentStroke = currentStroke + newPoint
                                        strokeManager.addStrokePoint(
                                            x = newPoint.x,
                                            y = newPoint.y,
                                            pressure = 0.5f,
                                            area = 100f
                                        )
                                    }
                                },
                                onDragEnd = {
                                    if (currentPhase == StrokeCapturePhase.CAPTURE && !isProcessing) {
                                        if (currentStroke.isNotEmpty()) {
                                            val lastPoint = currentStroke.last()
                                            strokeManager.endStroke(
                                                x = lastPoint.x,
                                                y = lastPoint.y,
                                                pressure = 0.5f,
                                                area = 100f
                                            )
                                            strokePaths = strokePaths + listOf(currentStroke)
                                            currentStroke = emptyList()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    // Draw completed strokes
                    strokePaths.forEach { stroke ->
                        if (stroke.size > 1) {
                            val path = Path()
                            path.moveTo(stroke.first().x, stroke.first().y)
                            stroke.drop(1).forEach { point ->
                                path.lineTo(point.x, point.y)
                            }
                            drawPath(
                                path = path,
                                color = ABHEDColors.DeftBlue,
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                    
                    // Draw current stroke
                    if (currentStroke.size > 1) {
                        val path = Path()
                        path.moveTo(currentStroke.first().x, currentStroke.first().y)
                        currentStroke.drop(1).forEach { point ->
                            path.lineTo(point.x, point.y)
                        }
                        drawPath(
                            path = path,
                            color = ABHEDColors.Warning,
                            style = Stroke(width = 6f)
                        )
                    }
                }
                
                // Instructions overlay
                if (currentPhase == StrokeCapturePhase.CAPTURE && strokeCount < 5) {
                    Text(
                        text = "Draw strokes here!\nStart from top-left, drag to bottom-right",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.DeftBlue,
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    )
                }
                
                // Capturing indicator
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = ABHEDColors.Warning,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    strokeManager.clearStats()
                    strokePaths = emptyList()
                    currentStroke = emptyList()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ABHEDColors.Lavender)
            ) {
                Text("Clear")
            }
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isProcessing = true
                        try {
                            val confidence = serverClient.getConfidence()
                            when (confidence) {
                                is ConfidenceResult.Success -> {
                                    instructionText = "Final confidence: ${(confidence.confidence * 100).toInt()}%"
                                }
                                is ConfidenceResult.CollectingData -> {
                                    instructionText = "Need more data: ${confidence.have}/${confidence.need} strokes"
                                }
                                is ConfidenceResult.Error -> {
                                    instructionText = "Error: ${confidence.message}"
                                }
                            }
                            // Regardless of server status, proceed to dashboard after a short delay
                            delay(800)
                            onComplete()
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                enabled = strokeCount >= 1 && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = ABHEDColors.LightSeaGreen)
            ) {
                Text(if (isProcessing) "Processing..." else "Complete Training")
            }
        }
        
        // Progress indicator
        if (strokeCount > 0) {
            LinearProgressIndicator(
                progress = (strokeCount.toFloat() / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = ABHEDColors.Success,
                trackColor = ABHEDColors.Lavender
            )
            Text(
                text = "$strokeCount / 100 strokes collected",
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.DeftBlue,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

enum class StrokeCapturePhase {
    WELCOME,
    CAPTURE,
    ERROR,
    COMPLETE
}
