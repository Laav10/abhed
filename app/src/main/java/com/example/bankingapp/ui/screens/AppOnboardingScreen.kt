package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bankingapp.ui.theme.ABHEDColors
import com.example.bankingapp.touch.TouchDataCollector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// Small helper types used by onboarding
enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT
}

data class OnboardingStep(
    val id: Int,
    val title: String,
    val description: String,
    val targetElement: String,
    val arrowDirection: ArrowDirection = ArrowDirection.DOWN
)

// ABHED Behavioral Data Collection Models
data class BehavioralGesture(
    val type: GestureType,
    val startTime: Long,
    val endTime: Long,
    val startPosition: Offset,
    val endPosition: Offset,
    val pressure: Float,
    val velocity: Float,
    val distance: Float,
    val features: FloatArray // 34-dimensional feature vector
)


data class NavigationTransition(
    val fromState: String,
    val toState: String,
    val timestamp: Long,
    val dwellTime: Long
)

enum class GestureType {
    SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN,
    SCROLL_UP, SCROLL_DOWN, TAP, LONG_PRESS, DRAG
}

enum class OnboardingPhase {
    CARD_EXPLORATION, TRANSACTION_REVIEW, QUICK_ACTIONS, NAVIGATION_FLOW, COMPLETED
}

data class BankingCard(
    val id: String,
    val name: String,
    val number: String,
    val balance: String,
    val color: Color,
    val type: String
)

data class MoneyTransaction(
    val id: String,
    val merchant: String,
    val amount: String,
    val date: String,
    val category: String,
    val icon: String,
    val isCredit: Boolean
)

@Composable
fun AppOnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var showTooltip by remember { mutableStateOf(true) }

    val onboardingSteps = listOf(
        OnboardingStep(
            id = 0,
            title = "Welcome to ABHED Banking!",
            description = "Let's take a quick tour of your new secure banking app. Tap anywhere to continue.",
            targetElement = "welcome",
            arrowDirection = ArrowDirection.DOWN
        ),
        OnboardingStep(
            id = 1,
            title = "Your Dashboard",
            description = "Here you can see your account balance and recent transactions at a glance.",
            targetElement = "dashboard",
            arrowDirection = ArrowDirection.UP
        ),
        OnboardingStep(
            id = 2,
            title = "Send Money",
            description = "Tap here to send money to friends, family, or pay bills securely.",
            targetElement = "send_money",
            arrowDirection = ArrowDirection.LEFT
        ),
        OnboardingStep(
            id = 3,
            title = "Transaction History",
            description = "View all your past transactions and track your spending patterns.",
            targetElement = "history",
            arrowDirection = ArrowDirection.RIGHT
        ),
        OnboardingStep(
            id = 4,
            title = "Security First",
            description = "Your biometric authentication keeps your account safe and secure.",
            targetElement = "security",
            arrowDirection = ArrowDirection.UP
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ABHEDColors.Lavender.copy(alpha = 0.3f),
                        ABHEDColors.Periwinkle.copy(alpha = 0.5f),
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.2f)
                    )
                )
            )
            .clickable {
                if (currentStep < onboardingSteps.size - 1) {
                    currentStep++
                } else {
                    onComplete()
                }
            }
    ) {
        // Mock Banking Interface
        MockBankingInterface(
            currentStep = currentStep,
            onboardingSteps = onboardingSteps
        )

        // Tooltip Overlay
        if (showTooltip && currentStep < onboardingSteps.size) {
            OnboardingTooltip(
                step = onboardingSteps[currentStep],
                onNext = {
                    if (currentStep < onboardingSteps.size - 1) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                onSkip = { onComplete() }
            )
        }
    }
}

@Composable
private fun MockBankingInterface(
    currentStep: Int,
    onboardingSteps: List<OnboardingStep>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onboardingSteps.getOrNull(currentStep)?.targetElement == "dashboard")
                        Modifier.border(3.dp, ABHEDColors.LapisLazuli, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Periwinkle
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Good Morning!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Account Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.Charcoal
                )

                Text(
                    text = "₹25,430.50",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Send Money Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onboardingSteps.getOrNull(currentStep)?.targetElement == "send_money")
                            Modifier.border(3.dp, ABHEDColors.LapisLazuli, RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.LightSeaGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💸",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send Money",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Transaction History Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onboardingSteps.getOrNull(currentStep)?.targetElement == "history")
                            Modifier.border(3.dp, ABHEDColors.LapisLazuli, RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.SavoyBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mock transactions
                repeat(3) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        ABHEDColors.Lavender,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (index == 0) "🛒" else if (index == 1) "☕" else "⛽",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (index == 0) "Grocery Store" else if (index == 1) "Coffee Shop" else "Gas Station",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.Charcoal
                                )
                            }
                        }

                        Text(
                            text = if (index == 0) "-₹1,250" else if (index == 1) "-₹180" else "-₹2,500",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ABHEDColors.MarianBlue
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Security Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onboardingSteps.getOrNull(currentStep)?.targetElement == "security")
                        Modifier.border(3.dp, ABHEDColors.LapisLazuli, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.GlaucousMoonstone.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Secured with biometric authentication",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.DeftBlue
                )
            }
        }
    }
}

@Composable
private fun OnboardingTooltip(
    step: OnboardingStep,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        // Tooltip
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(
                    when (step.id) {
                        0 -> Alignment.Center
                        1 -> Alignment.TopCenter
                        2 -> Alignment.CenterEnd
                        3 -> Alignment.CenterStart
                        else -> Alignment.BottomCenter
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.Charcoal
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onSkip
                    ) {
                        Text(
                            text = "Skip Tour",
                            color = ABHEDColors.GlaucousMoonstone
                        )
                    }

                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.LightSeaGreen
                        )
                    ) {
                        Text(
                            text = if (step.id < 4) "Next" else "Get Started",
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Animated Arrow (simplified)
        if (step.id > 0) {
            AnimatedArrow(
                direction = step.arrowDirection,
                modifier = Modifier.align(
                    when (step.arrowDirection) {
                        ArrowDirection.UP -> Alignment.BottomCenter
                        ArrowDirection.DOWN -> Alignment.TopCenter
                        ArrowDirection.LEFT -> Alignment.CenterEnd
                        ArrowDirection.RIGHT -> Alignment.CenterStart
                    }
                )
            )
        }
    }
}

@Composable
private fun AnimatedArrow(
    direction: ArrowDirection,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = when (direction) {
                ArrowDirection.UP -> "⬆️"
                ArrowDirection.DOWN -> "⬇️"
                ArrowDirection.LEFT -> "⬅️"
                ArrowDirection.RIGHT -> "➡️"
            },
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.offset(
                x = if (direction == ArrowDirection.LEFT || direction == ArrowDirection.RIGHT) animatedOffset.dp else 0.dp,
                y = if (direction == ArrowDirection.UP || direction == ArrowDirection.DOWN) animatedOffset.dp else 0.dp
            )
        )
    }
}
