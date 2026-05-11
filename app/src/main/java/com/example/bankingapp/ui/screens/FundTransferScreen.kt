package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.ui.theme.ABHEDColors
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundTransferScreen(
    onBackToDashboard: () -> Unit,
    onTransferSuccess: () -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(TransferStep.DETAILS) }
    var verificationMethod by remember { mutableStateOf<VerificationMethod?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var transferSuccess by remember { mutableStateOf(false) }
    
    // Dummy account balance for simulation
    val accountBalance = 45678.90
    
    if (transferSuccess) {
        TransferSuccessScreen(onBackToDashboard = onBackToDashboard)
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fund Transfer", color = ABHEDColors.Charcoal) },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Default.ArrowBack, "Back to Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ABHEDColors.Lavender
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            when (currentStep) {
                TransferStep.DETAILS -> {
                    TransferDetailsStep(
                        recipientName = recipientName,
                        onRecipientNameChange = { recipientName = it },
                        accountNumber = accountNumber,
                        onAccountNumberChange = { accountNumber = it },
                        ifscCode = ifscCode,
                        onIfscCodeChange = { ifscCode = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        transferNote = transferNote,
                        onTransferNoteChange = { transferNote = it },
                        accountBalance = accountBalance,
                        onNext = {
                            if (isFormValid(recipientName, accountNumber, ifscCode, amount)) {
                                currentStep = TransferStep.VERIFICATION
                            }
                        }
                    )
                }
                
                TransferStep.VERIFICATION -> {
                    VerificationStep(
                        onFaceVerification = {
                            verificationMethod = VerificationMethod.FACE
                            // Simulate face verification
                            isVerifying = true
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(2000)
                                isVerifying = false
                                currentStep = TransferStep.CONFIRMATION
                            }
                        },
                        onVoiceVerification = {
                            verificationMethod = VerificationMethod.VOICE
                            isVerifying = true
                            // Simulate voice verification
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(2000)
                                isVerifying = false
                                currentStep = TransferStep.CONFIRMATION
                            }
                        },
                        onBack = { currentStep = TransferStep.DETAILS },
                        isVerifying = isVerifying
                    )
                }
                
                TransferStep.CONFIRMATION -> {
                    TransferConfirmationStep(
                        recipientName = recipientName,
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        amount = amount,
                        transferNote = transferNote,
                        onConfirm = {
                            // Simulate transfer processing
                            transferSuccess = true
                            onTransferSuccess()
                        },
                        onBack = { currentStep = TransferStep.VERIFICATION }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferDetailsStep(
    recipientName: String,
    onRecipientNameChange: (String) -> Unit,
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    ifscCode: String,
    onIfscCodeChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    transferNote: String,
    onTransferNoteChange: (String) -> Unit,
    accountBalance: Double,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Account Balance Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            )
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
                        text = "Available Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                    Text(
                        text = "₹${String.format("%.2f", accountBalance)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LightSeaGreen
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = ABHEDColors.LightSeaGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Transfer Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Transfer Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = onRecipientNameChange,
                    label = { Text("Recipient Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = onAccountNumberChange,
                    label = { Text("Account Number") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = onIfscCodeChange,
                    label = { Text("IFSC Code") },
                    leadingIcon = { Icon(Icons.Default.Code, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = transferNote,
                    onValueChange = onTransferNoteChange,
                    label = { Text("Transfer Note (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Note, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ABHEDColors.LightSeaGreen
                    ),
                    enabled = isFormValid(recipientName, accountNumber, ifscCode, amount)
                ) {
                    Text("Continue to Verification", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VerificationStep(
    onFaceVerification: () -> Unit,
    onVoiceVerification: () -> Unit,
    onBack: () -> Unit,
    isVerifying: Boolean
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Security Verification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Please verify your identity using one of the following methods:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ABHEDColors.LapisLazuli,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isVerifying) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = ABHEDColors.LightSeaGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Verifying...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ABHEDColors.LightSeaGreen
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VerificationOption(
                            icon = Icons.Default.Face,
                            title = "Face Scan",
                            description = "Quick and secure",
                            onClick = onFaceVerification
                        )
                        
                        VerificationOption(
                            icon = Icons.Default.Mic,
                            title = "Voice",
                            description = "Say the phrase",
                            onClick = onVoiceVerification
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Details")
                }
            }
        }
    }
}

@Composable
private fun VerificationOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.Lavender.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = ABHEDColors.LightSeaGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ABHEDColors.Charcoal,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.GlaucousMoonstone,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TransferConfirmationStep(
    recipientName: String,
    accountNumber: String,
    ifscCode: String,
    amount: String,
    transferNote: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = ABHEDColors.LightSeaGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Confirm Transfer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Transfer Summary
                TransferSummaryItem("Recipient", recipientName)
                TransferSummaryItem("Account Number", accountNumber)
                TransferSummaryItem("IFSC Code", ifscCode)
                TransferSummaryItem("Amount", "₹$amount")
                if (transferNote.isNotEmpty()) {
                    TransferSummaryItem("Note", transferNote)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.LightSeaGreen
                        )
                    ) {
                        Text("Confirm Transfer", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferSummaryItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ABHEDColors.GlaucousMoonstone
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ABHEDColors.Charcoal
        )
    }
}

@Composable
private fun TransferSuccessScreen(
    onBackToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = ABHEDColors.LightSeaGreen
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Transfer Successful!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LightSeaGreen,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your money has been transferred successfully. You will receive a confirmation SMS shortly.",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBackToDashboard,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Dashboard", color = Color.White)
        }
    }
}

private fun isFormValid(
    recipientName: String,
    accountNumber: String,
    ifscCode: String,
    amount: String
): Boolean {
    return recipientName.isNotBlank() &&
           accountNumber.isNotBlank() &&
           ifscCode.isNotBlank() &&
           amount.isNotBlank() &&
           amount.toDoubleOrNull() != null &&
           amount.toDouble() > 0
}

enum class TransferStep {
    DETAILS, VERIFICATION, CONFIRMATION
}

enum class VerificationMethod {
    FACE, VOICE
}
