package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.bankingapp.R
import com.example.bankingapp.ui.viewmodels.RegistrationStep
import com.example.bankingapp.ui.viewmodels.RegistrationUiState
import com.example.bankingapp.ui.viewmodels.RegistrationViewModel
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// Color Palette
private val LightSeaGreen = Color(0xFF2EC4B6)
private val LapisLazuli = Color(0xFF33658A)
private val Charcoal = Color(0xFF2F4858)
private val MarianBlue = Color(0xFF2B4570)
private val GlaucousMoonstone = Color(0xFF66999B)
private val DeftBlue = Color(0xFF2B3A67)
private val SavoyBlue = Color(0xFF5158BB)
private val Lavender = Color(0xFFDEE2FF)
private val Periwinkle = Color(0xFFA6B1E1)

// Touch Collection Components
@Composable
private fun TouchCollectingEnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType,
    onTouchDataCollected: (TouchDataPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTextLength by remember { mutableIntStateOf(value.length) }
    
    LaunchedEffect(value.length) {
        if (value.length != lastTextLength) {
            // Collect typing pattern data
            onTouchDataCollected(
                TouchDataPoint(
                    x = value.length * 15f, // Approximate cursor position
                    y = 0f,
                    pressure = 1.0f,
                    timestamp = System.currentTimeMillis(),
                    type = TouchType.TAP,
                    velocity = 0f,
                    sessionId = "registration_typing",
                    userId = "registration_user",
                    deviceUUID = "registration_device",
                    additionalData = mapOf(
                        "scenario" to if (value.length > lastTextLength) "typing" else "deletion",
                        "field" to label,
                        "length" to value.length.toString()
                    )
                )
            )
            lastTextLength = value.length
        }
    }

    Column(
        modifier = modifier.pointerInput("text_field_touch") {
            detectTapGestures(
                onTap = { offset ->
                    onTouchDataCollected(
                        TouchDataPoint(
                            x = offset.x,
                            y = offset.y,
                            pressure = 1.0f,
                            timestamp = System.currentTimeMillis(),
                            type = TouchType.TAP,
                            velocity = 0f,
                            sessionId = "registration_field_tap",
                            userId = "registration_user",
                            deviceUUID = "registration_device",
                            additionalData = mapOf(
                                "scenario" to "field_tap",
                                "field" to label
                            )
                        )
                    )
                }
            )
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Charcoal
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = GlaucousMoonstone.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LapisLazuli,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LapisLazuli,
                unfocusedBorderColor = Periwinkle.copy(alpha = 0.5f),
                focusedLabelColor = LapisLazuli,
                unfocusedLabelColor = GlaucousMoonstone,
                cursorColor = LapisLazuli
            )
        )
    }
}

