package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositScreen(
    onBackToDashboard: () -> Unit
) {
    var currentTab by remember { mutableStateOf(FDTab.EXISTING) }
    
    // Dummy FD data
    val existingFDs = remember {
        listOf(
            FDData(
                fdNumber = "FD001234",
                principalAmount = 100000.0,
                interestRate = 7.5,
                tenure = 12,
                maturityAmount = 107500.0,
                startDate = "2024-01-15",
                maturityDate = "2025-01-15",
                status = "Active"
            ),
            FDData(
                fdNumber = "FD001235",
                principalAmount = 250000.0,
                interestRate = 8.0,
                tenure = 24,
                maturityAmount = 290000.0,
                startDate = "2023-06-01",
                maturityDate = "2025-06-01",
                status = "Active"
            ),
            FDData(
                fdNumber = "FD001230",
                principalAmount = 50000.0,
                interestRate = 7.0,
                tenure = 6,
                maturityAmount = 51750.0,
                startDate = "2023-10-01",
                maturityDate = "2024-04-01",
                status = "Matured"
            )
        )
    }
    
    val interestRates = remember {
        listOf(
            InterestRate("6 Months", 6.5),
            InterestRate("1 Year", 7.5),
            InterestRate("2 Years", 8.0),
            InterestRate("3 Years", 8.5),
            InterestRate("5 Years", 9.0)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fixed Deposits", color = ABHEDColors.Charcoal) },
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
                    selected = currentTab == FDTab.EXISTING,
                    onClick = { currentTab = FDTab.EXISTING },
                    text = { Text("My FDs") },
                    icon = { Icon(Icons.Default.AccountBalance, null) }
                )
                Tab(
                    selected = currentTab == FDTab.RATES,
                    onClick = { currentTab = FDTab.RATES },
                    text = { Text("Rates") },
                    icon = { Icon(Icons.Default.Percent, null) }
                )
                Tab(
                    selected = currentTab == FDTab.CREATE,
                    onClick = { currentTab = FDTab.CREATE },
                    text = { Text("Create FD") },
                    icon = { Icon(Icons.Default.Add, null) }
                )
            }
            
            // Tab Content
            when (currentTab) {
                FDTab.EXISTING -> {
                    ExistingFDsTab(existingFDs = existingFDs)
                }
                FDTab.RATES -> {
                    InterestRatesTab(interestRates = interestRates)
                }
                FDTab.CREATE -> {
                    CreateFDTab(
                        interestRates = interestRates,
                        onFDCreated = { currentTab = FDTab.EXISTING }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExistingFDsTab(existingFDs: List<FDData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total FD Investment",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val totalInvestment = existingFDs.filter { it.status == "Active" }.sumOf { it.principalAmount }
                    Text(
                        text = "₹${String.format("%.2f", totalInvestment)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LightSeaGreen
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FDSummaryItem("Active FDs", existingFDs.count { it.status == "Active" }.toString())
                        FDSummaryItem("Matured FDs", existingFDs.count { it.status == "Matured" }.toString())
                    }
                }
            }
        }
        
        items(existingFDs) { fd ->
            FDCard(fd = fd)
        }
    }
}

