package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebitCardScreen(
    onBackToDashboard: () -> Unit
) {
    var showCardDetails by remember { mutableStateOf(false) }
    var isCardBlocked by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(CardTab.DETAILS) }
    
    // Dummy card data
    val cardData = remember {
        CardData(
            cardNumber = "1234 5678 9012 3456",
            cardHolderName = "LAVANYA RAJAN",
            expiryDate = "12/28",
            cvv = "123",
            cardType = "VISA",
            dailyLimit = 50000.0,
            monthlyLimit = 200000.0
        )
    }
    
    val transactions = remember {
        listOf(
            CardTransaction("Amazon Purchase", "Amazon.in", -1299.0, "2024-01-15", "Online"),
            CardTransaction("ATM Withdrawal", "HDFC ATM", -2000.0, "2024-01-14", "ATM"),
            CardTransaction("Fuel Payment", "HP Petrol Pump", -1500.0, "2024-01-13", "POS"),
            CardTransaction("Grocery Store", "Reliance Fresh", -850.0, "2024-01-12", "POS"),
            CardTransaction("Restaurant", "Pizza Hut", -1200.0, "2024-01-11", "POS")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debit Card", color = ABHEDColors.Charcoal) },
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
                    selected = currentTab == CardTab.DETAILS,
                    onClick = { currentTab = CardTab.DETAILS },
                    text = { Text("Details") },
                    icon = { Icon(Icons.Default.CreditCard, null) }
                )
                Tab(
                    selected = currentTab == CardTab.TRANSACTIONS,
                    onClick = { currentTab = CardTab.TRANSACTIONS },
                    text = { Text("Transactions") },
                    icon = { Icon(Icons.Default.History, null) }
                )
                Tab(
                    selected = currentTab == CardTab.SETTINGS,
                    onClick = { currentTab = CardTab.SETTINGS },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
            }
            
            // Tab Content
            when (currentTab) {
                CardTab.DETAILS -> {
                    CardDetailsTab(
                        cardData = cardData,
                        showCardDetails = showCardDetails,
                        onToggleCardDetails = { showCardDetails = !showCardDetails },
                        isCardBlocked = isCardBlocked,
                        onToggleCardStatus = { isCardBlocked = !isCardBlocked }
                    )
                }
                CardTab.TRANSACTIONS -> {
                    CardTransactionsTab(transactions = transactions)
                }
                CardTab.SETTINGS -> {
                    CardSettingsTab(
                        cardData = cardData,
                        isCardBlocked = isCardBlocked,
                        onToggleCardStatus = { isCardBlocked = !isCardBlocked }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardDetailsTab(
    cardData: CardData,
    showCardDetails: Boolean,
    onToggleCardDetails: () -> Unit,
    isCardBlocked: Boolean,
    onToggleCardStatus: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Physical Card Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.LapisLazuli
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ABHED Bank",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = if (showCardDetails) cardData.cardNumber else "•••• •••• •••• ••••",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Card Holder",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = cardData.cardHolderName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Expires",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = cardData.expiryDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showCardDetails) {
                            Text(
                                text = "CVV: ${cardData.cvv}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "CVV: •••",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = cardData.cardType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Card Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (showCardDetails) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = ABHEDColors.LightSeaGreen
                            )
                            Text(
                                text = if (showCardDetails) "Hide Card Details" else "Show Card Details",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Switch(
                            checked = showCardDetails,
                            onCheckedChange = { onToggleCardDetails() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ABHEDColors.LightSeaGreen,
                                checkedTrackColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isCardBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isCardBlocked) ABHEDColors.Error else ABHEDColors.LightSeaGreen
                            )
                            Text(
                                text = if (isCardBlocked) "Card Blocked" else "Card Active",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Switch(
                            checked = isCardBlocked,
                            onCheckedChange = { onToggleCardStatus() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ABHEDColors.Error,
                                checkedTrackColor = ABHEDColors.Error.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
        
        // Card Limits
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Transaction Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.Charcoal
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LimitItem(
                        label = "Daily Limit",
                        amount = "₹${String.format("%.0f", cardData.dailyLimit)}",
                        icon = Icons.Default.Today
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LimitItem(
                        label = "Monthly Limit",
                        amount = "₹${String.format("%.0f", cardData.monthlyLimit)}",
                        icon = Icons.Default.CalendarMonth
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTransactionsTab(transactions: List<CardTransaction>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.Charcoal
            )
        }
        
        items(transactions) { transaction ->
            TransactionCard(transaction = transaction)
        }
    }
}

@Composable
private fun CardSettingsTab(
    cardData: CardData,
    isCardBlocked: Boolean,
    onToggleCardStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Card Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isCardBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isCardBlocked) ABHEDColors.Error else ABHEDColors.LightSeaGreen
                        )
                        Text(
                            text = if (isCardBlocked) "Card is currently blocked" else "Card is active and ready to use",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onToggleCardStatus,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCardBlocked) ABHEDColors.LightSeaGreen else ABHEDColors.Error
                    )
                ) {
                    Text(
                        if (isCardBlocked) "Unblock Card" else "Block Card",
                        color = Color.White
                    )
                }
            }
        }
        
        // Security Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Security Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SecuritySettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Transaction Alerts",
                    subtitle = "Get notified for all transactions",
                    isEnabled = true
                )
                
                SecuritySettingItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location Services",
                    subtitle = "Track card usage by location",
                    isEnabled = true
                )
                
                SecuritySettingItem(
                    icon = Icons.Default.Security,
                    title = "Fraud Protection",
                    subtitle = "Advanced fraud detection enabled",
                    isEnabled = true
                )
            }
        }
        
        // Emergency Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ABHEDColors.Error.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Emergency Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EmergencyActionButton(
                    icon = Icons.Default.Report,
                    title = "Report Lost/Stolen",
                    subtitle = "Immediately block your card"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                EmergencyActionButton(
                    icon = Icons.Default.Support,
                    title = "Contact Support",
                    subtitle = "24/7 customer service"
                )
            }
        }
    }
}

