package com.example.bankingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bankingapp.data.models.TouchDataPoint

/**
 * Example of how to integrate the new game-like onboarding flow into your existing app
 * 
 * This shows how to:
 * 1. Use the integrated onboarding flow
 * 2. Collect touch data during onboarding
 * 3. Handle completion and navigation to the main app
 */
@Composable
fun OnboardingIntegrationExample(
    onNavigateToMainApp: () -> Unit,
    context: android.content.Context
) {
    var showOnboarding by remember { mutableStateOf(true) }
    var collectedTouchData by remember { mutableStateOf(listOf<TouchDataPoint>()) }
    
    if (showOnboarding) {
        IntegratedOnboardingFlow(
            onComplete = {
                // Save collected touch data to your database/service
                saveTouchDataToDatabase(collectedTouchData)
                
                // Mark onboarding as complete in preferences
                markOnboardingComplete()
                
                // Navigate to main app
                showOnboarding = false
                onNavigateToMainApp()
            },
            onTouchDataCollected = { touchData ->
                // Collect touch data during onboarding
                collectedTouchData = collectedTouchData + touchData
                
                // Optional: Save data incrementally or process in real-time
                processTouchDataInRealTime(touchData)
            },
            userId = "demo_user",
            context = context
        )
    } else {
        // Main app content would go here
        MainAppContent()
    }
}

/**
 * Alternative: Use individual onboarding components separately
 */
@Composable
fun ModularOnboardingExample(
    onComplete: () -> Unit
) {
    var currentOnboardingStep by remember { mutableStateOf(0) }
    var collectedTouchData by remember { mutableStateOf(listOf<TouchDataPoint>()) }
    
    when (currentOnboardingStep) {
        0 -> {
            // Step 1: Game-like gesture training
            GameLikeOnboardingScreen(
                onComplete = { currentOnboardingStep = 1 },
                onTouchDataCollected = { touchData ->
                    collectedTouchData = collectedTouchData + touchData
                }
            )
        }
        1 -> {
            // Step 2: Banking simulation
            BankingSimulationOnboarding(
                onComplete = { currentOnboardingStep = 2 },
                onTouchDataCollected = { touchData ->
                    collectedTouchData = collectedTouchData + touchData
                }
            )
        }
        2 -> {
            // Step 3: Completion and transition to main app
            OnboardingCompletionScreen(
                totalTouchData = collectedTouchData.size,
                onComplete = onComplete
            )
        }
    }
}

/**
 * Simple completion screen for modular approach
 */
@Composable
private fun OnboardingCompletionScreen(
    totalTouchData: Int,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Onboarding Complete!",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Collected $totalTouchData touch data points",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to Main App")
        }
    }
}

/**
 * Placeholder for main app content
 */
@Composable
private fun MainAppContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to ABHED Banking!",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Helper functions for data management
 */
private fun saveTouchDataToDatabase(touchData: List<TouchDataPoint>) {
    // Implement your database saving logic here
    // Example:
    // touchDataRepository.saveTouchData(touchData)
    // or
    // behavioralAuthService.saveOnboardingData(touchData)
}

private fun markOnboardingComplete() {
    // Implement your preference management logic here
    // Example:
    // preferenceManager.setOnboardingComplete(true)
}

private fun processTouchDataInRealTime(touchData: TouchDataPoint) {
    // Implement real-time processing if needed
    // Example:
    // behavioralAuthService.processTouchData(touchData)
    // mlModelManager.updateModel(touchData)
}

/**
 * Usage in your MainActivity or main composable:
 * 
 * @Composable
 * fun MainApp() {
 *     var showOnboarding by remember { mutableStateOf(shouldShowOnboarding()) }
 *     val context = androidx.compose.ui.platform.LocalContext.current
 *     
 *     if (showOnboarding) {
 *         IntegratedOnboardingFlow(
 *             onComplete = { showOnboarding = false },
 *             onTouchDataCollected = { touchData ->
 *                 saveTouchData(touchData) // Your data collection logic
 *             },
 *             userId = "demo_user", // Replace with actual user ID
 *             context = context
 *         )
 *     } else {
 *         MainBankingDashboard(
 *             onLogout = { showOnboarding = true },
 *             // ... other parameters
 *         )
 *     }
 * }
 * 
 * private fun shouldShowOnboarding(): Boolean {
 *     // Check if user has completed onboarding
 *     return !preferenceManager.isOnboardingComplete()
 * }
 */
