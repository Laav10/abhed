package com.example.bankingapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bankingapp.ui.theme.ABHEDColors
import java.text.SimpleDateFormat
import java.util.*

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val date: Date,
    val category: String,
    val icon: String,
    val isDebit: Boolean = true
)

enum class TransactionType {
    PAYMENT, TRANSFER, DEPOSIT, WITHDRAWAL, BILL_PAYMENT, REFUND
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedPeriod by remember { mutableStateOf("This Month") }
    
    val mockTransactions = remember {
        generateMockTransactions()
    }
    
    val filteredTransactions = remember(selectedFilter, selectedPeriod) {
        mockTransactions.filter { transaction ->
            when (selectedFilter) {
                "All" -> true
                "Debit" -> transaction.isDebit
                "Credit" -> !transaction.isDebit
                else -> transaction.category == selectedFilter
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ABHEDColors.Lavender.copy(alpha = 0.3f),
                        Color.White
                    )
                )
            )
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ABHEDColors.LapisLazuli
            ),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = "Transaction History",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Balance Summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total In",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "+₹12,450",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.LightSeaGreen
                                )
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Out",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "-₹8,930",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ABHEDColors.MarianBlue
                                )
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Net",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "+₹3,520",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Filters
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Debit", "Credit", "Food", "Shopping", "Bills", "Transport")
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ABHEDColors.LightSeaGreen,
                        selectedLabelColor = Color.White,
                        containerColor = ABHEDColors.Lavender,
                        labelColor = ABHEDColors.DeftBlue
                    )
                )
            }
        }
        
        // Transaction List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTransactions) { transaction ->
                TransactionItem(transaction = transaction)
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (transaction.isDebit)
                            ABHEDColors.MarianBlue.copy(alpha = 0.1f)
                        else
                            ABHEDColors.LightSeaGreen.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ABHEDColors.DeftBlue
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                    
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                    
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                }
            }
            
            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (transaction.isDebit) "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isDebit) 
                            ABHEDColors.MarianBlue 
                        else 
                            ABHEDColors.LightSeaGreen
                    )
                )
                
                // Transaction Type Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (transaction.type) {
                            TransactionType.PAYMENT -> ABHEDColors.SavoyBlue.copy(alpha = 0.2f)
                            TransactionType.TRANSFER -> ABHEDColors.LightSeaGreen.copy(alpha = 0.2f)
                            TransactionType.DEPOSIT -> ABHEDColors.Periwinkle.copy(alpha = 0.3f)
                            TransactionType.WITHDRAWAL -> ABHEDColors.MarianBlue.copy(alpha = 0.2f)
                            TransactionType.BILL_PAYMENT -> ABHEDColors.GlaucousMoonstone.copy(alpha = 0.2f)
                            TransactionType.REFUND -> ABHEDColors.LightSeaGreen.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = transaction.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ABHEDColors.DeftBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun generateMockTransactions(): List<Transaction> {
    val calendar = Calendar.getInstance()
    return listOf(
        Transaction(
            id = "1",
            type = TransactionType.PAYMENT,
            amount = 1250.00,
            description = "Grocery Store - BigBazaar",
            date = calendar.time,
            category = "Food",
            icon = "🛒",
            isDebit = true
        ),
        Transaction(
            id = "2",
            type = TransactionType.DEPOSIT,
            amount = 5000.00,
            description = "Salary Credit",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
            category = "Income",
            icon = "💰",
            isDebit = false
        ),
        Transaction(
            id = "3",
            type = TransactionType.BILL_PAYMENT,
            amount = 2500.00,
            description = "Electricity Bill",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.time,
            category = "Bills",
            icon = "⚡",
            isDebit = true
        ),
        Transaction(
            id = "4",
            type = TransactionType.TRANSFER,
            amount = 500.00,
            description = "Transfer to John Doe",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -3) }.time,
            category = "Transfer",
            icon = "👤",
            isDebit = true
        ),
        Transaction(
            id = "5",
            type = TransactionType.PAYMENT,
            amount = 180.00,
            description = "Starbucks Coffee",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -3) }.time,
            category = "Food",
            icon = "☕",
            isDebit = true
        ),
        Transaction(
            id = "6",
            type = TransactionType.REFUND,
            amount = 750.00,
            description = "Amazon Refund",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -4) }.time,
            category = "Refund",
            icon = "📦",
            isDebit = false
        ),
        Transaction(
            id = "7",
            type = TransactionType.PAYMENT,
            amount = 3200.00,
            description = "Uber Ride",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -5) }.time,
            category = "Transport",
            icon = "🚗",
            isDebit = true
        ),
        Transaction(
            id = "8",
            type = TransactionType.PAYMENT,
            amount = 1800.00,
            description = "Zara Shopping",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -6) }.time,
            category = "Shopping",
            icon = "👕",
            isDebit = true
        ),
        Transaction(
            id = "9",
            type = TransactionType.DEPOSIT,
            amount = 2500.00,
            description = "Freelance Payment",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -7) }.time,
            category = "Income",
            icon = "💻",
            isDebit = false
        ),
        Transaction(
            id = "10",
            type = TransactionType.BILL_PAYMENT,
            amount = 1200.00,
            description = "Internet Bill",
            date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -8) }.time,
            category = "Bills",
            icon = "🌐",
            isDebit = true
        )
    )
}
