package com.example.bankingapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import androidx.core.app.ActivityCompat
import java.io.File
import java.util.*

import com.example.bankingapp.ui.theme.ABHEDColors

@Composable
fun BiometricCollectionScreen(
    onComplete: () -> Unit,
    userId: Long = 0
) {
    var currentStep by remember { mutableStateOf(BiometricStep.SELECTION) }
    var selectedOptions by remember { mutableStateOf(setOf<BiometricType>()) }
    var currentBiometricIndex by remember { mutableStateOf(0) }
    var biometricData by remember { mutableStateOf(mutableMapOf<BiometricType, String>()) }
    
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    
    // Get repositories
    val database = remember { com.example.bankingapp.data.room.BankingDatabase.getDatabase(context) }
    val biometricRepository = remember { com.example.bankingapp.data.repository.BiometricRepository(database.biometricDataDao()) }
    
    LaunchedEffect(Unit) {
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            // Biometric is available
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (currentStep) {
            BiometricStep.SELECTION -> {
                BiometricSelectionStep(
                    selectedOptions = selectedOptions,
                    onOptionSelected = { option ->
                        if (selectedOptions.contains(option)) {
                            selectedOptions = selectedOptions - option
                        } else if (selectedOptions.size < 2) {
                            selectedOptions = selectedOptions + option
                        }
                    },
                    onContinue = {
                        if (selectedOptions.size == 2) {
                            currentStep = BiometricStep.COLLECTION
                        }
                    }
                )
            }
            BiometricStep.COLLECTION -> {
                val biometricType = selectedOptions.toList()[currentBiometricIndex]
                BiometricCollectionStep(
                    biometricType = biometricType,
                    onDataCollected = { data ->
                        biometricData[biometricType] = data
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            biometricRepository.saveBiometricData(
                                userId = userId,
                                biometricType = biometricType.name,
                                status = data
                            )
                            
                        }
                        if (currentBiometricIndex < selectedOptions.size - 1) {
                            currentBiometricIndex++
                        } else {
                            currentStep = BiometricStep.COMPLETE
                        }
                    },
                    onSkip = {
                        if (currentBiometricIndex < selectedOptions.size - 1) {
                            currentBiometricIndex++
                        } else {
                            currentStep = BiometricStep.COMPLETE
                        }
                    }
                )
            }
            BiometricStep.COMPLETE -> {
                BiometricCompleteStep(
                    collectedData = biometricData,
                    onFinish = onComplete
                )
            }
        }
    }
}

@Composable
private fun BiometricSelectionStep(
    selectedOptions: Set<BiometricType>,
    onOptionSelected: (BiometricType) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Biometric Authentication Setup",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Select 2 biometric authentication methods for enhanced security",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = ABHEDColors.Charcoal
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        BiometricOptionCard(
            type = BiometricType.FINGERPRINT,
            title = "Fingerprint",
            description = "Use your fingerprint for quick and secure access",
            icon = Icons.Default.Fingerprint,
            isSelected = selectedOptions.contains(BiometricType.FINGERPRINT),
            onSelect = { onOptionSelected(BiometricType.FINGERPRINT) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BiometricOptionCard(
            type = BiometricType.FACE,
            title = "Face Recognition",
            description = "Use facial recognition for hands-free access",
            icon = Icons.Default.Face,
            isSelected = selectedOptions.contains(BiometricType.FACE),
            onSelect = { onOptionSelected(BiometricType.FACE) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BiometricOptionCard(
            type = BiometricType.VOICE,
            title = "Voice Recognition",
            description = "Use your voice for secure authentication",
            icon = Icons.Default.Mic,
            isSelected = selectedOptions.contains(BiometricType.VOICE),
            onSelect = { onOptionSelected(BiometricType.VOICE) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "${selectedOptions.size}/2 selected",
            style = MaterialTheme.typography.bodyMedium,
            color = ABHEDColors.LightSeaGreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedOptions.size == 2,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen,
                contentColor = Color.White,
                disabledContainerColor = ABHEDColors.GlaucousMoonstone,
                disabledContentColor = Color.White
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun BiometricOptionCard(
    type: BiometricType,
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                ABHEDColors.Periwinkle
            else 
                ABHEDColors.Lavender
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, ABHEDColors.LapisLazuli)
        } else {
            BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = ABHEDColors.LightSeaGreen
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ABHEDColors.DeftBlue
                    )
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.Charcoal
                )
            }
            
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = ABHEDColors.LapisLazuli
                )
            }
        }
        
        if (!isSelected) {
            TextButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ABHEDColors.SavoyBlue
                )
            ) {
                Text("Select")
            }
        }
    }
}

