package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAccountScreen(
    onBackToDashboard: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit
) {
    var showBalance by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("1M") }
    
    // Dummy data for prototyping
    val accountBalance = 45678.90
    val accountNumber = "1234567890"
    val ifscCode = "ABHE0001234"
    val transactions = remember {
        listOf(
            SavingsTransaction("Salary Credit", "ICICI Bank", 25000.0, "Credit", "2024-01-15"),
            SavingsTransaction("UPI Payment", "Amazon", -1299.0, "Debit", "2024-01-14"),
            SavingsTransaction("ATM Withdrawal", "HDFC ATM", -2000.0, "Debit", "2024-01-13"),
            SavingsTransaction("Fund Transfer", "John Doe", -5000.0, "Debit", "2024-01-12"),
            SavingsTransaction("Interest Credit", "ABHED Bank", 125.50, "Credit", "2024-01-10")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Account", color = ABHEDColors.Charcoal) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Available Balance",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ABHEDColors.GlaucousMoonstone
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (showBalance) "₹${String.format("%.2f", accountBalance)}" else "₹••••••••",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ABHEDColors.LapisLazuli
                            )
                            
                            IconButton(
                                onClick = { showBalance = !showBalance },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (showBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Toggle balance visibility",
                                    modifier = Modifier.size(20.dp),
                                    tint = ABHEDColors.LightSeaGreen
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Account Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Account Number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.GlaucousMoonstone
                                )
                                Text(
                                    text = accountNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column {
                                Text(
                                    text = "IFSC Code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.GlaucousMoonstone
                                )
                                Text(
                                    text = ifscCode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Actions
            item {
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Send,
                                label = "Transfer",
                                onClick = onNavigateToTransfer
                            )
                            QuickActionButton(
                                icon = Icons.Default.History,
                                label = "History",
                                onClick = onNavigateToTransactionHistory
                            )
                            QuickActionButton(
                                icon = Icons.Default.Download,
                                label = "Statement",
                                onClick = { /* TODO: Download statement */ }
                            )
                        }
                    }
                }
            }
            
            // Recent Transactions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ABHEDColors.Charcoal
                            )
                            
                            TextButton(onClick = onNavigateToTransactionHistory) {
                                Text("View All", color = ABHEDColors.LightSeaGreen)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        transactions.take(3).forEach { transaction ->
                            TransactionItem(transaction = transaction)
                            if (transaction != transactions.take(3).last()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ABHEDColors.LightSeaGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.Charcoal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionItem(transaction: SavingsTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (transaction.type == "Credit") 
                        ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                    else 
                        ABHEDColors.Error.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (transaction.type == "Credit") Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = transaction.type,
                        tint = if (transaction.type == "Credit") ABHEDColors.LightSeaGreen else ABHEDColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ABHEDColors.Charcoal
                )
                Text(
                    text = transaction.party,
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${if (transaction.type == "Credit") "+" else "-"}₹${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "Credit") ABHEDColors.LightSeaGreen else ABHEDColors.Error
            )
            Text(
                text = transaction.date,
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.GlaucousMoonstone
            )
        }
    }
}

data class SavingsTransaction(
    val description: String,
    val party: String,
    val amount: Double,
    val type: String, // "Credit" or "Debit"
    val date: String
)
