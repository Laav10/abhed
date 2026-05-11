package com.example.bankingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.data.models.TouchDataPoint
import com.example.bankingapp.data.models.TouchType
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.bankingapp.data.DeviceInfo
import com.example.bankingapp.data.DeviceStatus
import com.example.bankingapp.data.TouchDataStorage
import com.example.bankingapp.data.DeviceManager
import com.example.bankingapp.data.TouchStrokeManager
import com.example.bankingapp.data.MLPrediction
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.bankingapp.ui.viewmodels.RegistrationViewModel
import com.example.bankingapp.data.repository.AuthRepository
import com.example.bankingapp.data.room.BankingDatabase
import com.example.bankingapp.security.DeviceIdentifier
import com.example.bankingapp.services.TwilioSMSService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

// Onboarding states for real app flow
enum class OnboardingState {
    CONSENT,                 // Terms and biometric consent
    REGISTRATION_DETAILS,    // Name + Debit card, send OTP to DB phone
    OTP_VERIFICATION,        // Enter OTP received via SMS
    BIOMETRIC_SETUP,         // Choose and collect 2/3 biometrics
    TOUCH_TRAINING,          // Capture strokes and show live metrics
    DASHBOARD_TOUR,          // Guided dashboard exploration
    FUND_TRANSFER,           // Fund transfer tutorial
    QR_SCANNER,              // QR scanner tutorial
    ACCOUNT_HISTORY,         // Transaction history tutorial
    DEVICE_ACTIVATION,       // Device activation and backend sync
    COMPLETE                 // Onboarding finished
}

