package com.example.bankingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.bankingapp.security.CredentialsManager

@Composable
fun TwilioCredentialsScreen(
    onCredentialsSaved: () -> Unit
) {
    val context = LocalContext.current
    val credentialsManager = remember { CredentialsManager(context) }
    
    var accountSid by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf("") }
    var fromPhone by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // Load existing credentials if any
    LaunchedEffect(Unit) {
        credentialsManager.getAccountSid()?.let { accountSid = it }
        credentialsManager.getAuthToken()?.let { authToken = it }
        credentialsManager.getFromPhone()?.let { fromPhone = it }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Twilio SMS Configuration",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter your Twilio credentials to enable real SMS OTP delivery",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Credentials Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Twilio Credentials",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Account SID
                OutlinedTextField(
                    value = accountSid,
                    onValueChange = { accountSid = it },
                    label = { Text("Account SID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("AC...") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Auth Token
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("Auth Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Enter your auth token") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // From Phone Number
                OutlinedTextField(
                    value = fromPhone,
                    onValueChange = { fromPhone = it },
                    label = { Text("From Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text("+1234567890") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save Button
                Button(
                    onClick = {
                        isSaving = true
                        // Save credentials securely
                        credentialsManager.saveCredentials(accountSid, authToken, fromPhone)
                        onCredentialsSaved()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = accountSid.isNotBlank() && authToken.isNotBlank() && fromPhone.isNotBlank() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Save Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Show current credentials status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Credentials Status:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val areValid = credentialsManager.areCredentialsValid()
                Text(
                    text = if (areValid) "✅ Credentials are configured" else "❌ No credentials found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (areValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                if (areValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Account SID: ${credentialsManager.getAccountSid()?.take(10)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "From Phone: ${credentialsManager.getFromPhone()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to Get Twilio Credentials:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "1. Sign up at twilio.com\n" +
                           "2. Get your Account SID from Dashboard\n" +
                           "3. Get your Auth Token from Dashboard\n" +
                           "4. Buy a phone number for SMS\n" +
                           "5. Enter all details above",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
