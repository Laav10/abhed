package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.ui.theme.ABHEDColors
import com.example.bankingapp.network.TouchDataManager
import com.example.bankingapp.network.AuthenticationStatus
import com.example.bankingapp.ui.components.ConfidenceScoreDisplay
import kotlinx.coroutines.delay

// Main integrated onboarding flow that combines gesture training and banking simulation
@Composable
fun IntegratedOnboardingFlow(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit = {},
    userId: String = "demo_user",
    context: android.content.Context
) {
    var currentPhase by remember { mutableStateOf(IntegratedOnboardingPhase.WELCOME) }
    var totalTouchDataCollected by remember { mutableStateOf(0) }
    var showCompletion by remember { mutableStateOf(false) }
    
    // Initialize TouchDataManager
    val touchDataManager = remember { TouchDataManager(context) }
    
    // Observe TouchDataManager state
    val isAuthenticated by touchDataManager.isAuthenticated.collectAsState()
    val confidenceScore by touchDataManager.confidenceScore.collectAsState()
    val isCollectingData by touchDataManager.isCollectingData.collectAsState()
    val dataCollectionProgress by touchDataManager.dataCollectionProgress.collectAsState()
    val lastError by touchDataManager.lastError.collectAsState()
    
    // Initialize the touch data manager
    LaunchedEffect(Unit) {
        touchDataManager.initialize(userId)
    }
    
    // Track progress across all phases
    val phaseProgress = when (currentPhase) {
        IntegratedOnboardingPhase.WELCOME -> 0.0f
        IntegratedOnboardingPhase.GESTURE_TRAINING -> 0.2f
        IntegratedOnboardingPhase.BANKING_SIMULATION -> 0.6f
        IntegratedOnboardingPhase.COMPLETION -> 1.0f
    }
    
    LaunchedEffect(currentPhase) {
        if (currentPhase == IntegratedOnboardingPhase.COMPLETION) {
            delay(3000)
            showCompletion = true
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
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        // Global progress header
        GlobalProgressHeader(
            currentPhase = currentPhase,
            progress = phaseProgress,
            totalTouchData = totalTouchDataCollected
        )
        
        // Phase content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (currentPhase) {
                IntegratedOnboardingPhase.WELCOME -> WelcomePhase(
                    onNext = { currentPhase = IntegratedOnboardingPhase.GESTURE_TRAINING }
                )
                
                IntegratedOnboardingPhase.GESTURE_TRAINING -> GameLikeOnboardingScreen(
                    onComplete = { currentPhase = IntegratedOnboardingPhase.BANKING_SIMULATION },
                    onTouchDataCollected = { touchData ->
                        onTouchDataCollected(touchData)
                        totalTouchDataCollected++
                        // Submit to ABHED server
                        touchDataManager.submitTouchPoint(touchData)
                    }
                )
                
                IntegratedOnboardingPhase.BANKING_SIMULATION -> BankingSimulationOnboarding(
                    onComplete = { currentPhase = IntegratedOnboardingPhase.COMPLETION },
                    onTouchDataCollected = { touchData ->
                        onTouchDataCollected(touchData)
                        totalTouchDataCollected++
                        // Submit to ABHED server
                        touchDataManager.submitTouchPoint(touchData)
                    }
                )
                
                IntegratedOnboardingPhase.COMPLETION -> CompletionPhase(
                    totalTouchData = totalTouchDataCollected,
                    onComplete = onComplete,
                    showCompletion = showCompletion,
                    confidenceScore = confidenceScore,
                    authenticationStatus = touchDataManager.getAuthenticationStatus(),
                    dataCollectionProgress = dataCollectionProgress
                )
            }
        }
    }
}

enum class IntegratedOnboardingPhase {
    WELCOME,
    GESTURE_TRAINING,
    BANKING_SIMULATION,
    COMPLETION
}

@Composable
private fun GlobalProgressHeader(
    currentPhase: IntegratedOnboardingPhase,
    progress: Float,
    totalTouchData: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (currentPhase) {
                            IntegratedOnboardingPhase.WELCOME -> "Welcome to ABHED"
                            IntegratedOnboardingPhase.GESTURE_TRAINING -> "Learning Your Patterns"
                            IntegratedOnboardingPhase.BANKING_SIMULATION -> "Banking Simulation"
                            IntegratedOnboardingPhase.COMPLETION -> "Setup Complete!"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ABHEDColors.Charcoal
                    )
                    
                    Text(
                        text = when (currentPhase) {
                            IntegratedOnboardingPhase.WELCOME -> "Let's get started with your secure banking setup"
                            IntegratedOnboardingPhase.GESTURE_TRAINING -> "Capturing your unique touch patterns"
                            IntegratedOnboardingPhase.BANKING_SIMULATION -> "Practice real banking scenarios"
                            IntegratedOnboardingPhase.COMPLETION -> "Your behavioral profile is ready"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                }
                
                // Touch data counter
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalTouchData.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ABHEDColors.LightSeaGreen
                        )
                        Text(
                            text = "Touch Points",
                            style = MaterialTheme.typography.bodySmall,
                            color = ABHEDColors.Charcoal
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Overall progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Overall Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.Charcoal
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Phase indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PhaseIndicator(
                    phase = IntegratedOnboardingPhase.WELCOME,
                    isActive = currentPhase == IntegratedOnboardingPhase.WELCOME,
                    isCompleted = currentPhase.ordinal > IntegratedOnboardingPhase.WELCOME.ordinal
                )
                PhaseIndicator(
                    phase = IntegratedOnboardingPhase.GESTURE_TRAINING,
                    isActive = currentPhase == IntegratedOnboardingPhase.GESTURE_TRAINING,
                    isCompleted = currentPhase.ordinal > IntegratedOnboardingPhase.GESTURE_TRAINING.ordinal
                )
                PhaseIndicator(
                    phase = IntegratedOnboardingPhase.BANKING_SIMULATION,
                    isActive = currentPhase == IntegratedOnboardingPhase.BANKING_SIMULATION,
                    isCompleted = currentPhase.ordinal > IntegratedOnboardingPhase.BANKING_SIMULATION.ordinal
                )
                PhaseIndicator(
                    phase = IntegratedOnboardingPhase.COMPLETION,
                    isActive = currentPhase == IntegratedOnboardingPhase.COMPLETION,
                    isCompleted = false
                )
            }
        }
    }
}