// Guided tour step for real app features
data class TourStep(
    val id: String,
    val title: String,
    val description: String,
    val targetElement: String, // Element to highlight
    val instructions: String,
    val isCompleted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedBankingOnboardingScreen(
    onComplete: () -> Unit,
    onTouchDataCollected: (TouchDataPoint) -> Unit = {}
) {
    val context = LocalContext.current
    val deviceManager = remember { DeviceManager(context) }
    val touchDataStorage = remember { TouchDataStorage(context) }
    val touchStrokeManager = remember { TouchStrokeManager(context) }
    
    var currentState by remember { mutableStateOf(OnboardingState.CONSENT) }
    var hasConsent by remember { mutableStateOf(false) }
    var currentTourStep by remember { mutableIntStateOf(0) }
    var showTutorialOverlay by remember { mutableStateOf(true) }
    var deviceInfo by remember { mutableStateOf(deviceManager.getDeviceInfo()) }
    var registeredUserId by remember { mutableStateOf<String?>(null) }
    
    // Track all user interactions for ML model
    var totalInteractions by remember { mutableIntStateOf(0) }
    
    // Initialize device and start session
    LaunchedEffect(Unit) {
        deviceInfo = deviceManager.initializeDevice()
        touchDataStorage.startNewSession()
        // Ensure sample debit cards exist for OTP flow
        try {
            val database = BankingDatabase.getDatabase(context)
            val cards = withContext(Dispatchers.IO) { database.debitCardDao().getAllDebitCards().first() }
            if (cards.isEmpty()) {
                com.example.bankingapp.data.room.DatabaseInitializer(database).initializeDatabase()
            }
        } catch (_: Exception) { }
        
        Log.d("Onboarding", "Device initialized: ${deviceInfo.deviceId}")
        Log.d("Onboarding", "Device status: ${deviceInfo.status}")
    }
    
    // Dashboard tour steps for real app features
    val dashboardTourSteps = remember {
        listOf(
            TourStep(
                id = "balance_visibility",
                title = "View Your Balance",
                description = "Tap the eye icon to show/hide your account balance",
                targetElement = "balance_toggle",
                instructions = "Tap the 👁️ icon to toggle balance visibility"
            ),
            TourStep(
                id = "quick_actions",
                title = "Quick Actions",
                description = "Access common banking functions quickly",
                targetElement = "quick_actions_grid",
                instructions = "Try tapping on Transfer, Bills, or QR Pay"
            ),
            TourStep(
                id = "account_cards",
                title = "Account Overview",
                description = "View details of your different accounts",
                targetElement = "account_cards",
                instructions = "Tap on any account card to see details"
            )
        )
    }
    
    // Record every touch interaction for ML model
    fun recordTouchData(
        type: TouchType,
        x: Float = 0f,
        y: Float = 0f,
        pressure: Float = 1.0f,
        velocity: Float = 0f,
        action: String = "",
        element: String = ""
    ) {
        val touchData = TouchDataPoint(
            x = x, y = y, pressure = pressure,
            timestamp = System.currentTimeMillis(),
            type = type,
            velocity = velocity,
            sessionId = "onboarding_session_${System.currentTimeMillis()}",
            userId = deviceInfo.userId ?: "unknown_user",
            deviceUUID = deviceInfo.deviceId,
            additionalData = mapOf(
                "action" to action,
                "element" to element,
                "onboarding_state" to currentState.name,
                "tour_step" to currentTourStep.toString(),
                "interaction_number" to totalInteractions.toString(),
                "device_status" to deviceInfo.status.name
            )
        )
        
        // Store locally until device activation
        touchDataStorage.storeTouchData(touchData)
        
        // Also send to callback for immediate processing if needed
        onTouchDataCollected(touchData)
        totalInteractions++
        
        // Handle stroke-based collection for ML model
        when (type) {
            TouchType.TAP -> {
                touchStrokeManager.handleTap(
                    x = x, y = y, pressure = pressure,
                    sessionId = "onboarding_session_${System.currentTimeMillis()}",
                    userId = deviceInfo.userId ?: "unknown_user",
                    deviceUUID = deviceInfo.deviceId,
                    additionalData = mapOf(
                        "action" to action,
                        "element" to element,
                        "onboarding_state" to currentState.name,
                        "tour_step" to currentTourStep.toString(),
                        "interaction_number" to totalInteractions.toString(),
                        "device_status" to deviceInfo.status.name
                    )
                )
            }
            TouchType.SWIPE -> {
                // Handle swipe gestures
                Log.d("Onboarding", "🔄 SWIPE detected - will be processed as stroke")
                // Note: Swipe data will be collected by gesture detection
            }
            TouchType.SCROLL -> {
                // Handle scroll gestures
                Log.d("Onboarding", "📜 SCROLL detected - will be processed as stroke")
                // Note: Scroll data will be collected by gesture detection
            }
            TouchType.LONG_PRESS -> {
                // Handle as extended tap
                touchStrokeManager.handleTap(
                    x = x, y = y, pressure = pressure,
                    sessionId = "onboarding_session_${System.currentTimeMillis()}",
                    userId = deviceInfo.userId ?: "unknown_user",
                    deviceUUID = deviceInfo.deviceId,
                    additionalData = mapOf(
                        "action" to action,
                        "element" to element,
                        "onboarding_state" to currentState.name,
                        "tour_step" to currentTourStep.toString(),
                        "interaction_number" to totalInteractions.toString(),
                        "device_status" to deviceInfo.status.name,
                        "gesture_type" to "LONG_PRESS"
                    )
                )
            }
            else -> {
                // Handle other touch types
                Log.d("Onboarding", "📱 Other touch type detected: $type")
            }
        }
        
        // Log for debugging
        Log.d("Onboarding", "Touch recorded: $type on $element - Total: $totalInteractions")
        Log.d("Onboarding", "Device status: ${deviceInfo.status}")
    }
    
    // Handle state transitions (react to consent changes too)
    LaunchedEffect(currentState, hasConsent) {
        when (currentState) {
            OnboardingState.CONSENT -> {
                if (hasConsent) {
                    // Update device status to REGISTERING
                    deviceManager.updateDeviceStatus(DeviceStatus.REGISTERING)
                    deviceInfo = deviceManager.getDeviceInfo()
                    
                    delay(500)
                    currentState = OnboardingState.REGISTRATION_DETAILS
                }
            }
            OnboardingState.REGISTRATION_DETAILS -> { /* wait for UI */ }
            OnboardingState.OTP_VERIFICATION -> { /* handled in RegistrationScreen */ }
            OnboardingState.BIOMETRIC_SETUP -> { /* handled in BiometricCollectionScreen */ }
            OnboardingState.TOUCH_TRAINING -> { /* handled in StrokeCaptureOnboardingScreen */ }
            OnboardingState.DASHBOARD_TOUR -> {
                if (currentTourStep >= dashboardTourSteps.size) {
                    delay(500)
                    currentState = OnboardingState.FUND_TRANSFER
                    currentTourStep = 0
                }
            }
            OnboardingState.FUND_TRANSFER -> {
                delay(500)
                currentState = OnboardingState.QR_SCANNER
            }
            OnboardingState.QR_SCANNER -> {
                delay(500)
                currentState = OnboardingState.ACCOUNT_HISTORY
            }
            OnboardingState.ACCOUNT_HISTORY -> {
                delay(500)
                currentState = OnboardingState.DEVICE_ACTIVATION
            }
            OnboardingState.DEVICE_ACTIVATION -> {
                delay(1000)
                currentState = OnboardingState.COMPLETE
            }
            OnboardingState.COMPLETE -> {
                delay(1000)
                onComplete()
            }
        }
    }
    
    when (currentState) {
        OnboardingState.CONSENT -> {
            ConsentScreen(
                deviceInfo = deviceInfo,
                onConsentGiven = { 
                    hasConsent = true
                    recordTouchData(TouchType.TAP, action = "consent_given", element = "consent_button")
                }
            )
        }
        OnboardingState.REGISTRATION_DETAILS, OnboardingState.OTP_VERIFICATION -> {
            val database = remember { com.example.bankingapp.data.room.BankingDatabase.getDatabase(context) }
            val authRepository = remember {
                com.example.bankingapp.data.repository.AuthRepository(
                    userDao = database.userDao(),
                    deviceDao = database.deviceDao(),
                    debitCardDao = database.debitCardDao(),
                    otpDao = database.otpDao(),
                    deviceIdentifier = com.example.bankingapp.security.DeviceIdentifier(context),
                    smsService = com.example.bankingapp.services.TwilioSMSService()
                )
            }
            val registrationViewModel = remember { com.example.bankingapp.ui.viewmodels.RegistrationViewModel(authRepository) }

            RegistrationScreen(
                viewModel = registrationViewModel,
                onRegistrationComplete = {
                    val user = registrationViewModel.uiState.value.registeredUser
                    registeredUserId = user?.id?.toString() ?: user?.name ?: "unknown_user"
                    deviceManager.setUserId(registeredUserId ?: "unknown_user")
                    deviceInfo = deviceManager.getDeviceInfo()
                    currentState = OnboardingState.BIOMETRIC_SETUP
                },
                onTouchDataCollected = { td ->
                    recordTouchData(
                        type = td.type,
                        x = td.x,
                        y = td.y,
                        pressure = td.pressure,
                        velocity = td.velocity,
                        action = "registration_interaction",
                        element = (td.additionalData["field"] as? String) ?: "registration"
                    )
                }
            )
        }
        OnboardingState.BIOMETRIC_SETUP -> {
            BiometricCollectionScreen(
                onComplete = {
                    currentState = OnboardingState.TOUCH_TRAINING
                }
            )
        }
        OnboardingState.TOUCH_TRAINING -> {
            StrokeCaptureOnboardingScreen(
                userId = registeredUserId ?: (deviceInfo.userId ?: "unknown_user"),
                onComplete = {
                    // After training, proceed to app dashboard
                    currentState = OnboardingState.COMPLETE
                }
            )
        }
        OnboardingState.DASHBOARD_TOUR -> {
            DashboardTourScreen(
                deviceInfo = deviceInfo,
                tourSteps = dashboardTourSteps,
                currentStep = currentTourStep,
                onStepCompleted = { 
                    currentTourStep++
                    recordTouchData(TouchType.TAP, action = "tour_step_completed", element = "tour_step_$currentTourStep")
                },
                onTouchData = { touchData ->
                    recordTouchData(
                        type = touchData.type,
                        x = touchData.x,
                        y = touchData.y,
                        pressure = touchData.pressure,
                        velocity = touchData.velocity,
                        action = "dashboard_interaction",
                        element = (touchData.additionalData["element"] as? String) ?: "unknown"
                    )
                },
                onTutorialToggle = { showTutorialOverlay = !showTutorialOverlay }
            )
        }
        OnboardingState.FUND_TRANSFER -> {
            FundTransferTutorialScreen(
                onComplete = { 
                    recordTouchData(TouchType.TAP, action = "fund_transfer_completed", element = "fund_transfer_tutorial")
                },
                onTouchData = { touchData ->
                    recordTouchData(
                        type = touchData.type,
                        x = touchData.x,
                        y = touchData.y,
                        pressure = touchData.pressure,
                        velocity = touchData.velocity,
                        action = "fund_transfer_interaction",
                        element = (touchData.additionalData["element"] as? String) ?: "unknown"
                    )
                }
            )
        }
        OnboardingState.QR_SCANNER -> {
            QRScannerTutorialScreen(
                onComplete = { 
                    recordTouchData(TouchType.TAP, action = "qr_scanner_completed", element = "qr_scanner_tutorial")
                },
                onTouchData = { touchData ->
                    recordTouchData(
                        type = touchData.type,
                        x = touchData.x,
                        y = touchData.y,
                        pressure = touchData.pressure,
                        velocity = touchData.velocity,
                        action = "qr_scanner_interaction",
                        element = (touchData.additionalData["element"] as? String) ?: "unknown"
                    )
                }
            )
        }
        OnboardingState.ACCOUNT_HISTORY -> {
            AccountHistoryTutorialScreen(
                onComplete = { 
                    recordTouchData(TouchType.TAP, action = "account_history_completed", element = "account_history_tutorial")
                },
                onTouchData = { touchData ->
                    recordTouchData(
                        type = touchData.type,
                        x = touchData.x,
                        y = touchData.y,
                        pressure = touchData.pressure,
                        velocity = touchData.velocity,
                        action = "account_history_interaction",
                        element = (touchData.additionalData["element"] as? String) ?: "unknown"
                    )
                }
            )
        }
        OnboardingState.DEVICE_ACTIVATION -> {
            DeviceActivationScreen(
                deviceInfo = deviceInfo,
                touchDataStorage = touchDataStorage,
                deviceManager = deviceManager,
                onDeviceActivated = { userId ->
                    // Update device status to ACTIVE
                    deviceManager.updateDeviceStatus(DeviceStatus.ACTIVE)
                    deviceManager.setUserId(userId)
                    deviceInfo = deviceManager.getDeviceInfo()
                    
                    Log.d("Onboarding", "Device activated for user: $userId")
                    Log.d("Onboarding", "Touch data summary: ${touchDataStorage.getDataSummary()}")
                }
            )
        }
        OnboardingState.COMPLETE -> {
            OnboardingCompleteScreen(
                deviceInfo = deviceInfo,
                totalInteractions = totalInteractions,
                onFinish = onComplete
            )
        }
    }
}

@Composable
private fun DeviceActivationScreen(
    deviceInfo: DeviceInfo,
    touchDataStorage: TouchDataStorage,
    deviceManager: DeviceManager,
    onDeviceActivated: (String) -> Unit
) {
    var isActivating by remember { mutableStateOf(false) }
    var activationMessage by remember { mutableStateOf("") }
    var showUserInput by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Device Activation",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Device Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Device ID: ${deviceInfo.deviceId}")
                Text("Status: ${deviceInfo.status}")
                Text("Model: ${deviceInfo.deviceModel}")
                Text("Android: ${deviceInfo.androidVersion}")
                Text("Screen: ${deviceInfo.screenResolution}")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Touch Data Collected: ${touchDataStorage.getTotalInteractions()} interactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.LightSeaGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!showUserInput) {
            Text(
                "Your device has collected touch data during onboarding. To complete the process, we need to activate your device and sync this data with our backend.",
                style = MaterialTheme.typography.bodyLarge,
                color = ABHEDColors.Charcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showUserInput = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ABHEDColors.LightSeaGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Start Device Activation",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // User ID Input
            Text(
                "Please enter your user ID to complete device activation:",
                style = MaterialTheme.typography.bodyLarge,
                color = ABHEDColors.Charcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("User ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (userId.isNotBlank()) {
                        isActivating = true
                        activationMessage = "Activating device..."
                        
                        // Simulate backend sync
                        // In real app, this would call your API
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            delay(2000) // Simulate API call
                            
                            // Update device status
                            deviceManager.updateDeviceStatus(DeviceStatus.ACTIVE)
                            deviceManager.setUserId(userId)
                            
                            activationMessage = "Device activated successfully!"
                            
                            delay(1000)
                            onDeviceActivated(userId)
                        }
                    }
                },
                enabled = userId.isNotBlank() && !isActivating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ABHEDColors.LightSeaGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Activate Device",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (activationMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = activationMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (activationMessage.contains("successfully")) 
                        ABHEDColors.LightSeaGreen else ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Data Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Touch Data Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val stats = touchDataStorage.getSessionStats()
                Text("Total Interactions: ${stats.totalInteractions}")
                Text("Session Duration: ${stats.sessionDuration / 1000}s")
                Text("Touch Types: ${stats.touchTypeCounts.keys.joinToString(", ")}")
            }
        }
    }
}

