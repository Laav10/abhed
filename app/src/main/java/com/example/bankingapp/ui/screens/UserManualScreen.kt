package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualScreen(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var touchDataPoints by remember { mutableStateOf(mutableListOf<TouchDataPoint>()) }
    
    val totalSteps = 6
    
    LaunchedEffect(Unit) {
        // Start collecting touch data immediately
        delay(1000)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with progress
        HeaderSection(currentStep, totalSteps)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main content area
        when (currentStep) {
            0 -> WelcomeStep(
                onNext = { currentStep++ },
                onTouchDataCollected = onTouchDataCollected
            )
            1 -> FeaturesOverviewStep(
                onNext = { currentStep++ },
                onTouchDataCollected = onTouchDataCollected
            )
            2 -> SecurityFeaturesStep(
                onNext = { currentStep++ },
                onTouchDataCollected = onTouchDataCollected
            )
            3 -> TouchDataCollectionStep(
                onNext = { currentStep++ },
                onTouchDataCollected = onTouchDataCollected
            )
            4 -> BehavioralSetupStep(
                onNext = { currentStep++ },
                onTouchDataCollected = onTouchDataCollected
            )
            5 -> FinalSetupStep(
                onComplete = onComplete,
                onTouchDataCollected = onTouchDataCollected
            )
        }
    }
}

@Composable
private fun HeaderSection(currentStep: Int, totalSteps: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome to ABHED Banking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / totalSteps,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to ABHED",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ABHED means 'Impenetrable' in Sanskrit",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your banking app is now protected by the most advanced behavioral authentication system. ABHED learns your unique touch patterns, navigation habits, and usage behavior to provide continuous, invisible security.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun FeaturesOverviewStep(
    onNext: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "App Features Overview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FeatureCard(
            icon = Icons.Default.AccountBalance,
            title = "Account Management",
            description = "View balances, transaction history, and account details",
            onTouchDataCollected = onTouchDataCollected
        )
        
        FeatureCard(
            icon = Icons.Default.Payment,
            title = "Money Transfer",
            description = "Send money to other accounts or UPI IDs",
            onTouchDataCollected = onTouchDataCollected
        )
        
        FeatureCard(
            icon = Icons.Default.CreditCard,
            title = "Card Management",
            description = "Manage your debit and credit cards",
            onTouchDataCollected = onTouchDataCollected
        )
        
        FeatureCard(
            icon = Icons.Default.Settings,
            title = "Security Settings",
            description = "Configure your security preferences",
            onTouchDataCollected = onTouchDataCollected
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Security Features")
        }
    }
}

@Composable
private fun SecurityFeaturesStep(
    onNext: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ABHED Security Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SecurityFeatureCard(
            icon = Icons.Default.TouchApp,
            title = "Touch & Swipe Analytics",
            description = "Analyzes your unique touch patterns, pressure, and gesture velocity",
            onTouchDataCollected = onTouchDataCollected
        )
        
        SecurityFeatureCard(
            icon = Icons.Default.Navigation,
            title = "Navigation Pattern Learning",
            description = "Learns your app usage flow and detects anomalies",
            onTouchDataCollected = onTouchDataCollected
        )
        
        SecurityFeatureCard(
            icon = Icons.Default.LocationOn,
            title = "Geolocation Monitoring",
            description = "Tracks location patterns and detects travel scenarios",
            onTouchDataCollected = onTouchDataCollected
        )
        
        SecurityFeatureCard(
            icon = Icons.Default.Psychology,
            title = "Behavioral Biometrics",
            description = "Continuous authentication using your unique behavior patterns",
            onTouchDataCollected = onTouchDataCollected
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Touch Data Collection")
        }
    }
}

@Composable
private fun TouchDataCollectionStep(
    onNext: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var touchCount by remember { mutableStateOf(0) }
    var isCollecting by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isCollecting = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Touch Data Collection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ABHED needs to learn your unique touch patterns. Please interact with the elements below naturally.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Interactive touch collection area
        TouchCollectionArea(
            onTouchDataCollected = { touchData ->
                onTouchDataCollected(touchData)
                touchCount++
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Touch interactions collected: $touchCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = touchCount >= 10
        ) {
            Text("Next: Behavioral Setup")
        }
        
        if (touchCount < 10) {
            Text(
                text = "Please collect at least 10 touch interactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BehavioralSetupStep(
    onNext: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Behavioral Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Let's set up your behavioral profile. This will help ABHED learn your patterns quickly.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Typing speed test
        TypingSpeedTest(onTouchDataCollected = onTouchDataCollected)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation pattern test
        NavigationPatternTest(onTouchDataCollected = onTouchDataCollected)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Final Setup")
        }
    }
}

@Composable
private fun FinalSetupStep(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ABHED is now learning your behavioral patterns. Your banking app is protected by the most advanced continuous authentication system.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "What happens next:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("• ABHED will continue learning your patterns")
                Text("• No more passwords needed for login")
                Text("• Continuous security monitoring")
                Text("• Automatic fraud detection")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Start Using ABHED Banking")
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTouchDataCollected(
                        TouchDataPoint(
                            x = offset.x,
                            y = offset.y,
                            pressure = 1.0f,
                            timestamp = System.currentTimeMillis(),
                            type = TouchType.TAP
                        )
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SecurityFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTouchDataCollected(
                        TouchDataPoint(
                            x = offset.x,
                            y = offset.y,
                            pressure = 1.0f,
                            timestamp = System.currentTimeMillis(),
                            type = TouchType.TAP
                        )
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TouchCollectionArea(
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTouchDataCollected(
                        TouchDataPoint(
                            x = offset.x,
                            y = offset.y,
                            pressure = 1.0f,
                            timestamp = System.currentTimeMillis(),
                            type = TouchType.TAP
                        )
                    )
                }
            },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tap anywhere in this area to collect touch data",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypingSpeedTest(
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "Type the following text:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "The quick brown fox jumps over the lazy dog",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                // Collect touch data for each keystroke
                onTouchDataCollected(
                    TouchDataPoint(
                        x = 0f,
                        y = 0f,
                        pressure = 1.0f,
                        timestamp = System.currentTimeMillis(),
                        type = TouchType.KEYSTROKE
                    )
                )
            },
            label = { Text("Type here") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NavigationPatternTest(
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Home", "Accounts", "Transfer", "Cards", "Settings")
    
    Column {
        Text(
            text = "Navigate through these sections:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            steps.forEachIndexed { index, step ->
                Button(
                    onClick = {
                        currentStep = index
                        onTouchDataCollected(
                            TouchDataPoint(
                                x = 0f,
                                y = 0f,
                                pressure = 1.0f,
                                timestamp = System.currentTimeMillis(),
                                type = TouchType.NAVIGATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(step)
                }
            }
        }
    }
}

// Touch data models
