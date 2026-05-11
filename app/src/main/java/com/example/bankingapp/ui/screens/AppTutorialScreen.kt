package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bankingapp.ui.theme.ABHEDColors
import com.example.bankingapp.touch.TouchDataCollector
import kotlinx.coroutines.delay

data class TutorialStep(
    val id: Int,
    val title: String,
    val instruction: String,
    val highlightArea: String = "",
    val expectedAction: String = "",
    val isCompleted: Boolean = false
)

@Composable
fun AppTutorialScreen(
    onTutorialComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableStateOf(0) }
    var touchDataCollector by remember { mutableStateOf(TouchDataCollector(context)) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    var showOverlay by remember { mutableStateOf(true) }
    
    val tutorialSteps = remember {
        listOf(
            TutorialStep(
                id = 1,
                title = "Welcome to ABHED Banking",
                instruction = "This tutorial will guide you through using the app. Tap 'Continue' to start learning.",
                expectedAction = "tap_continue"
            ),
            TutorialStep(
                id = 2,
                title = "Navigation Basics",
                instruction = "Let's learn navigation. Tap the 'Account Balance' card below to view your balance.",
                highlightArea = "balance_card",
                expectedAction = "tap_balance"
            ),
            TutorialStep(
                id = 3,
                title = "Scrolling Through Content",
                instruction = "Great! Now scroll down through your recent transactions to see your spending history.",
                highlightArea = "transaction_list",
                expectedAction = "scroll_transactions"
            ),
            TutorialStep(
                id = 4,
                title = "Using Action Buttons",
                instruction = "Perfect! Now tap the 'Send Money' button to learn about transfers.",
                highlightArea = "send_money_button",
                expectedAction = "tap_send_money"
            ),
            TutorialStep(
                id = 5,
                title = "Menu Navigation",
                instruction = "Excellent! Tap the menu icon (☰) in the top left to see more options.",
                highlightArea = "menu_button",
                expectedAction = "tap_menu"
            ),
            TutorialStep(
                id = 6,
                title = "Settings Access",
                instruction = "Good! Now tap 'Settings' in the menu to access your preferences.",
                highlightArea = "settings_option",
                expectedAction = "tap_settings"
            ),
            TutorialStep(
                id = 7,
                title = "Tutorial Complete",
                instruction = "Congratulations! You've learned the basics of ABHED Banking. You're ready to use the app.",
                expectedAction = "complete"
            )
        )
    }
    
    val currentStep = tutorialSteps[currentStepIndex]
    val progress = (completedSteps.size.toFloat() / tutorialSteps.size.toFloat())
    
    LaunchedEffect(completedSteps.size) {
        if (completedSteps.size == tutorialSteps.size) {
            delay(2000)
            touchDataCollector.saveCollectedData()
            onTutorialComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        ABHEDColors.Lavender.copy(alpha = 0.3f),
                        Color.White
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchDataCollector.recordTap(
                        x = offset.x / size.width,
                        y = offset.y / size.height,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    touchDataCollector.recordScroll(
                        scrollDistance = change.position.y,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
    ) {
        // Main app content (simulated banking interface)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // App header
            TutorialAppHeader(
                currentStep = currentStep,
                onMenuClick = {
                    if (currentStep.expectedAction == "tap_menu") {
                        completedSteps = completedSteps + currentStep.id
                        if (currentStepIndex < tutorialSteps.size - 1) {
                            currentStepIndex++
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main content area
            when (currentStepIndex) {
                0, 1 -> TutorialDashboardContent(
                    currentStep = currentStep,
                    onBalanceClick = {
                        if (currentStep.expectedAction == "tap_balance") {
                            completedSteps = completedSteps + currentStep.id
                            currentStepIndex++
                        }
                    }
                )
                2, 3 -> TutorialTransactionContent(
                    currentStep = currentStep,
                    onSendMoneyClick = {
                        if (currentStep.expectedAction == "tap_send_money") {
                            completedSteps = completedSteps + currentStep.id
                            currentStepIndex++
                        }
                    },
                    onScroll = {
                        if (currentStep.expectedAction == "scroll_transactions") {
                            completedSteps = completedSteps + currentStep.id
                            currentStepIndex++
                        }
                    }
                )
                4, 5 -> TutorialMenuContent(
                    currentStep = currentStep,
                    onSettingsClick = {
                        if (currentStep.expectedAction == "tap_settings") {
                            completedSteps = completedSteps + currentStep.id
                            currentStepIndex++
                        }
                    }
                )
                else -> TutorialCompleteContent()
            }
        }
        
        // Overlay with instructions
        if (showOverlay && currentStepIndex < tutorialSteps.size - 1) {
            TutorialOverlay(
                step = currentStep,
                progress = progress,
                onContinue = {
                    if (currentStep.expectedAction == "tap_continue") {
                        completedSteps = completedSteps + currentStep.id
                        currentStepIndex++
                    }
                },
                onSkip = {
                    showOverlay = false
                    completedSteps = (1..tutorialSteps.size).toSet()
                }
            )
        }
    }
}

@Composable
private fun TutorialAppHeader(
    currentStep: TutorialStep,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .then(
                    if (currentStep.highlightArea == "menu_button") {
                        Modifier.background(
                            ABHEDColors.LightSeaGreen.copy(alpha = 0.3f),
                            CircleShape
                        )
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = ABHEDColors.LapisLazuli
            )
        }
        
        Text(
            text = "ABHED Banking",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )
        )
        
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = ABHEDColors.LapisLazuli
            )
        }
    }
}

@Composable
private fun TutorialDashboardContent(
    currentStep: TutorialStep,
    onBalanceClick: () -> Unit
) {
    Column {
        // Balance card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBalanceClick() }
                .then(
                    if (currentStep.highlightArea == "balance_card") {
                        Modifier.border(
                            3.dp,
                            ABHEDColors.LightSeaGreen,
                            RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ABHEDColors.LapisLazuli,
                            ABHEDColors.LightSeaGreen
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Account Balance",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$12,450.67",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton("Send", Icons.Default.Send)
            QuickActionButton("Receive", Icons.Default.GetApp)
            QuickActionButton("Pay Bills", Icons.Default.Receipt)
        }
    }
}

@Composable
private fun TutorialTransactionContent(
    currentStep: TutorialStep,
    onSendMoneyClick: () -> Unit,
    onScroll: () -> Unit
) {
    var hasScrolled by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (currentStep.highlightArea == "transaction_list") {
                        Modifier.border(
                            2.dp,
                            ABHEDColors.LightSeaGreen,
                            RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                )
                .pointerInput(Unit) {
                    detectDragGestures { _, _ ->
                        if (!hasScrolled && currentStep.expectedAction == "scroll_transactions") {
                            hasScrolled = true
                            onScroll()
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(10) { index ->
                TransactionItem(
                    title = "Transaction ${index + 1}",
                    amount = "-$${(10..200).random()}.${(10..99).random()}",
                    date = "Dec ${(1..30).random()}"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSendMoneyClick,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (currentStep.highlightArea == "send_money_button") {
                        Modifier.border(
                            3.dp,
                            ABHEDColors.LightSeaGreen,
                            RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Send Money", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TutorialMenuContent(
    currentStep: TutorialStep,
    onSettingsClick: () -> Unit
) {
    Column {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val menuItems = listOf(
            "Profile" to Icons.Default.Person,
            "Settings" to Icons.Default.Settings,
            "Help" to Icons.Default.Help,
            "About" to Icons.Default.Info
        )
        
        menuItems.forEach { (title, icon) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (title == "Settings") onSettingsClick()
                    }
                    .then(
                        if (title == "Settings" && currentStep.highlightArea == "settings_option") {
                            Modifier.border(
                                3.dp,
                                ABHEDColors.LightSeaGreen,
                                RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.Lavender.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = ABHEDColors.LapisLazuli
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = ABHEDColors.LapisLazuli
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TutorialCompleteContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Complete",
            modifier = Modifier.size(80.dp),
            tint = ABHEDColors.LightSeaGreen
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Tutorial Complete!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    progress: Float,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .zIndex(10f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip Tutorial", color = ABHEDColors.GlaucousMoonstone)
                    }
                    
                    if (step.expectedAction == "tap_continue") {
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ABHEDColors.LightSeaGreen
                            )
                        ) {
                            Text("Continue", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ABHEDColors.LapisLazuli
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.Charcoal
        )
    }
}

@Composable
private fun TransactionItem(title: String, amount: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.Lavender.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ABHEDColors.LapisLazuli
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ABHEDColors.Charcoal
            )
        }
    }
}