@Composable
private fun ConsentScreen(
    deviceInfo: DeviceInfo,
    onConsentGiven: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ABHED Banking Onboarding",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Device Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Status: ${deviceInfo.status.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (deviceInfo.status == DeviceStatus.UNREGISTERED) 
                        ABHEDColors.LightSeaGreen else ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Device ID: ${deviceInfo.deviceId}")
                Text("Model: ${deviceInfo.deviceModel}")
                Text("Screen: ${deviceInfo.screenResolution}")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "This app collects touch data to improve user experience and train ML models. By continuing, you agree to our terms and consent to biometric authentication.",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onConsentGiven,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "I Agree and Consent",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeviceRegistrationScreen(
    deviceInfo: DeviceInfo,
    onRegistered: (String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Device Registration",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device ID: ${deviceInfo.deviceId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.Charcoal
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(15) },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSubmitting = true
                // Simulate quick validation/registration
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    delay(600)
                    onRegistered(userId.ifBlank { "user_${System.currentTimeMillis()}" })
                }
            },
            enabled = userId.isNotBlank() && phone.length >= 8 && !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = ABHEDColors.LightSeaGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Register Device", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DashboardTourScreen(
    deviceInfo: DeviceInfo,
    tourSteps: List<TourStep>,
    currentStep: Int,
    onStepCompleted: () -> Unit,
    onTouchData: (TouchDataPoint) -> Unit,
    onTutorialToggle: () -> Unit
) {
    var showBalance by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    
    // Record touch data for ML model
    fun recordTouchData(
        type: TouchType,
        x: Float = 0f,
        y: Float = 0f,
        pressure: Float = 1.0f,
        velocity: Float = 0f,
        action: String = "",
        element: String = ""
    ) {
        val touchData = TouchDataPoint(
            x = x, y = y, pressure = pressure,
            timestamp = System.currentTimeMillis(),
            type = type,
            velocity = velocity,
            sessionId = "dashboard_tour_${System.currentTimeMillis()}",
            userId = deviceInfo.userId ?: "unknown_user",
            deviceUUID = deviceInfo.deviceId,
            additionalData = mapOf(
                "action" to action,
                "element" to element,
                "tour_step" to currentStep.toString(),
                "balance_visible" to showBalance.toString(),
                "selected_account" to (selectedAccount ?: "none")
            )
        )
        onTouchData(touchData)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with tutorial toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ABHED Banking Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )
            
            IconButton(
                onClick = onTutorialToggle,
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                recordTouchData(TouchType.TAP, offset.x, offset.y, action = "tutorial_toggle", element = "tutorial_button")
                            }
                        )
                    }
            ) {
                Icon(
                    Icons.Default.Help,
                    "Toggle Tutorial",
                    tint = ABHEDColors.LightSeaGreen
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Account Balances Section - REAL INTERACTIVE ELEMENT
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            recordTouchData(TouchType.TAP, offset.x, offset.y, action = "account_section_tap", element = "account_balances_section")
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account Balances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LapisLazuli
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Savings Account Card - REAL INTERACTIVE
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        selectedAccount = "savings"
                                        recordTouchData(TouchType.TAP, offset.x, offset.y, action = "savings_account_tap", element = "savings_account_card")
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAccount == "savings") 
                                ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                            else 
                                ABHEDColors.Periwinkle.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Savings Account", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showBalance) "₹45,678.90" else "••••••••",
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.Charcoal
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Balance Visibility Toggle - REAL INTERACTIVE ELEMENT
                                IconButton(
                                    onClick = { 
                                        showBalance = !showBalance
                                        recordTouchData(TouchType.TAP, action = "balance_visibility_toggle", element = "balance_toggle")
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    recordTouchData(TouchType.TAP, offset.x, offset.y, action = "balance_toggle_tap", element = "balance_toggle")
                                                }
                                            )
                                        }
                                ) {
                                    Icon(
                                        if (showBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        "Toggle balance visibility",
                                        modifier = Modifier.size(16.dp),
                                        tint = ABHEDColors.LightSeaGreen
                                    )
                                }
                            }
                        }
                    }
                    
                    // Current Account Card - REAL INTERACTIVE
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        selectedAccount = "current"
                                        recordTouchData(TouchType.TAP, offset.x, offset.y, action = "current_account_tap", element = "current_account_card")
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAccount == "current") 
                                ABHEDColors.LightSeaGreen.copy(alpha = 0.2f) 
                            else 
                                ABHEDColors.Periwinkle.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Current Account", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (showBalance) "₹1,23,456.78" else "••••••••",
                                fontWeight = FontWeight.Bold,
                                color = ABHEDColors.Charcoal
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions Section - REAL INTERACTIVE GRID
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(
                        Triple(Icons.Default.Send, "Transfer", "fund_transfer"),
                        Triple(Icons.Default.Receipt, "Bills", "bills_payment"),
                        Triple(Icons.Default.QrCodeScanner, "QR Pay", "qr_payment")
                    )) { (icon, title, action) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            recordTouchData(TouchType.TAP, offset.x, offset.y, action = "quick_action_tap", element = action)
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = ABHEDColors.Lavender.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, null, modifier = Modifier.size(24.dp), tint = ABHEDColors.LightSeaGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(title, style = MaterialTheme.typography.bodySmall, color = ABHEDColors.Charcoal)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recent Transactions Section - REAL SCROLLABLE LIST
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .height(200.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    recordTouchData(TouchType.SWIPE, offset.x, offset.y, action = "transaction_list_scroll_start", element = "transaction_list")
                                },
                                onDragEnd = { 
                                    recordTouchData(TouchType.SWIPE, 0f, 0f, action = "transaction_list_scroll_end", element = "transaction_list")
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    // Record scroll during drag for ML model
                                    val velocity = change.position.y - change.previousPosition.y
                                    recordTouchData(TouchType.SCROLL, change.position.x, change.position.y, velocity = velocity, action = "transaction_list_scroll", element = "transaction_list")
                                }
                            )
                        }
                ) {
                    items(10) { index ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            recordTouchData(TouchType.TAP, offset.x, offset.y, action = "transaction_tap", element = "transaction_item_$index")
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(containerColor = ABHEDColors.Lavender.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Transaction ${index + 1}", fontWeight = FontWeight.Medium, color = ABHEDColors.Charcoal)
                                    Text("₹${(index + 1) * 1000}", color = ABHEDColors.LightSeaGreen)
                                }
                                Text("2024-01-${(index + 1).toString().padStart(2, '0')}", 
                                     style = MaterialTheme.typography.bodySmall, color = ABHEDColors.GlaucousMoonstone)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tour Progress Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tour Progress: Step ${currentStep + 1} of ${tourSteps.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.LightSeaGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = (currentStep + 1).toFloat() / tourSteps.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ABHEDColors.LightSeaGreen,
                    trackColor = ABHEDColors.Lavender
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = tourSteps.getOrNull(currentStep)?.instructions ?: "Complete the tour steps above",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onStepCompleted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ABHEDColors.LightSeaGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentStep < tourSteps.size - 1) "Next Step" else "Complete Tour",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Placeholder screens for other tutorials
@Composable
private fun FundTransferTutorialScreen(
    onComplete: () -> Unit,
    onTouchData: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Fund Transfer Tutorial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "This is a placeholder for a real fund transfer tutorial. In a real app, you'd learn how to send money safely.",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Button(onClick = onComplete) {
            Text("Complete Fund Transfer Tutorial")
        }
    }
}

@Composable
private fun QRScannerTutorialScreen(
    onComplete: () -> Unit,
    onTouchData: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("QR Payment Tutorial", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onComplete) {
            Text("Complete QR Tutorial")
        }
    }
}