@Composable
private fun TouchCollectingOTPInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var lastOtpLength by remember { mutableIntStateOf(value.length) }
    
    LaunchedEffect(value.length) {
        if (value.length != lastOtpLength && value.length <= 6) {
            // Collect OTP digit entry timing
            onTouchDataCollected(
                TouchDataPoint(
                    x = value.length * 60f, // Position based on digit number
                    y = 0f,
                    pressure = 1.0f,
                    timestamp = System.currentTimeMillis(),
                    type = TouchType.TAP,
                    velocity = 0f,
                    sessionId = "registration_otp",
                    userId = "registration_user",
                    deviceUUID = "registration_device",
                    additionalData = mapOf(
                        "scenario" to "otp_digit_${value.length}_${if (value.length > lastOtpLength) "entry" else "deletion"}",
                        "digit_position" to value.length.toString()
                    )
                )
            )
            lastOtpLength = value.length
        }
    }
    
    Column {
                Text(
            text = "Enter OTP Code",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Charcoal
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput("otp_touch") {
                    detectTapGestures(
                        onTap = { offset ->
                            onTouchDataCollected(
                                TouchDataPoint(
                                    x = offset.x,
                                    y = offset.y,
                                    pressure = 1.0f,
                                    timestamp = System.currentTimeMillis(),
                                    type = TouchType.TAP,
                                    velocity = 0f,
                                    sessionId = "registration_otp_tap",
                                    userId = "registration_user",
                                    deviceUUID = "registration_device",
                                    additionalData = mapOf(
                                        "scenario" to "otp_field_tap"
                                    )
                                )
                            )
                        }
                    )
                },
            placeholder = {
                Text(
                    text = "123456",
                    color = GlaucousMoonstone.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 4.sp
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SavoyBlue,
                    modifier = Modifier.size(20.dp)
                )
            },
            enabled = isEnabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SavoyBlue,
                unfocusedBorderColor = Periwinkle.copy(alpha = 0.5f),
                disabledBorderColor = GlaucousMoonstone.copy(alpha = 0.3f),
                focusedLabelColor = SavoyBlue,
                unfocusedLabelColor = GlaucousMoonstone,
                cursorColor = SavoyBlue
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun TouchCollectingButton(
    onClick: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = {
            // Collect button press data
            onTouchDataCollected(
                TouchDataPoint(
                    x = 0f,
                    y = 0f,
                    pressure = 1.5f, // Higher pressure for button press
                    timestamp = System.currentTimeMillis(),
                    type = TouchType.BUTTON_PRESS,
                    velocity = 0f,
                    sessionId = "registration_button",
                    userId = "registration_user",
                    deviceUUID = "registration_device",
                    additionalData = mapOf(
                        "scenario" to "button_press"
                    )
                )
            )
            onClick()
        },
        enabled = enabled,
        modifier = modifier.pointerInput("button_touch") {
            detectTapGestures(
                onPress = { offset ->
                    onTouchDataCollected(
                        TouchDataPoint(
                            x = offset.x,
                            y = offset.y,
                            pressure = 1.3f,
                            timestamp = System.currentTimeMillis(),
                            type = TouchType.LONG_PRESS,
                            velocity = 0f,
                            sessionId = "registration_button_hold",
                            userId = "registration_user",
                            deviceUUID = "registration_device",
                            additionalData = mapOf(
                                "scenario" to "button_hold"
                            )
                        )
                    )
                    tryAwaitRelease()
                }
            )
        },
        colors = colors,
        content = content
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onRegistrationComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == RegistrationStep.REGISTRATION_COMPLETE) {
            delay(2000) // Show success animation before navigating
            onRegistrationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Lavender.copy(alpha = 0.2f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            AnimatedVisibility(
                visible = screenVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(600, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                HeaderSection()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress Indicator
            AnimatedVisibility(
                visible = screenVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(800, delayMillis = 200, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
            ) {
                ProgressIndicator(currentStep = uiState.currentStep)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main Content with slide transition
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(500, easing = EaseOutCubic)
                    ) + fadeIn() with
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(500, easing = EaseInCubic)
                    ) + fadeOut()
                },
                label = "registration_steps"
            ) { step ->
                when (step) {
            RegistrationStep.INITIAL -> {
                InitialRegistrationForm(
                    uiState = uiState,
                    onNameChange = viewModel::updateName,
                    onDebitCardChange = viewModel::updateDebitCardNumber,
                            onVerifyCard = { name, cardNumber ->
                                viewModel.verifyDebitCardAndGenerateOTP(name, cardNumber)
                            },
                            onTouchDataCollected = onTouchDataCollected
                )
            }

            RegistrationStep.OTP_VERIFICATION -> {
                OTPVerificationForm(
                    uiState = uiState,
                    onVerifyOTP = viewModel::verifyOTPAndCompleteRegistration,
                            onBackToInitial = { viewModel.resetToInitialState() },
                            onTouchDataCollected = onTouchDataCollected
                )
            }

            RegistrationStep.REGISTRATION_COMPLETE -> {
                RegistrationCompleteCard(uiState = uiState)
                    }
            }
        }

            // Status Messages
            if (!uiState.message.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                        containerColor = if (uiState.error != null) Color(0xFFFFEBEE) else Color(0xFFE8F5E8)
                )
            ) {
                    Text(
                        text = uiState.message ?: "",
                    modifier = Modifier.padding(16.dp),
                        color = if (uiState.error != null) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }
            }
            
            // Error Messages
            if (!uiState.error.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo with Gradient Ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LapisLazuli.copy(alpha = 0.1f),
                            LightSeaGreen.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(LightSeaGreen, LapisLazuli)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img2),
                contentDescription = "ABHED Banking Logo",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
                    Text(
            text = "Device Registration",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Charcoal
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Secure your account with device verification",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = GlaucousMoonstone,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressIndicator(currentStep: RegistrationStep) {
    val steps = listOf(
        "Details" to Icons.Default.Person,
        "Verify" to Icons.Default.Message,
        "Complete" to Icons.Default.CheckCircle
    )
    
    val currentIndex = when (currentStep) {
        RegistrationStep.INITIAL -> 0
        RegistrationStep.OTP_VERIFICATION -> 1
        RegistrationStep.REGISTRATION_COMPLETE -> 2
    }
    
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
            ) {
                Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
            steps.forEachIndexed { index, (title, icon) ->
                StepIndicator(
                    title = title,
                    icon = icon,
                    isActive = index <= currentIndex,
                    isCompleted = index < currentIndex,
                    modifier = Modifier.weight(1f)
                )
                
                if (index < steps.size - 1) {
                    StepConnector(isCompleted = index < currentIndex)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "step_scale"
    )
    
    Column(
        modifier = modifier.scale(animatedScale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = when {
                        isCompleted -> LightSeaGreen
                        isActive -> LapisLazuli
                        else -> Periwinkle.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isActive || isCompleted) Color.White else GlaucousMoonstone,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Charcoal else GlaucousMoonstone
            )
        )
    }
}

@Composable
private fun StepConnector(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .height(2.dp)
            .width(40.dp)
            .background(
                color = if (isCompleted) LightSeaGreen else Periwinkle.copy(alpha = 0.3f),
                shape = RoundedCornerShape(1.dp)
            )
    )
}

@Composable
private fun InitialRegistrationForm(
    uiState: RegistrationUiState,
    onNameChange: (String) -> Unit,
    onDebitCardChange: (String) -> Unit,
    onVerifyCard: (String, String) -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var name by remember { mutableStateOf(uiState.name) }
    var debitCardNumber by remember { mutableStateOf(uiState.debitCardNumber) }
    var isFormVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.name) { name = uiState.name }
    LaunchedEffect(uiState.debitCardNumber) { debitCardNumber = uiState.debitCardNumber }
    
    LaunchedEffect(Unit) {
        delay(300)
        isFormVisible = true
    }

    AnimatedVisibility(
        visible = isFormVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(600, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(600))
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                color = Periwinkle.copy(alpha = 0.2f)
            )
    ) {
        Column(
                modifier = Modifier.padding(28.dp)
            ) {
                // Section Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        LightSeaGreen.copy(alpha = 0.15f),
                                        LapisLazuli.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = LapisLazuli,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
            Text(
                        text = "Personal Information",
                style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Enhanced Name Input
                TouchCollectingEnhancedTextField(
                value = name,
                onValueChange = { newName ->
                    name = newName
                    onNameChange(newName)
                },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    placeholder = "Enter your full name",
                    keyboardType = KeyboardType.Text,
                    onTouchDataCollected = onTouchDataCollected
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Simple Debit Card Input
                TouchCollectingEnhancedTextField(
                value = debitCardNumber,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        if (digitsOnly.length <= 16) {
                            debitCardNumber = digitsOnly
                            onDebitCardChange(digitsOnly)
                        }
                    },
                    label = "Debit Card Number",
                    icon = Icons.Default.CreditCard,
                    placeholder = "Enter card number",
                    keyboardType = KeyboardType.Number,
                    onTouchDataCollected = onTouchDataCollected
                )

                Spacer(modifier = Modifier.height(28.dp))
                
                // Security Notice
                SecurityNotice()

                Spacer(modifier = Modifier.height(16.dp))



                Spacer(modifier = Modifier.height(28.dp))

            // Verify Button
                TouchCollectingButton(
                    onClick = { onVerifyCard(name, debitCardNumber.replace(" ", "")) },
                    onTouchDataCollected = onTouchDataCollected,
                    enabled = name.isNotBlank() && 
                             debitCardNumber.replace(" ", "").length >= 9 && 
                             !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LightSeaGreen)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                    )
                } else {
                        Text("Verify Card & Send OTP", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun OTPVerificationForm(
    uiState: RegistrationUiState,
    onVerifyOTP: (String) -> Unit,
    onBackToInitial: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(120L) }
    var isTimerExpired by remember { mutableStateOf(false) }
    var isFormVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        isFormVisible = true
    }
    
    LaunchedEffect(uiState.otpExpiryTime) {
        if (uiState.otpExpiryTime != null) {
            timeRemaining = ((uiState.otpExpiryTime!! - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining = ((uiState.otpExpiryTime!! - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                if (timeRemaining <= 0) {
                    isTimerExpired = true
                    break
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isFormVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(600, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(600))
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                color = Periwinkle.copy(alpha = 0.2f)
            )
    ) {
        Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SavoyBlue.copy(alpha = 0.15f),
                                        LapisLazuli.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = SavoyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
            Text(
                text = "OTP Verification",
                style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Sent Message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Lavender.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
            Text(
                            text = "OTP sent to ****${uiState.phoneNumber.takeLast(3)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Charcoal
                            ),
                            textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

                        // Timer Display
                        TimerDisplay(
                            timeRemaining = timeRemaining,
                            isExpired = isTimerExpired
                        )
                    }
                }

            Spacer(modifier = Modifier.height(24.dp))

                // OTP Input Field
                TouchCollectingOTPInputField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                    isEnabled = !isTimerExpired,
                    onTouchDataCollected = onTouchDataCollected
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TouchCollectingButton(
                onClick = { onVerifyOTP(otpCode) },
                        onTouchDataCollected = onTouchDataCollected,
                        enabled = otpCode.length == 6 && !uiState.isLoading && !isTimerExpired,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LightSeaGreen)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                    )
                } else {
                            Text("Verify OTP", color = Color.White)
                        }
                    }
                    
            OutlinedButton(
                        onClick = {
                            // Collect back button data
                            onTouchDataCollected(
                                TouchDataPoint(
                                    x = 0f,
                                    y = 0f,
                                    pressure = 1.0f,
                                    timestamp = System.currentTimeMillis(),
                                    type = TouchType.TAP,
                                    velocity = 0f,
                                    sessionId = "registration_back_button",
                                    userId = "registration_user",
                                    deviceUUID = "registration_device",
                                    additionalData = mapOf(
                                        "scenario" to "back_button"
                                    )
                                )
                            )
                            onBackToInitial()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LapisLazuli)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Details")
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationCompleteCard(uiState: RegistrationUiState) {
    var isVisible by remember { mutableStateOf(false) }
    var celebrationScale by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        delay(200)
        isVisible = true
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            celebrationScale = value
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn()
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(LightSeaGreen, LapisLazuli)
                )
            )
    ) {
        Column(
                modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Animation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(celebrationScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    LightSeaGreen.copy(alpha = 0.2f),
                                    LightSeaGreen.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayMedium
            )
                }

                Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Registration Complete!",
                style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Charcoal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome ${uiState.registeredUser?.name}!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = LapisLazuli
                    ),
                textAlign = TextAlign.Center
            )

                Spacer(modifier = Modifier.height(12.dp))

            Text(
                    text = "Your device is now registered and trusted for secure banking access.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = GlaucousMoonstone,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Success Features
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Registration Successful!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "You can now proceed to biometric setup",
                style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Charcoal
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = GlaucousMoonstone.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LapisLazuli,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LapisLazuli,
                unfocusedBorderColor = Periwinkle.copy(alpha = 0.5f),
                focusedLabelColor = LapisLazuli,
                unfocusedLabelColor = GlaucousMoonstone,
                cursorColor = LapisLazuli
            )
        )
    }
}

