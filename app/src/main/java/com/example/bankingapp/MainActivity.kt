package com.example.bankingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bankingapp.data.DeviceManager
import com.example.bankingapp.data.DeviceStatus
import com.example.bankingapp.ui.screens.EnhancedBankingOnboardingScreen
import com.example.bankingapp.ui.screens.LandingPage
import com.example.bankingapp.ui.screens.MainBankingDashboard
import com.example.bankingapp.ui.theme.BankingAppTheme
import com.example.bankingapp.ui.theme.ABHEDColors
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainScreen()
                }
            }
        }
    }
}

@Composable
fun AppMainScreen() {
    val context = LocalContext.current
    val deviceManager = remember { DeviceManager(context) }
    var deviceStatus by remember { mutableStateOf(DeviceStatus.UNREGISTERED) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showLanding by remember { mutableStateOf(true) }
    var showMainApp by remember { mutableStateOf(false) }
    
    // Initialize device and check status (do not auto-navigate; start on Landing)
    LaunchedEffect(Unit) {
        val deviceInfo = deviceManager.initializeDevice()
        deviceStatus = deviceInfo.status
    }
    
    when {
        showLanding -> {
            LandingPage(
                onGetStarted = {
                    showLanding = false
                    if (deviceStatus == DeviceStatus.ACTIVE || deviceStatus == DeviceStatus.BIOMETRIC_READY) {
                        showMainApp = true
                    } else {
                        showOnboarding = true
                    }
                },
                onTouchModelTest = {
                    // Optional: direct to onboarding for testing touch model within flow
                    showLanding = false
                    showOnboarding = true
                }
            )
        }
        showOnboarding -> {
            EnhancedBankingOnboardingScreen(
                onComplete = {
                    showOnboarding = false
                    showMainApp = true
                    deviceStatus = deviceManager.getDeviceStatus()
                }
            )
        }
        showMainApp -> {
            MainBankingDashboard(
                onLogout = {
                    deviceManager.resetDevice()
                    deviceStatus = DeviceStatus.UNREGISTERED
                    showMainApp = false
                    showOnboarding = true
                },
                onUnregisterDevice = {
                    deviceManager.resetDevice()
                    deviceStatus = DeviceStatus.UNREGISTERED
                    showMainApp = false
                    showOnboarding = true
                },
                userId = deviceManager.getDeviceInfo().userId ?: "unknown_user",
                deviceUUID = deviceManager.getDeviceInfo().deviceId
            )
        }
        else -> {
            // Loading state
            LoadingScreen()
        }
    }
}

@Composable
fun MainAppScreen(
    deviceStatus: DeviceStatus,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ABHED Banking!",
            style = MaterialTheme.typography.headlineLarge,
            color = ABHEDColors.LapisLazuli
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Status: ${deviceStatus.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = when (deviceStatus) {
                        DeviceStatus.ACTIVE -> ABHEDColors.LightSeaGreen
                        DeviceStatus.BIOMETRIC_READY -> ABHEDColors.LightSeaGreen
                        else -> ABHEDColors.Charcoal
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (deviceStatus) {
                    DeviceStatus.ACTIVE -> {
                        Text("✅ Device is active and ready for use")
                        Text("🔐 Biometric authentication available")
                    }
                    DeviceStatus.BIOMETRIC_READY -> {
                        Text("✅ Device is fully registered")
                        Text("🔐 Biometric authentication ready")
                    }
                    else -> {
                        Text("❌ Device status unknown")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your device has been successfully activated and is now ready for secure banking!",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            )
        ) {
            Text("Logout (Reset Device)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Note: This will reset the device for demonstration purposes",
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.Charcoal.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ABHEDColors.LightSeaGreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Initializing ABHED Banking...",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.Charcoal
        )
    }
}
