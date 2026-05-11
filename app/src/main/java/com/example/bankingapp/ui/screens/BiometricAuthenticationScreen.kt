package com.example.bankingapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.util.Log
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlinx.coroutines.launch

@Composable
fun BiometricAuthenticationScreen(
    deviceId: String,
    onAuthenticationSuccess: () -> Unit,
    onAuthenticationFailed: () -> Unit
) {
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shouldUpdateLastLogin by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    
    // Get repositories
    val database = remember { com.example.bankingapp.data.room.BankingDatabase.getDatabase(context) }
    val deviceRegistrationRepository = remember { 
        com.example.bankingapp.data.repository.DeviceRegistrationRepository(database.deviceRegistrationDao()) 
    }
    
    // Get primary biometric type for this device
    var primaryBiometricType by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        primaryBiometricType = deviceRegistrationRepository.getPrimaryBiometricType(deviceId)
        Log.d("BiometricAuth", "Primary biometric type: $primaryBiometricType")
    }
    
    // Auto-trigger authentication when screen loads
    LaunchedEffect(primaryBiometricType) {
        if (primaryBiometricType != null && !isAuthenticating) {
            isAuthenticating = true
            triggerBiometricAuthentication(
                context = context,
                biometricType = primaryBiometricType!!,
                onSuccess = {
                    isAuthenticating = false
                    shouldUpdateLastLogin = true
                    onAuthenticationSuccess()
                },
                onError = { error ->
                    isAuthenticating = false
                    errorMessage = error
                    onAuthenticationFailed()
                }
            )
        }
    }
    
    // Handle last login update
    LaunchedEffect(shouldUpdateLastLogin) {
        if (shouldUpdateLastLogin) {
            try {
                deviceRegistrationRepository.updateLastLogin(deviceId)
                Log.d("BiometricAuth", "Last login updated successfully")
            } catch (e: Exception) {
                Log.e("BiometricAuth", "Failed to update last login: ${e.message}")
            } finally {
                shouldUpdateLastLogin = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ABHED App logo
        Card(
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            border = BorderStroke(2.dp, ABHEDColors.LapisLazuli)
        ) {
            Text(
                text = "🔐",
                fontSize = 64.sp,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "ABHED",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli,
                fontSize = 32.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ABHEDColors.DeftBlue
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Periwinkle
            ),
            border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (primaryBiometricType) {
                        "FINGERPRINT" -> "👆"
                        "FACE" -> "👤"
                        "VOICE" -> "🎤"
                        else -> "🔒"
                    },
                    style = MaterialTheme.typography.displaySmall
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when (primaryBiometricType) {
                        "FINGERPRINT" -> "Please authenticate with your fingerprint"
                        "FACE" -> "Please authenticate with face recognition"
                        "VOICE" -> "Please authenticate with your voice"
                        else -> "Please authenticate with biometric"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = ABHEDColors.DeftBlue
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isAuthenticating) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.Lavender
                ),
                border = BorderStroke(1.dp, ABHEDColors.LightSeaGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = ABHEDColors.LightSeaGreen,
                        strokeWidth = 3.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Authenticating...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = ABHEDColors.Charcoal
                        )
                    )
                }
            }
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(

                    containerColor = Color(0xFFFFEBEE)
                ),
                border = BorderStroke(1.dp, Color(0xFFE57373))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Authentication Failed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD32F2F)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8B0000),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Manual retry button
        Button(
            onClick = {
                isAuthenticating = true
                errorMessage = null
                triggerBiometricAuthentication(
                    context = context,
                    biometricType = primaryBiometricType ?: "FINGERPRINT",
                    onSuccess = {
                        isAuthenticating = false
                        shouldUpdateLastLogin = true
                        onAuthenticationSuccess()
                    },
                    onError = { error ->
                        isAuthenticating = false
                        errorMessage = error
                    }
                )
            },
            enabled = !isAuthenticating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen,
                contentColor = Color.White,
                disabledContainerColor = ABHEDColors.GlaucousMoonstone,
                disabledContentColor = Color.White
            )
        ) {
            Text(
                text = if (isAuthenticating) "Authenticating..." else "Authenticate",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Alternative authentication option
        OutlinedButton(
            onClick = {
                // Navigate to alternative auth (PIN/Password)
                onAuthenticationFailed()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAuthenticating,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ABHEDColors.SavoyBlue
            ),
            border = BorderStroke(1.dp, ABHEDColors.SavoyBlue)
        ) {
            Text(
                text = "Use PIN Instead",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Security info
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡️",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Your data is protected with bank-level security",
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.GlaucousMoonstone
            )
        }
    }
}

private fun triggerBiometricAuthentication(
    context: android.content.Context,
    biometricType: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val fragmentActivity = context as? FragmentActivity
    if (fragmentActivity == null) {
        onError("Invalid context")
        return
    }
    
    val biometricManager = BiometricManager.from(context)
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("Biometric authentication not available")
        return
    }
    
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("ABHED Authentication")
        .setSubtitle(when (biometricType) {
            "FINGERPRINT" -> "Use your fingerprint to access your account"
            "FACE" -> "Use face recognition to access your account"
            "VOICE" -> "Use voice recognition to access your account"
            else -> "Use biometric authentication to access your account"
        })
        .setNegativeButtonText("Cancel")
        .build()
    
    val biometricPrompt = BiometricPrompt(fragmentActivity, ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d("BiometricAuth", "Authentication successful")
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e("BiometricAuth", "Authentication error: $errorCode - $errString")
                onError("Authentication failed: $errString")
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.e("BiometricAuth", "Authentication failed")
                onError("Authentication failed. Please try again.")
            }
        })
    
    biometricPrompt.authenticate(promptInfo)
}