@Composable
private fun OTPInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean
) {
    Column {
        Text(
            text = "Enter OTP Code",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Charcoal
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "123456",
                    color = GlaucousMoonstone.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 4.sp
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SavoyBlue,
                    modifier = Modifier.size(20.dp)
                )
            },
            enabled = isEnabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SavoyBlue,
                unfocusedBorderColor = Periwinkle.copy(alpha = 0.5f),
                disabledBorderColor = GlaucousMoonstone.copy(alpha = 0.3f),
                focusedLabelColor = SavoyBlue,
                unfocusedLabelColor = GlaucousMoonstone,
                cursorColor = SavoyBlue
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun TimerDisplay(
    timeRemaining: Long,
    isExpired: Boolean
) {
    val timerColor = if (isExpired) {
        MaterialTheme.colorScheme.error
    } else if (timeRemaining <= 30) {
        Color(0xFFFF7043)
    } else {
        LapisLazuli
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = timerColor,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = if (isExpired) "OTP Expired" else "${timeRemaining / 60}:${String.format("%02d", timeRemaining % 60)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        )
    }
}

@Composable
private fun SecurityNotice() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Lavender.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = LapisLazuli,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "Your information is encrypted and secure",
                style = MaterialTheme.typography.bodySmall,
                color = LapisLazuli
            )
        }
    }
}


