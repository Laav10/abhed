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
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.abs
import kotlin.math.hypot

// Banking simulation scenarios that mirror the actual dashboard
data class BankingScenario(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tutorialSteps: List<TutorialStep>,
    val targetInteractions: Int
)



@Composable
fun BankingSimulationOnboarding(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit = {}
) {
    var currentScenario by remember { mutableStateOf(0) }
    var currentStep by remember { mutableStateOf(0) }
    var showTutorial by remember { mutableStateOf(true) }
    var completedInteractions by remember { mutableStateOf(0) }
    var totalTouchDataCollected by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    
    val bankingScenarios = remember {
        listOf(
            BankingScenario(
                id = "dashboard_exploration",
                title = "Explore Your Dashboard",
                description = "Learn to navigate your banking interface",
                icon = Icons.Default.Dashboard,
                targetInteractions = 8,
                tutorialSteps = listOf(
                    TutorialStep(
                        id = 1,
                        title = "Account Balances",
                        instruction = "Welcome! Let's explore your banking dashboard. First, tap on 'Account Balances' to see your money.",
                        highlightArea = "Account Balances",
                        expectedAction = "Tap"
                    ),
                    TutorialStep(
                        id = 2,
                        title = "Balance Toggle",
                        instruction = "Great! Now try the visibility toggle to hide or show your balance.",
                        highlightArea = "Balance Toggle",
                        expectedAction = "Tap"
                    ),
                    TutorialStep(
                        id = 3,
                        title = "Quick Actions",
                        instruction = "Perfect! Now scroll through the Quick Actions section to see all available services.",
                        highlightArea = "Quick Actions",
                        expectedAction = "Swipe"
                    ),
                    TutorialStep(
                        id = 4,
                        title = "Fund Transfer",
                        instruction = "Excellent! Try tapping on 'Fund Transfer' to learn about money transfers.",
                        highlightArea = "Fund Transfer",
                        expectedAction = "Tap"
                    )
                )
            ),
            BankingScenario(
                id = "money_transfer",
                title = "Money Transfer Tutorial",
                description = "Practice sending money safely",
                icon = Icons.Default.Send,
                targetInteractions = 6,
                tutorialSteps = listOf(
                    TutorialStep(
                        id = 1,
                        title = "Send Money",
                        instruction = "Now let's learn money transfers! Tap on 'Send Money' to start.",
                        highlightArea = "Send Money",
                        expectedAction = "Tap"
                    ),
                    TutorialStep(
                        id = 2,
                        title = "Amount Field",
                        instruction = "Enter the amount you want to send. Try typing '100'.",
                        highlightArea = "Amount Field",
                        expectedAction = "Tap"
                    ),
                    TutorialStep(
                        id = 3,
                        title = "Choose Contact",
                        instruction = "Now select a recipient. Tap on 'Choose Contact'.",
                        highlightArea = "Choose Contact",
                        expectedAction = "Tap"
                    )
                )
            ),
            BankingScenario(
                id = "security_features",
                title = "Security Features",
                description = "Learn about your security settings",
                icon = Icons.Default.Security,
                targetInteractions = 4,
                tutorialSteps = listOf(
                    TutorialStep(
                        id = 1,
                        title = "Security Status",
                        instruction = "Let's explore security features! Tap on the security status indicator.",
                        highlightArea = "Security Status",
                        expectedAction = "Tap"
                    ),
                    TutorialStep(
                        id = 2,
                        title = "Security Icon",
                        instruction = "Great! Now long press on the security icon to see advanced options.",
                        highlightArea = "Security Icon",
                        expectedAction = "LongPress"
                    )
                )
            )
        )
    }
    
    val currentScenarioData = bankingScenarios[currentScenario]
    val currentTutorialStep = currentScenarioData.tutorialSteps[currentStep]
    val scenarioProgress = (completedInteractions.toFloat() / currentScenarioData.targetInteractions.toFloat())
    val overallProgress = (currentScenario.toFloat() / bankingScenarios.size.toFloat())
    
    // Auto-advance logic
    LaunchedEffect(completedInteractions) {
        if (completedInteractions >= currentScenarioData.targetInteractions) {
            if (currentScenario < bankingScenarios.size - 1) {
                coroutineScope.launch {
                    delay(2000)
                    currentScenario++
                    currentStep = 0
                    completedInteractions = 0
                    showTutorial = true
                }
            } else {
                coroutineScope.launch {
                    delay(2000)
                    onComplete()
                }
            }
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
    ) {
        // Progress header
        BankingSimulationHeader(
            currentScenario = currentScenarioData,
            scenarioProgress = scenarioProgress,
            overallProgress = overallProgress,
            completedInteractions = completedInteractions,
            targetInteractions = currentScenarioData.targetInteractions
        )
        
        // Main simulation area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Tutorial overlay
            if (showTutorial) {
                TutorialOverlay(
                    text = currentTutorialStep.instruction,
                    targetPosition = Offset(200f, 300f),
                    arrowDirection = ArrowDirection.DOWN,
                    isVisible = showTutorial,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            
            // Simulated banking interface
            when (currentScenarioData.id) {
                "dashboard_exploration" -> SimulatedDashboardInterface(
                    onInteraction = { touchData ->
                        onTouchDataCollected(touchData)
                        totalTouchDataCollected++
                        completedInteractions++
                        showTutorial = false
                        
                        // Auto-advance to next tutorial step
                        if (currentStep < currentScenarioData.tutorialSteps.size - 1) {
                            coroutineScope.launch {
                                delay(1000)
                                currentStep++
                                showTutorial = true
                            }
                        }
                    }
                )
                "money_transfer" -> SimulatedTransferInterface(
                    onInteraction = { touchData ->
                        onTouchDataCollected(touchData)
                        totalTouchDataCollected++
                        completedInteractions++
                        showTutorial = false
                        
                        if (currentStep < currentScenarioData.tutorialSteps.size - 1) {
                            coroutineScope.launch {
                                delay(1000)
                                currentStep++
                                showTutorial = true
                            }
                        }
                    }
                )
                "security_features" -> SimulatedSecurityInterface(
                    onInteraction = { touchData ->
                        onTouchDataCollected(touchData)
                        totalTouchDataCollected++
                        completedInteractions++
                        showTutorial = false
                        
                        if (currentStep < currentScenarioData.tutorialSteps.size - 1) {
                            coroutineScope.launch {
                                delay(1000)
                                currentStep++
                                showTutorial = true
                            }
                        }
                    }
                )
            }
        }
        
        // Bottom stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("Touch Data", totalTouchDataCollected.toString())
                StatItem("Interactions", completedInteractions.toString())
                StatItem("Progress", "${(overallProgress * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun BankingSimulationHeader(
    currentScenario: BankingScenario,
    scenarioProgress: Float,
    overallProgress: Float,
    completedInteractions: Int,
    targetInteractions: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = currentScenario.icon,
                        contentDescription = currentScenario.title,
                        tint = ABHEDColors.LightSeaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentScenario.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ABHEDColors.Charcoal
                    )
                }
                
                Text(
                    text = "$completedInteractions/$targetInteractions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ABHEDColors.LightSeaGreen
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = currentScenario.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ABHEDColors.GlaucousMoonstone
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bars
            Column {
                Text(
                    text = "Scenario Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal
                )
                LinearProgressIndicator(
                    progress = scenarioProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ABHEDColors.LightSeaGreen,
                    trackColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal
                )
                LinearProgressIndicator(
                    progress = overallProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ABHEDColors.Periwinkle,
                    trackColor = ABHEDColors.Periwinkle.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SimulatedDashboardInterface(
    onInteraction: (TouchDataPoint) -> Unit
) {
    var showBalance by remember { mutableStateOf(true) }
    var selectedAccount by remember { mutableStateOf("Savings") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Account Balances Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput("Account Balances") {
                        detectTapGestures(
                            onTap = { offset ->
                                onInteraction(
                                    TouchDataPoint(
                                        x = offset.x,
                                        y = offset.y,
                                        pressure = 0.5f,
                                        timestamp = System.currentTimeMillis(),
                                        type = TouchType.TAP
                                    )
                                )
                            }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Account Balances",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Savings Account Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput("Savings Account") {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            selectedAccount = "Savings"
                                            onInteraction(
                                                TouchDataPoint(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    pressure = 0.5f,
                                                    timestamp = System.currentTimeMillis(),
                                                    type = TouchType.TAP
                                                )
                                            )
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAccount == "Savings") 
                                    ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                                else ABHEDColors.Periwinkle.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (selectedAccount == "Savings") ABHEDColors.LightSeaGreen else ABHEDColors.LapisLazuli.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Savings Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ABHEDColors.DeftBlue
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showBalance) "₹45,678.90" else "••••••••",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ABHEDColors.LapisLazuli
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    IconButton(
                                        onClick = {
                                            showBalance = !showBalance
                                            onInteraction(
                                                TouchDataPoint(
                                                    x = 0f,
                                                    y = 0f,
                                                    pressure = 0.5f,
                                                    timestamp = System.currentTimeMillis(),
                                                    type = TouchType.TAP
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            "Toggle balance visibility",
                                            modifier = Modifier.size(16.dp),
                                            tint = ABHEDColors.GlaucousMoonstone
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "XXXX1234",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.GlaucousMoonstone
                                )
                            }
                        }
                        
                        // Current Account Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput("Current Account") {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            selectedAccount = "Current"
                                            onInteraction(
                                                TouchDataPoint(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    pressure = 0.5f,
                                                    timestamp = System.currentTimeMillis(),
                                                    type = TouchType.TAP
                                                )
                                            )
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAccount == "Current") 
                                    ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                                else ABHEDColors.Periwinkle.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (selectedAccount == "Current") ABHEDColors.LightSeaGreen else ABHEDColors.LapisLazuli.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Current Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ABHEDColors.DeftBlue
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (showBalance) "₹1,23,456.78" else "••••••••",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.LapisLazuli
                                )
                                
                                Text(
                                    text = "XXXX5678",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.GlaucousMoonstone
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            // Quick Actions Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput("Quick Actions") {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onInteraction(
                                    TouchDataPoint(
                                        x = offset.x,
                                        y = offset.y,
                                        pressure = 0.6f,
                                        timestamp = System.currentTimeMillis(),
                                        type = TouchType.SWIPE_UP
                                    )
                                )
                            },
                            onDrag = { _, _ -> },
                            onDragEnd = { }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(
                            ServiceItem(Icons.Default.Send, "Fund Transfer"),
                            ServiceItem(Icons.Default.Phone, "Pay to Mobile"),
                            ServiceItem(Icons.Default.Receipt, "Bill Pay"),
                            ServiceItem(Icons.Default.People, "Beneficiaries")
                        )) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .pointerInput(item.title) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                onInteraction(
                                                    TouchDataPoint(
                                                        type = TouchType.TAP,
                                                        x = offset.x,
                                                        y = offset.y,
                                                        pressure = 0.5f,
                                                        timestamp = System.currentTimeMillis(),
                
                                                    )
                                                )
                                            }
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = ABHEDColors.Lavender.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = ABHEDColors.LightSeaGreen
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium,
                                        color = ABHEDColors.Charcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatedTransferInterface(
    onInteraction: (TouchDataPoint) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Send Money",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Amount field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = "Amount")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput("Amount Field") {
                                detectTapGestures(
                                    onTap = { offset ->
                                        onInteraction(
                                            TouchDataPoint(
                                                type = TouchType.TAP,
                                                x = offset.x,
                                                y = offset.y,
                                                pressure = 0.5f,
                                                timestamp = System.currentTimeMillis(),
        
                                            )
                                        )
                                    }
                                )
                            },
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Contact selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput("Choose Contact") {
                                detectTapGestures(
                                    onTap = { offset ->
                                        selectedContact = "John Doe"
                                        onInteraction(
                                            TouchDataPoint(
                                                type = TouchType.TAP,
                                                x = offset.x,
                                                y = offset.y,
                                                pressure = 0.5f,
                                                timestamp = System.currentTimeMillis(),
        
                                            )
                                        )
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedContact.isNotEmpty()) 
                                ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                            else ABHEDColors.Lavender.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (selectedContact.isNotEmpty()) ABHEDColors.LightSeaGreen else ABHEDColors.GlaucousMoonstone
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Contact",
                                tint = ABHEDColors.LightSeaGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (selectedContact.isNotEmpty()) selectedContact else "Choose Contact",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedContact.isNotEmpty()) ABHEDColors.Charcoal else ABHEDColors.GlaucousMoonstone
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatedSecurityInterface(
    onInteraction: (TouchDataPoint) -> Unit
) {
    var showSecurityDetails by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Security Center",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Security status
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput("Security Status") {
                                detectTapGestures(
                                    onTap = { offset ->
                                        showSecurityDetails = !showSecurityDetails
                                        onInteraction(
                                            TouchDataPoint(
                                                type = TouchType.TAP,
                                                x = offset.x,
                                                y = offset.y,
                                                pressure = 0.5f,
                                                timestamp = System.currentTimeMillis(),
        
                                            )
                                        )
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(2.dp, ABHEDColors.LightSeaGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Security",
                                tint = ABHEDColors.LightSeaGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Security Status: SECURE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.LightSeaGreen
                                )
                                Text(
                                    text = "Trust Score: 95%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ABHEDColors.Charcoal
                                )
                            }
                        }
                    }
                    
                    if (showSecurityDetails) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput("Security Icon") {
                                    detectTapGestures(
                                        onLongPress = { offset ->
                                            onInteraction(
                                                TouchDataPoint(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    pressure = 0.8f,
                                                    timestamp = System.currentTimeMillis(),
                                                    type = TouchType.LONG_PRESS
                                                )
                                            )
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = ABHEDColors.Periwinkle.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Advanced Security Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.Charcoal
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Long press on this card to access advanced settings",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ABHEDColors.GlaucousMoonstone
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ABHEDColors.LightSeaGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.GlaucousMoonstone
        )
    }
}