@Composable
private fun LimitItem(
    label: String,
    amount: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ABHEDColors.LightSeaGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ABHEDColors.GlaucousMoonstone
            )
        }
        
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.Charcoal
        )
    }
}

@Composable
private fun TransactionCard(transaction: CardTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        containerColor = when (transaction.type) {
                            "Online" -> ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                            "ATM" -> ABHEDColors.LapisLazuli.copy(alpha = 0.1f)
                            else -> ABHEDColors.Periwinkle.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = when (transaction.type) {
                                "Online" -> Icons.Default.Computer
                                "ATM" -> Icons.Default.Atm
                                else -> Icons.Default.PointOfSale
                            },
                            contentDescription = transaction.type,
                            tint = when (transaction.type) {
                                "Online" -> ABHEDColors.LightSeaGreen
                                "ATM" -> ABHEDColors.LapisLazuli
                                else -> ABHEDColors.Periwinkle
                            },
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
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodySmall,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Error
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
        }
    }
}

@Composable
private fun SecuritySettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ABHEDColors.LightSeaGreen,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ABHEDColors.Charcoal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.GlaucousMoonstone
                )
            }
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = { },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ABHEDColors.LightSeaGreen,
                checkedTrackColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun EmergencyActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ABHEDColors.Error
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

data class CardData(
    val cardNumber: String,
    val cardHolderName: String,
    val expiryDate: String,
    val cvv: String,
    val cardType: String,
    val dailyLimit: Double,
    val monthlyLimit: Double
)

data class CardTransaction(
    val description: String,
    val merchant: String,
    val amount: Double,
    val date: String,
    val type: String // "Online", "ATM", "POS"
)

enum class CardTab {
    DETAILS, TRANSACTIONS, SETTINGS
}