@Composable
private fun BiometricCollectionStep(
    biometricType: BiometricType,
    onDataCollected: (String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var isCollecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Setup ${biometricType.displayName}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = biometricType.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = ABHEDColors.Charcoal
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (biometricType) {
            BiometricType.FINGERPRINT -> {
                FingerprintCollection(
                    onDataCollected = onDataCollected,
                    onSkip = onSkip
                )
            }
            BiometricType.FACE -> {
                FaceCollection(
                    onDataCollected = onDataCollected,
                    onSkip = onSkip
                )
            }
            BiometricType.VOICE -> {
                VoiceCollection(
                    onDataCollected = onDataCollected,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun FingerprintCollection(
    onDataCollected: (String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(true) }
    var hasFingerprint by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        
        hasFingerprint = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        isChecking = false
    }
    
    if (isChecking) {
        CircularProgressIndicator(color = ABHEDColors.LightSeaGreen)
        Text(
            "Checking fingerprint availability...",
            color = ABHEDColors.Charcoal
        )
    } else if (hasFingerprint) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Periwinkle
            ),
            border = BorderStroke(1.dp, ABHEDColors.LapisLazuli)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Fingerprint Already Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your device already has fingerprint authentication set up.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onDataCollected("fingerprint_available") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen,
                contentColor = Color.White
            )
        ) {
            Text("Continue")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ABHEDColors.MarianBlue
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Fingerprint Not Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.MarianBlue
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Please set up fingerprint authentication in your device settings first.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ABHEDColors.SavoyBlue
            ),
            border = BorderStroke(1.dp, ABHEDColors.SavoyBlue)
        ) {
            Text("Skip")
        }
    }
}

@Composable
private fun FaceCollection(
    onDataCollected: (String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(true) }
    var hasFace by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        
        hasFace = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        isChecking = false
    }
    
    if (isChecking) {
        CircularProgressIndicator(color = ABHEDColors.LightSeaGreen)
        Text(
            "Checking face recognition availability...",
            color = ABHEDColors.Charcoal
        )
    } else if (hasFace) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Periwinkle
            ),
            border = BorderStroke(1.dp, ABHEDColors.LapisLazuli)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Face Recognition Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your device supports face recognition authentication.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onDataCollected("face_available") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen,
                contentColor = Color.White
            )
        ) {
            Text("Continue")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ABHEDColors.MarianBlue
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Face Recognition Not Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.MarianBlue
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your device doesn't support face recognition or it's not set up.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ABHEDColors.SavoyBlue
            ),
            border = BorderStroke(1.dp, ABHEDColors.SavoyBlue)
        ) {
            Text("Skip")
        }
    }
}

@Composable
private fun VoiceCollection(
    onDataCollected: (String) -> Unit,
    onSkip: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var currentPhrase by remember { mutableStateOf("") }
    var recordingProgress by remember { mutableStateOf(0f) }
    var phrases by remember { mutableStateOf(generateVoicePhrases()) }
    var currentPhraseIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        currentPhrase = phrases[currentPhraseIndex]
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Voice Recognition Setup",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Please read the following phrase clearly:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ABHEDColors.Periwinkle
                    ),
                    border = BorderStroke(1.dp, ABHEDColors.LapisLazuli)
                ) {
                    Text(
                        text = currentPhrase,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ABHEDColors.DeftBlue
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isRecording) {
                    CircularProgressIndicator(color = ABHEDColors.LightSeaGreen)
                    Text(
                        "Recording... Please speak clearly",
                        color = ABHEDColors.Charcoal
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LinearProgressIndicator(
                        progress = recordingProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = ABHEDColors.LightSeaGreen,
                        trackColor = ABHEDColors.GlaucousMoonstone
                    )
                } else {
                    Button(
                        onClick = {
                            isRecording = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.LightSeaGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Start Recording")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Phrase ${currentPhraseIndex + 1} of ${phrases.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ABHEDColors.SavoyBlue
            ),
            border = BorderStroke(1.dp, ABHEDColors.SavoyBlue)
        ) {
            Text("Skip Voice Recognition")
        }
    }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            kotlinx.coroutines.delay(3000)
            isRecording = false
            recordingProgress = 1f
            
            if (currentPhraseIndex < phrases.size - 1) {
                currentPhraseIndex++
                currentPhrase = phrases[currentPhraseIndex]
                recordingProgress = 0f
            } else {
                onDataCollected("voice_completed")
            }
        }
    }
}

@Composable
private fun BiometricCompleteStep(
    collectedData: Map<BiometricType, String>,
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Celebration,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ABHEDColors.LightSeaGreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Biometric Setup Complete!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your biometric authentication methods have been configured successfully.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = ABHEDColors.Charcoal
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Lavender
            ),
            border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Configured Methods:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ABHEDColors.DeftBlue
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                collectedData.forEach { (type, status) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = ABHEDColors.LightSeaGreen
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ABHEDColors.Charcoal
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ABHEDColors.LightSeaGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Complete Setup",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

private fun generateVoicePhrases(): List<String> {
    return listOf(
        "The quick brown fox jumps over the lazy dog",
        "Hello, this is my voice for authentication",
        "Security is important for banking applications",
        "Please verify my identity using voice recognition"
    )
}

enum class BiometricStep {
    SELECTION,
    COLLECTION,
    COMPLETE
}

enum class BiometricType(val displayName: String, val description: String, val icon: ImageVector) {
    FINGERPRINT(
        "Fingerprint",
        "Place your finger on the sensor to register your fingerprint",
        Icons.Default.Fingerprint
    ),
    FACE(
        "Face Recognition", 
        "Position your face in the camera view to register facial features",
        Icons.Default.Face
    ),
    VOICE(
        "Voice Recognition",
        "Read the provided phrases to register your voice pattern",
        Icons.Default.Mic
    )
}