@Composable
private fun InterestRatesTab(interestRates: List<InterestRate>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Current Interest Rates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.Charcoal
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Effective from January 2024",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.GlaucousMoonstone
                    )
                }
            }
        }
        
        items(interestRates) { rate ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rate.tenure,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = ABHEDColors.Charcoal
                        )
                        Text(
                            text = "Tenure",
                            style = MaterialTheme.typography.bodySmall,
                            color = ABHEDColors.GlaucousMoonstone
                        )
                    }
                    
                    Text(
                        text = "${rate.rate}% p.a.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.LightSeaGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateFDTab(
    interestRates: List<InterestRate>,
    onFDCreated: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedTenure by remember { mutableStateOf(interestRates.first()) }
    var showCalculation by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    if (showSuccess) {
        FDCreationSuccessScreen(
            amount = amount,
            tenure = selectedTenure.tenure,
            rate = selectedTenure.rate,
            onDone = {
                showSuccess = false
                onFDCreated()
            }
        )
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create New Fixed Deposit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ABHEDColors.Charcoal
                    )
                    
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { 
                            amount = it
                            showCalculation = amount.isNotEmpty() && amount.toDoubleOrNull() != null
                        },
                        label = { Text("Investment Amount (₹)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Tenure Selection
                    Text(
                        text = "Select Tenure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = ABHEDColors.Charcoal
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        interestRates.forEach { rate ->
                            TenureOption(
                                rate = rate,
                                isSelected = selectedTenure == rate,
                                onSelect = { 
                                    selectedTenure = rate
                                    showCalculation = amount.isNotEmpty() && amount.toDoubleOrNull() != null
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Returns Calculator
        if (showCalculation && amount.toDoubleOrNull() != null) {
            item {
                val principal = amount.toDouble()
                val maturityAmount = calculateMaturityAmount(principal, selectedTenure.rate, getTenureInYears(selectedTenure.tenure))
                val interestEarned = maturityAmount - principal
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Maturity Calculation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ABHEDColors.Charcoal
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CalculationRow("Principal Amount", "₹${String.format("%.2f", principal)}")
                        CalculationRow("Interest Rate", "${selectedTenure.rate}% p.a.")
                        CalculationRow("Tenure", selectedTenure.tenure)
                        CalculationRow("Interest Earned", "₹${String.format("%.2f", interestEarned)}")
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        CalculationRow(
                            "Maturity Amount", 
                            "₹${String.format("%.2f", maturityAmount)}",
                            isTotal = true
                        )
                    }
                }
            }
        }
        
        // Create FD Button
        if (amount.isNotEmpty() && amount.toDoubleOrNull() != null) {
            item {
                Button(
                    onClick = { showSuccess = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ABHEDColors.LightSeaGreen
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Fixed Deposit", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FDCard(fd: FDData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    text = fd.fdNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.Charcoal
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (fd.status == "Active") 
                            ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
                        else 
                            ABHEDColors.LapisLazuli.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = fd.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fd.status == "Active") ABHEDColors.LightSeaGreen else ABHEDColors.LapisLazuli,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FDDetailColumn("Principal", "₹${String.format("%.0f", fd.principalAmount)}")
                FDDetailColumn("Rate", "${fd.interestRate}%")
                FDDetailColumn("Tenure", "${fd.tenure} months")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FDDetailColumn("Start Date", fd.startDate)
                FDDetailColumn("Maturity Date", fd.maturityDate)
                FDDetailColumn("Maturity Amount", "₹${String.format("%.0f", fd.maturityAmount)}")
            }
        }
    }
}

@Composable
private fun FDSummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LightSeaGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.GlaucousMoonstone
        )
    }
}

@Composable
private fun FDDetailColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
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
private fun TenureOption(
    rate: InterestRate,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                ABHEDColors.LightSeaGreen.copy(alpha = 0.1f)
            else 
                Color.White
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, ABHEDColors.LightSeaGreen)
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rate.tenure,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ABHEDColors.Charcoal
            )
            
            Text(
                text = "${rate.rate}% p.a.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LightSeaGreen
            )
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = ABHEDColors.Charcoal
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) ABHEDColors.LightSeaGreen else ABHEDColors.Charcoal
        )
    }
}

@Composable
private fun FDCreationSuccessScreen(
    amount: String,
    tenure: String,
    rate: Double,
    onDone: () -> Unit
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
            text = "FD Created Successfully!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ABHEDColors.LightSeaGreen,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your Fixed Deposit of ₹$amount for $tenure at $rate% p.a. has been created.",
            style = MaterialTheme.typography.bodyLarge,
            color = ABHEDColors.LapisLazuli,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = ABHEDColors.LightSeaGreen
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View My FDs", color = Color.White)
        }
    }
}

private fun calculateMaturityAmount(principal: Double, rate: Double, years: Double): Double {
    return principal * (1 + (rate / 100) * years)
}

private fun getTenureInYears(tenure: String): Double {
    return when {
        tenure.contains("6 Months") -> 0.5
        tenure.contains("1 Year") -> 1.0
        tenure.contains("2 Years") -> 2.0
        tenure.contains("3 Years") -> 3.0
        tenure.contains("5 Years") -> 5.0
        else -> 1.0
    }
}

data class FDData(
    val fdNumber: String,
    val principalAmount: Double,
    val interestRate: Double,
    val tenure: Int,
    val maturityAmount: Double,
    val startDate: String,
    val maturityDate: String,
    val status: String
)

data class InterestRate(
    val tenure: String,
    val rate: Double
)

enum class FDTab {
    EXISTING, RATES, CREATE
}