@Composable
private fun PhaseIndicator(
    phase: IntegratedOnboardingPhase,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val (icon, color) = when {
        isCompleted -> Pair(
            Icons.Filled.CheckCircle,
            ABHEDColors.LightSeaGreen
        )
        isActive -> {
            val icon = when (phase) {
                IntegratedOnboardingPhase.WELCOME -> Icons.Filled.Home
                IntegratedOnboardingPhase.GESTURE_TRAINING -> Icons.Filled.TouchApp
                IntegratedOnboardingPhase.BANKING_SIMULATION -> Icons.Filled.AccountBalance
                IntegratedOnboardingPhase.COMPLETION -> Icons.Filled.CheckCircle
            }
            Pair(icon, ABHEDColors.LightSeaGreen)
        }
        else -> {
            val icon = when (phase) {
                IntegratedOnboardingPhase.WELCOME -> Icons.Outlined.Home
                IntegratedOnboardingPhase.GESTURE_TRAINING -> Icons.Outlined.TouchApp
                IntegratedOnboardingPhase.BANKING_SIMULATION -> Icons.Outlined.AccountBalance
                IntegratedOnboardingPhase.COMPLETION -> Icons.Outlined.CheckCircle
            }
            Pair(icon, ABHEDColors.GlaucousMoonstone)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isActive || isCompleted) 
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                    else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = phase.name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = when (phase) {
                IntegratedOnboardingPhase.WELCOME -> "Welcome"
                IntegratedOnboardingPhase.GESTURE_TRAINING -> "Training"
                IntegratedOnboardingPhase.BANKING_SIMULATION -> "Simulation"
                IntegratedOnboardingPhase.COMPLETION -> "Complete"
            },
            style = MaterialTheme.typography.bodySmall,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WelcomePhase(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            ),
            border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated logo
                val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ABHEDColors.LightSeaGreen,
                                    ABHEDColors.LightSeaGreen.copy(alpha = 0.7f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = "ABHED Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Welcome to ABHED Banking",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Adaptive Behavioral Heuristics for Enhanced Defense",
                    style = MaterialTheme.typography.titleSmall,
                    color = ABHEDColors.GlaucousMoonstone,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Your device will learn your unique behavioral patterns to provide continuous, passwordless security. This process takes just a few minutes and ensures your banking experience is both secure and seamless.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Key benefits
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "What you'll learn:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                BenefitItem(
                    icon = Icons.Filled.TouchApp,
                    title = "Touch Patterns",
                    description = "How you tap, swipe, and interact with your device"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                BenefitItem(
                    icon = Icons.Filled.AccountBalance,
                    title = "Banking Behaviors",
                    description = "Your natural navigation patterns in banking apps"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                BenefitItem(
                    icon = Icons.Filled.Security,
                    title = "Security Profile",
                    description = "Continuous authentication based on your behavior"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Start button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Start",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Learning Process",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ABHEDColors.LightSeaGreen,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ABHEDColors.Charcoal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.GlaucousMoonstone
            )
        }
    }
}

@Composable
private fun CompletionPhase(
    totalTouchData: Int,
    onComplete: () -> Unit,
    showCompletion: Boolean,
    confidenceScore: Double = 0.0,
    authenticationStatus: AuthenticationStatus = AuthenticationStatus.NotAuthenticated,
    dataCollectionProgress: Pair<Int, Int> = 0 to 100
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        val infiniteTransition = rememberInfiniteTransition(label = "success_animation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ABHEDColors.LightSeaGreen,
                            ABHEDColors.LightSeaGreen.copy(alpha = 0.7f)
                        )
                    ),
                    shape = CircleShape
                )
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your behavioral profile has been successfully created with $totalTouchData touch data points. You're now ready to enjoy secure, passwordless banking!",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.GlaucousMoonstone,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Confidence Score Display
        ConfidenceScoreDisplay(
            confidenceScore = confidenceScore,
            authenticationStatus = authenticationStatus,
            dataCollectionProgress = dataCollectionProgress,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            ),
            border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Security Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Touch Points", totalTouchData.toString())
                    StatCard("Gestures", "6")
                    StatCard("Scenarios", "3")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Complete button
        AnimatedVisibility(
            visible = showCompletion,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(1000)
            )
        ) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ABHEDColors.LightSeaGreen
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Done,
                    contentDescription = "Complete",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enter ABHED Banking",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ABHEDColors.LightSeaGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ABHEDColors.Charcoal
        )
    }
}
