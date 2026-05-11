package com.example.bankingapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlinx.coroutines.delay

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatar: String,
    val isFrequent: Boolean = false
)

data class QuickAmount(
    val amount: Int,
    val label: String
)

@Composable
fun SendMoneyScreen(
    onBack: () -> Unit,
    onTransactionComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(SendMoneyStep.RECIPIENT_SELECTION) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.1f),
                        Color.White
                    )
                )
            )
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "← Back",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = when (currentStep) {
                        SendMoneyStep.RECIPIENT_SELECTION -> "Send Money"
                        SendMoneyStep.AMOUNT_ENTRY -> "Enter Amount"
                        SendMoneyStep.CONFIRMATION -> "Confirm Transfer"
                        SendMoneyStep.SUCCESS -> "Transfer Complete"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        // Step Indicator
        StepIndicator(
            currentStep = currentStep,
            modifier = Modifier.padding(16.dp)
        )
        
        // Content based on current step
        when (currentStep) {
            SendMoneyStep.RECIPIENT_SELECTION -> {
                RecipientSelectionContent(
                    onContactSelected = { contact ->
                        selectedContact = contact
                        currentStep = SendMoneyStep.AMOUNT_ENTRY
                    }
                )
            }
            
            SendMoneyStep.AMOUNT_ENTRY -> {
                AmountEntryContent(
                    amount = amount,
                    note = note,
                    onAmountChange = { amount = it },
                    onNoteChange = { note = it },
                    onNext = {
                        if (amount.isNotEmpty() && amount.toDoubleOrNull() != null) {
                            currentStep = SendMoneyStep.CONFIRMATION
                        }
                    }
                )
            }
            
            SendMoneyStep.CONFIRMATION -> {
                ConfirmationContent(
                    contact = selectedContact!!,
                    amount = amount,
                    note = note,
                    isProcessing = isProcessing,
                    onConfirm = {
                        isProcessing = true
                    },
                    onEdit = {
                        currentStep = SendMoneyStep.AMOUNT_ENTRY
                    }
                )
                
                // Simulate processing
                LaunchedEffect(isProcessing) {
                    if (isProcessing) {
                        delay(3000) // Simulate processing time
                        currentStep = SendMoneyStep.SUCCESS
                        isProcessing = false
                    }
                }
            }
            
            SendMoneyStep.SUCCESS -> {
                SuccessContent(
                    contact = selectedContact!!,
                    amount = amount,
                    onDone = onTransactionComplete
                )
            }
        }
    }
}

enum class SendMoneyStep {
    RECIPIENT_SELECTION, AMOUNT_ENTRY, CONFIRMATION, SUCCESS
}

@Composable
private fun StepIndicator(
    currentStep: SendMoneyStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val steps = SendMoneyStep.values()
        steps.forEachIndexed { index, step ->
            val isActive = step.ordinal <= currentStep.ordinal
            val isCurrent = step == currentStep
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isActive) ABHEDColors.LightSeaGreen else ABHEDColors.Lavender,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else ABHEDColors.GlaucousMoonstone
                        )
                    )
                }
                
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(
                                color = if (isActive) ABHEDColors.LightSeaGreen else ABHEDColors.Lavender
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientSelectionContent(
    onContactSelected: (Contact) -> Unit
) {
    val contacts = remember { generateMockContacts() }
    val frequentContacts = contacts.filter { it.isFrequent }
    val allContacts = contacts.filter { !it.isFrequent }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Frequent Contacts
        if (frequentContacts.isNotEmpty()) {
            item {
                Text(
                    text = "Frequent",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    )
                )
            }
            
            items(frequentContacts) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onContactSelected(contact) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All Contacts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    )
                )
            }
        }
        
        // All Contacts
        items(allContacts) { contact ->
            ContactItem(
                contact = contact,
                onClick = { onContactSelected(contact) }
            )
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = ABHEDColors.Periwinkle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.avatar,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ABHEDColors.DeftBlue
                )
                
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
            
            if (contact.isFrequent) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⭐",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountEntryContent(
    amount: String,
    note: String,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val quickAmounts = listOf(
        QuickAmount(500, "₹500"),
        QuickAmount(1000, "₹1K"),
        QuickAmount(2000, "₹2K"),
        QuickAmount(5000, "₹5K")
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Balance Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.Periwinkle.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Available Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.DeftBlue
                )
                Text(
                    text = "₹25,430.50",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                )
            }
        }
        
        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Enter Amount") },
            placeholder = { Text("₹0.00") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ABHEDColors.LightSeaGreen,
                focusedLabelColor = ABHEDColors.LightSeaGreen
            )
        )
        
        // Quick Amount Buttons
        Text(
            text = "Quick Amount",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = ABHEDColors.DeftBlue
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            quickAmounts.forEach { quickAmount ->
                OutlinedButton(
                    onClick = { onAmountChange(quickAmount.amount.toString()) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ABHEDColors.LightSeaGreen
                    ),
                    border = BorderStroke(1.dp, ABHEDColors.LightSeaGreen)
                ) {
                    Text(quickAmount.label)
                }
            }
        }
        
        // Note Input
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Add Note (Optional)") },
            placeholder = { Text("What's this for?") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ABHEDColors.LightSeaGreen,
                focusedLabelColor = ABHEDColors.LightSeaGreen
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun ConfirmationContent(
    contact: Contact,
    amount: String,
    note: String,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Transaction Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = ABHEDColors.Periwinkle,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.avatar,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.DeftBlue
                    )
                )
                
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.GlaucousMoonstone
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "₹$amount",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LapisLazuli
                    )
                )
                
                if (note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$note\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ABHEDColors.GlaucousMoonstone
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                enabled = !isProcessing,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ABHEDColors.GlaucousMoonstone
                ),
                border = BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone)
            ) {
                Text("Edit")
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ABHEDColors.LightSeaGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text("Confirm & Send")
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    contact: Contact,
    amount: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Transfer Successful!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LightSeaGreen
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "₹$amount has been sent to ${contact.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.DeftBlue,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

private fun generateMockContacts(): List<Contact> {
    return listOf(
        Contact("1", "John Doe", "+91 98765 43210", "👨", isFrequent = true),
        Contact("2", "Sarah Wilson", "+91 87654 32109", "👩", isFrequent = true),
        Contact("3", "Mike Johnson", "+91 76543 21098", "👨‍💼", isFrequent = true),
        Contact("4", "Emily Davis", "+91 65432 10987", "👩‍💻"),
        Contact("5", "Alex Brown", "+91 54321 09876", "🧑"),
        Contact("6", "Lisa Garcia", "+91 43210 98765", "👩‍🎨"),
        Contact("7", "David Miller", "+91 32109 87654", "👨‍🔬"),
        Contact("8", "Anna Taylor", "+91 21098 76543", "👩‍🏫"),
        Contact("9", "Chris Wilson", "+91 10987 65432", "👨‍🎤"),
        Contact("10", "Maya Patel", "+91 09876 54321", "👩‍⚕️")
    )
}
