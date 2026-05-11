package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

// Game-like tutorial overlay with cloud speech bubble
@Composable
fun TutorialOverlay(
    text: String,
    targetPosition: Offset,
    arrowDirection: ArrowDirection = ArrowDirection.DOWN,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f),
        modifier = modifier.zIndex(10f)
    ) {
        Card(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Character avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ABHEDColors.LightSeaGreen,
                                    ABHEDColors.LightSeaGreen.copy(alpha = 0.7f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Speech bubble text
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Arrow pointing to target
                when (arrowDirection) {
                    ArrowDirection.UP -> Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Point up",
                        tint = ABHEDColors.LightSeaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    ArrowDirection.DOWN -> Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Point down",
                        tint = ABHEDColors.LightSeaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    ArrowDirection.LEFT -> Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Point left",
                        tint = ABHEDColors.LightSeaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    ArrowDirection.RIGHT -> Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Point right",
                        tint = ABHEDColors.LightSeaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}



// Enhanced gesture trainer with visual feedback
@Composable
fun EnhancedGestureTrainer(
    expectedGesture: String,
    onGestureDetected: (TouchDataPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPath by remember { mutableStateOf(listOf<Offset>()) }
    var longPressTriggered by remember { mutableStateOf(false) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var gestureCompleted by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Success animation
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            showSuccess = false
            gestureCompleted = true
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gesture instruction with visual cue
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (expectedGesture) {
                    "Tap" -> ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                    "LongPress" -> ABHEDColors.Periwinkle.copy(alpha = 0.1f)
                    "SwipeLeft", "SwipeRight", "SwipeUp", "SwipeDown" -> ABHEDColors.Lavender.copy(alpha = 0.1f)
                    else -> ABHEDColors.Lavender.copy(alpha = 0.1f)
                }
            ),
            border = BorderStroke(
                2.dp, 
                when (expectedGesture) {
                    "Tap" -> ABHEDColors.LightSeaGreen
                    "LongPress" -> ABHEDColors.Periwinkle
                    "SwipeLeft", "SwipeRight", "SwipeUp", "SwipeDown" -> ABHEDColors.Lavender
                    else -> ABHEDColors.Lavender
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated gesture icon
                val infiniteTransition = rememberInfiniteTransition(label = "gesture_icon")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Icon(
                    imageVector = when (expectedGesture) {
                        "Tap" -> Icons.Filled.TouchApp
                        "LongPress" -> Icons.Filled.Timer
                        "SwipeLeft" -> Icons.Filled.ArrowBack
                        "SwipeRight" -> Icons.Filled.ArrowForward
                        "SwipeUp" -> Icons.Filled.KeyboardArrowUp
                        "SwipeDown" -> Icons.Filled.KeyboardArrowDown
                        else -> Icons.Filled.Gesture
                    },
                    contentDescription = expectedGesture,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    tint = when (expectedGesture) {
                        "Tap" -> ABHEDColors.LightSeaGreen
                        "LongPress" -> ABHEDColors.Periwinkle
                        "SwipeLeft", "SwipeRight", "SwipeUp", "SwipeDown" -> ABHEDColors.Lavender
                        else -> ABHEDColors.Lavender
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = when (expectedGesture) {
                            "Tap" -> "Tap once inside the box"
                            "LongPress" -> "Press and hold for 2 seconds"
                            "SwipeLeft" -> "Swipe from right to left"
                            "SwipeRight" -> "Swipe from left to right"
                            "SwipeUp" -> "Swipe from bottom to top"
                            "SwipeDown" -> "Swipe from top to bottom"
                            else -> "Perform the gesture"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ABHEDColors.Charcoal
                    )
                    
                    Text(
                        text = "Follow the visual guide below",
                        style = MaterialTheme.typography.bodySmall,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Interactive training area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = ABHEDColors.Lavender.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    color = if (gestureCompleted) ABHEDColors.LightSeaGreen else ABHEDColors.GlaucousMoonstone,
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInput(expectedGesture) {
                    detectTapGestures(
                        onPress = { offset ->
                            startPoint = offset
                            startTime = System.currentTimeMillis()
                            tryAwaitRelease()
                        },
                        onTap = {
                            if (expectedGesture == "Tap" && !gestureCompleted) {
                                val endTime = System.currentTimeMillis()
                                val duration = (startTime?.let { endTime - it } ?: 0L)
                                onGestureDetected(
                                    TouchDataPoint(
                                        x = startPoint?.x ?: 0f,
                                        y = startPoint?.y ?: 0f,
                                        pressure = 0.5f,
                                        timestamp = System.currentTimeMillis(),
                                        type = TouchType.TAP
                                    )
                                )
                                showSuccess = true
                            }
                            startPoint = null
                            startTime = null
                        },
                        onLongPress = { offset ->
                            if (expectedGesture == "LongPress" && !gestureCompleted) {
                                longPressTriggered = true
                                val endTime = System.currentTimeMillis()
                                val duration = (startTime?.let { endTime - it } ?: 0L)
                                onGestureDetected(
                                    TouchDataPoint(
                                        x = startPoint?.x ?: offset.x,
                                        y = startPoint?.y ?: offset.y,
                                        pressure = 0.8f,
                                        timestamp = System.currentTimeMillis(),
                                        type = TouchType.LONG_PRESS
                                    )
                                )
                                showSuccess = true
                            }
                            startPoint = null
                            startTime = null
                        }
                    )
                }
                .pointerInput(expectedGesture) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (expectedGesture.startsWith("Swipe") && !gestureCompleted) {
                                dragPath = listOf(offset)
                                startPoint = offset
                                startTime = System.currentTimeMillis()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (expectedGesture.startsWith("Swipe")) {
                                dragPath = dragPath + change.position
                            }
                        },
                        onDragEnd = {
                            if (expectedGesture.startsWith("Swipe") && !gestureCompleted && dragPath.size >= 2 && startPoint != null && startTime != null) {
                                val dx = dragPath.last().x - startPoint!!.x
                                val dy = dragPath.last().y - startPoint!!.y
                                val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                
                                val detectedGesture = when {
                                    abs(dx) > abs(dy) -> if (dx < 0) "SwipeLeft" else "SwipeRight"
                                    else -> if (dy < 0) "SwipeUp" else "SwipeDown"
                                }
                                
                                if (detectedGesture == expectedGesture) {
                                    val endTime = System.currentTimeMillis()
                                    val duration = endTime - startTime!!
                                    onGestureDetected(
                                        TouchDataPoint(
                                            x = startPoint!!.x,
                                            y = startPoint!!.y,
                                            pressure = 0.6f,
                                            timestamp = System.currentTimeMillis(),
                                            type = when (expectedGesture) {
                                                "SwipeLeft" -> TouchType.SWIPE_LEFT
                                                "SwipeRight" -> TouchType.SWIPE_RIGHT
                                                "SwipeUp" -> TouchType.SWIPE_UP
                                                "SwipeDown" -> TouchType.SWIPE_DOWN
                                                else -> TouchType.SWIPE
                                            }
                                        )
                                    )
                                    showSuccess = true
                                }
                            }
                            dragPath = emptyList()
                            startPoint = null
                            startTime = null
                        }
                    )
                }
        ) {
            // Visual feedback canvas
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                // Draw gesture path
                if (dragPath.size >= 2) {
                    for (i in 1 until dragPath.size) {
                        drawLine(
                            color = ABHEDColors.LightSeaGreen,
                            start = dragPath[i - 1],
                            end = dragPath[i],
                            strokeWidth = 8.dp.toPx()
                        )
                    }
                }
                
                // Draw long press indicator
                if (longPressTriggered) {
                    drawCircle(
                        color = ABHEDColors.Periwinkle,
                        radius = size.minDimension / 4f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
                
                // Draw success indicator
                if (showSuccess) {
                    drawCircle(
                        color = ABHEDColors.LightSeaGreen,
                        radius = size.minDimension / 6f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            
            // Success overlay
            if (showSuccess) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Great! Gesture detected",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Main game-like onboarding screen
@Composable
fun GameLikeOnboardingScreen(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(0) }
    var showTutorial by remember { mutableStateOf(true) }
    var completedGestures by remember { mutableStateOf(setOf<String>()) }
    var totalTouchDataCollected by remember { mutableStateOf(0) }
    
    val onboardingSteps = remember {
        listOf(
            OnboardingStep(
                id = 0,
                title = "Welcome to ABHED Banking!",
                description = "Let's learn your unique touch patterns",
                targetElement = "Tap",
                arrowDirection = ArrowDirection.DOWN
            ),
            OnboardingStep(
                id = 1,
                title = "Long Press Practice",
                description = "Now let's learn your long press behavior",
                targetElement = "LongPress",
                arrowDirection = ArrowDirection.DOWN
            ),
            OnboardingStep(
                id = 2,
                title = "Swipe Gestures",
                description = "Learn your swipe patterns",
                targetElement = "SwipeLeft",
                arrowDirection = ArrowDirection.DOWN
            ),
            OnboardingStep(
                id = 3,
                title = "More Swipe Patterns",
                description = "Practice different swipe directions",
                targetElement = "SwipeRight",
                arrowDirection = ArrowDirection.DOWN
            ),
            OnboardingStep(
                id = 4,
                title = "Vertical Swipes",
                description = "Learn up and down swipes",
                targetElement = "SwipeUp",
                arrowDirection = ArrowDirection.DOWN
            ),
            OnboardingStep(
                id = 5,
                title = "Final Gesture",
                description = "Last swipe pattern",
                targetElement = "SwipeDown",
                arrowDirection = ArrowDirection.DOWN
            )
        )
    }
    
    val currentStepData = onboardingSteps[currentStep]
    val progress = (completedGestures.size.toFloat() / onboardingSteps.size.toFloat())
    
    // Auto-advance when gesture is completed
    LaunchedEffect(completedGestures.size) {
        if (completedGestures.contains(currentStepData.targetElement) && currentStep < onboardingSteps.size - 1) {
            delay(2000) // Show success for 2 seconds
            currentStep++
            showTutorial = true
        } else if (completedGestures.size == onboardingSteps.size) {
            delay(2000)
            onComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ABHEDColors.Lavender.copy(alpha = 0.1f),
                        Color.White,
                        ABHEDColors.Lavender.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Progress header
        OnboardingProgressHeader(
            progress = progress,
            currentStep = currentStep + 1,
            totalSteps = onboardingSteps.size,
            title = currentStepData.title,
            description = currentStepData.description
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Tutorial overlay
            if (showTutorial) {
                TutorialOverlay(
                    text = "Follow the instructions to complete: ${currentStepData.targetElement}",
                    targetPosition = Offset(200f, 300f),
                    arrowDirection = currentStepData.arrowDirection,
                    isVisible = showTutorial,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            
            // Gesture trainer
            EnhancedGestureTrainer(
                expectedGesture = currentStepData.targetElement,
                onGestureDetected = { touchData ->
                    onTouchDataCollected(touchData)
                    totalTouchDataCollected++
                    completedGestures = completedGestures + currentStepData.targetElement
                    showTutorial = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )
        }
        
        // Bottom action area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Touch Data Collected: $totalTouchDataCollected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Progress: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.LightSeaGreen
                )
                
                if (completedGestures.size == onboardingSteps.size) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.LightSeaGreen
                        )
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Complete",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Onboarding",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressHeader(
    progress: Float,
    currentStep: Int,
    totalSteps: Int,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.LightSeaGreen
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.LightSeaGreen
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ABHEDColors.LightSeaGreen,
                trackColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ABHEDColors.Charcoal,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = ABHEDColors.GlaucousMoonstone,
                textAlign = TextAlign.Center
            )
        }
    }
}