@Composable
private fun AccountHistoryTutorialScreen(
    onComplete: () -> Unit,
    onTouchData: (TouchDataPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Account History Tutorial", 
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "This is a placeholder for a real account history tutorial. In a real app, you'd learn how to view your transaction history.",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Button(onClick = onComplete) {
            Text("Complete Account History Tutorial")
        }
    }
}

@Composable
private fun OnboardingCompleteScreen(
    deviceInfo: DeviceInfo,
    totalInteractions: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Onboarding Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Device Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Status: ${deviceInfo.status.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Device ID: ${deviceInfo.deviceId}")
                if (deviceInfo.userId != null) {
                    Text("User ID: ${deviceInfo.userId}")
                }
                Text("Total Interactions: $totalInteractions")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Your device has been successfully activated and is now ready for biometric authentication!",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Finish Onboarding",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MLPredictionsPanel(
    predictions: List<MLPrediction>,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.LapisLazuli.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 Real-time ML Predictions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (predictions.isEmpty()) {
                Text(
                    text = "No predictions yet. Start interacting with the app!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(predictions.takeLast(10)) { prediction ->
                        PredictionItem(prediction = prediction)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total Predictions: ${predictions.size}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PredictionItem(prediction: MLPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Confidence indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            prediction.confidence >= 80f -> ABHEDColors.LightSeaGreen
                            prediction.confidence >= 60f -> Color.Yellow
                            else -> Color.Red
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${prediction.confidence.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Prediction details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prediction.prediction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ABHEDColors.Charcoal
                )
                
                Text(
                    text = "Stroke: ${prediction.strokeId.takeLast(8)} | Model: ${prediction.modelVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "Processing: ${prediction.processingTime}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal.copy(alpha = 0.7f)
                )
            }
        }
    }
}

