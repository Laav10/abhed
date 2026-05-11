package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.ui.theme.ABHEDColors
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRPaymentScreen(
    onBackToDashboard: () -> Unit
) {
    var currentTab by remember { mutableStateOf(QRTab.SCAN) }
    var scannedAmount by remember { mutableStateOf("") }
    var scannedUPI by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var showPaymentSuccess by remember { mutableStateOf(false) }
    
    // Dummy account balance for simulation
    val accountBalance = 45678.90
    
    if (showPaymentSuccess) {
        PaymentSuccessScreen(
            amount = scannedAmount,
            upiId = scannedUPI,
            onBackToDashboard = onBackToDashboard
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Payment", color = ABHEDColors.Charcoal) },
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
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = ABHEDColors.Lavender,
                contentColor = ABHEDColors.Charcoal
            ) {
                Tab(
                    selected = currentTab == QRTab.SCAN,
                    onClick = { currentTab = QRTab.SCAN },
                    text = { Text("Scan QR") },
                    icon = { Icon(Icons.Default.QrCodeScanner, null) }
                )
                Tab(
                    selected = currentTab == QRTab.RECEIVE,
                    onClick = { currentTab = QRTab.RECEIVE },
                    text = { Text("Receive") },
                    icon = { Icon(Icons.Default.QrCode, null) }
                )
            }
            
            // Tab Content
            when (currentTab) {
                QRTab.SCAN -> {
                    QRScanTab(
                        accountBalance = accountBalance,
                        onScanSuccess = { amount, upiId ->
                            scannedAmount = amount
                            scannedUPI = upiId
                            showPaymentSuccess = true
                        }
                    )
                }
                QRTab.RECEIVE -> {
                    QRReceiveTab(
                        onBackToDashboard = onBackToDashboard
                    )
                }
            }
        }
    }
}

@Composable
private fun QRScanTab(
    accountBalance: Double,
    onScanSuccess: (String, String) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
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
        
        // Camera Scanner Simulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isScanning) {
                    // Scanning Animation
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                color = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = ABHEDColors.LightSeaGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Scanning QR Code...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ABHEDColors.LightSeaGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { isScanning = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.Error
                        )
                    ) {
                        Text("Cancel Scan", color = Color.White)
                    }
                } else {
                    // Camera Preview Placeholder
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                color = ABHEDColors.Charcoal.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = ABHEDColors.LightSeaGreen
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera Preview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ABHEDColors.GlaucousMoonstone
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { isScanning = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ABHEDColors.LightSeaGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Scanning", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Position the QR code within the frame to scan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ABHEDColors.LapisLazuli,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Demo QR Data (for testing)
        if (!isScanning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.Lavender.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Demo QR Codes (for testing)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.Charcoal
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DemoQRButton(
                        label = "Pay ₹100 to john@upi",
                        onClick = { onScanSuccess("100", "john@upi") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DemoQRButton(
                        label = "Pay ₹500 to shop@paytm",
                        onClick = { onScanSuccess("500", "shop@paytm") }
                    )
                }
            }
        }
    }
}

@Composable
private fun QRReceiveTab(
    onBackToDashboard: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showQRCode by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Generate QR Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Generate QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Note, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Button(
                    onClick = { showQRCode = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ABHEDColors.LightSeaGreen
                    ),
                    enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null
                ) {
                    Icon(Icons.Default.QrCode, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate QR Code", color = Color.White)
                }
            }
        }
        
        // Generated QR Code
        if (showQRCode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.Charcoal
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // QR Code Placeholder
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                color = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = ABHEDColors.LightSeaGreen
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "QR Code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ABHEDColors.GlaucousMoonstone
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Amount: ₹$amount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LightSeaGreen
                    )
                    
                    if (note.isNotBlank()) {
                        Text(
                            text = "Note: $note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ABHEDColors.LapisLazuli
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Show this QR code to receive payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.GlaucousMoonstone,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoQRButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ABHEDColors.LapisLazuli.copy(alpha = 0.1f)
        )
    ) {
        Text(label, color = ABHEDColors.LapisLazuli)
    }
}

@Composable
private fun PaymentSuccessScreen(
    amount: String,
    upiId: String,
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
            text = "Payment Successful!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LightSeaGreen,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "₹$amount paid to $upiId",
            style = MaterialTheme.typography.titleLarge,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You will receive a confirmation SMS shortly.",
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

enum class QRTab {
    SCAN, RECEIVE
}
