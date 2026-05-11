package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.abs

data class OnboardingTask(
    val id: Int,
    val title: String,
    val description: String,
    val instruction: String,
    val type: TaskType,
    val isCompleted: Boolean = false
)

enum class TaskType {
    TAP_BUTTONS,
    SCROLL_LIST,
    SWIPE_CARDS,
    PINCH_ZOOM,
    LONG_PRESS
}

@Composable
fun TouchOnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentTaskIndex by remember { mutableStateOf(0) }
    var touchDataCollector by remember { mutableStateOf(TouchDataCollector(context)) }
    var completedTasks by remember { mutableStateOf(setOf<Int>()) }
    var showProgress by remember { mutableStateOf(false) }
    
    val onboardingTasks = remember {
        listOf(
            OnboardingTask(
                id = 1,
                title = "Welcome to ABHED!",
                description = "Let's learn how you interact with your device",
                instruction = "Tap the 'Start Learning' button below",
                type = TaskType.TAP_BUTTONS
            ),
            OnboardingTask(
                id = 2,
                title = "Navigation Practice",
                description = "Practice tapping different buttons",
                instruction = "Tap each colored button below",
                type = TaskType.TAP_BUTTONS
            ),
            OnboardingTask(
                id = 3,
                title = "Scrolling Behavior",
                description = "Show us how you scroll through content",
                instruction = "Scroll through this list of banking features",
                type = TaskType.SCROLL_LIST
            ),
            OnboardingTask(
                id = 4,
                title = "Card Interactions",
                description = "Swipe through account cards",
                instruction = "Swipe left and right on the cards below",
                type = TaskType.SWIPE_CARDS
            ),
            OnboardingTask(
                id = 5,
                title = "Gesture Recognition",
                description = "Test long press interactions",
                instruction = "Long press on the security icon",
                type = TaskType.LONG_PRESS
            )
        )
    }
    
    val currentTask = onboardingTasks[currentTaskIndex]
    val progress = (completedTasks.size.toFloat() / onboardingTasks.size.toFloat())
    
    LaunchedEffect(completedTasks.size) {
        if (completedTasks.size == onboardingTasks.size) {
            showProgress = true
            delay(2000)
            touchDataCollector.saveCollectedData()
            onOnboardingComplete()
        }
    }
    
    Column(
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
            .padding(24.dp)
    ) {
        // Header with progress
        OnboardingHeader(
            progress = progress,
            currentStep = completedTasks.size + 1,
            totalSteps = onboardingTasks.size
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (showProgress) {
            OnboardingCompleteScreen()
        } else {
            // Current task content
            OnboardingTaskContent(
                task = currentTask,
                touchDataCollector = touchDataCollector,
                onTaskCompleted = { taskId ->
                    completedTasks = completedTasks + taskId
                    if (currentTaskIndex < onboardingTasks.size - 1) {
                        currentTaskIndex++
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    progress: Float,
    currentStep: Int,
    totalSteps: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Touch Learning",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LapisLazuli
                )
            )
            
            Text(
                text = "$currentStep / $totalSteps",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ABHEDColors.GlaucousMoonstone
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = ABHEDColors.LightSeaGreen,
            trackColor = ABHEDColors.Lavender
        )
    }
}

@Composable
private fun OnboardingTaskContent(
    task: OnboardingTask,
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Task description card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ABHEDColors.Periwinkle.copy(alpha = 0.3f),
                            ABHEDColors.Lavender.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = task.instruction,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ABHEDColors.LightSeaGreen
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Interactive task area
        when (task.type) {
            TaskType.TAP_BUTTONS -> TapButtonsTask(touchDataCollector, onTaskCompleted, task.id)
            TaskType.SCROLL_LIST -> ScrollListTask(touchDataCollector, onTaskCompleted, task.id)
            TaskType.SWIPE_CARDS -> SwipeCardsTask(touchDataCollector, onTaskCompleted, task.id)
            TaskType.LONG_PRESS -> LongPressTask(touchDataCollector, onTaskCompleted, task.id)
            TaskType.PINCH_ZOOM -> PinchZoomTask(touchDataCollector, onTaskCompleted, task.id)
        }
    }
}

@Composable
private fun TapButtonsTask(
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit,
    taskId: Int
) {
    var tappedButtons by remember { mutableStateOf(setOf<Int>()) }
    val requiredTaps = if (taskId == 1) 1 else 4
    
    LaunchedEffect(tappedButtons.size) {
        if (tappedButtons.size >= requiredTaps) {
            delay(500)
            onTaskCompleted(taskId)
        }
    }
    
    if (taskId == 1) {
        // Single start button
        Button(
            onClick = { 
                touchDataCollector.recordTap(0.5f, 0.5f, System.currentTimeMillis())
                tappedButtons = tappedButtons + 1
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ABHEDColors.LightSeaGreen,
                            ABHEDColors.LapisLazuli
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Start Learning",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    } else {
        // Multiple colored buttons
        val buttonColors = listOf(
            ABHEDColors.LightSeaGreen,
            ABHEDColors.LapisLazuli,
            ABHEDColors.SavoyBlue,
            ABHEDColors.MarianBlue
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(buttonColors.size) { index ->
                val isCompleted = tappedButtons.contains(index)
                Button(
                    onClick = { 
                        touchDataCollector.recordTap(
                            x = 0.5f, 
                            y = 0.3f + (index * 0.1f), 
                            timestamp = System.currentTimeMillis()
                        )
                        tappedButtons = tappedButtons + index
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) 
                            ABHEDColors.LightSeaGreen 
                        else 
                            buttonColors[index]
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Button ${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollListTask(
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit,
    taskId: Int
) {
    var scrollDistance by remember { mutableStateOf(0f) }
    val requiredScroll = 1000f // pixels
    
    LaunchedEffect(scrollDistance) {
        if (abs(scrollDistance) >= requiredScroll) {
            onTaskCompleted(taskId)
        }
    }
    
    val bankingFeatures = listOf(
        "Account Balance" to "View your current balance instantly",
        "Transaction History" to "Track all your spending and income",
        "Bill Payments" to "Pay bills with just a few taps",
        "Money Transfer" to "Send money to friends and family",
        "Investment Portfolio" to "Monitor your investment performance",
        "Savings Goals" to "Set and track your financial goals",
        "Credit Score" to "Check your credit score for free",
        "Loan Applications" to "Apply for loans digitally",
        "Card Management" to "Manage your debit and credit cards",
        "Budget Tracker" to "Track your monthly spending habits"
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    scrollDistance += change.position.y
                    touchDataCollector.recordScroll(
                        scrollDistance, 
                        System.currentTimeMillis()
                    )
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bankingFeatures) { (title, description) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.Lavender.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ABHEDColors.LapisLazuli
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.Charcoal
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeCardsTask(
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit,
    taskId: Int
) {
    var swipeCount by remember { mutableStateOf(0) }
    val requiredSwipes = 3
    
    LaunchedEffect(swipeCount) {
        if (swipeCount >= requiredSwipes) {
            onTaskCompleted(taskId)
        }
    }
    
    val accounts = listOf(
        "Checking Account" to "$2,450.00",
        "Savings Account" to "$15,230.00",
        "Credit Card" to "$1,200.00"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        accounts.forEachIndexed { index, (accountName, balance) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            touchDataCollector.recordTap(
                                offset.x, 
                                offset.y, 
                                System.currentTimeMillis()
                            )
                            swipeCount++
                            if (swipeCount >= requiredSwipes) {
                                onTaskCompleted(taskId)
                            }
                        }
                    }
                    .background(
                        brush = Brush.horizontalGradient(
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = balance,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    
                    if (swipeCount > index) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongPressTask(
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit,
    taskId: Int
) {
    var isLongPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLongPressed) {
        if (isLongPressed) {
            delay(500)
            onTaskCompleted(taskId)
        }
    }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.8f),
                        ABHEDColors.LapisLazuli.copy(alpha = 0.6f)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        touchDataCollector.recordLongPress(
                            x = 0.5f,
                            y = 0.5f,
                            duration = 1000L,
                            timestamp = System.currentTimeMillis()
                        )
                        isLongPressed = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLongPressed) Icons.Default.Check else Icons.Default.Security,
            contentDescription = "Security",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun PinchZoomTask(
    touchDataCollector: TouchDataCollector,
    onTaskCompleted: (Int) -> Unit,
    taskId: Int
) {
    // Placeholder for pinch zoom task
    // This would require more complex gesture detection
    LaunchedEffect(Unit) {
        delay(2000)
        onTaskCompleted(taskId)
    }
    
    Text("Pinch zoom task placeholder")
}

@Composable
private fun OnboardingCompleteScreen() {
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
            text = "Touch Learning Complete!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "We've learned your unique touch patterns.\nYour device is now ready for secure authentication.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = ABHEDColors.Charcoal
        )
    }
}
