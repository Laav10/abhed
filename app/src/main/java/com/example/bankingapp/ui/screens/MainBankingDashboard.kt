package com.example.bankingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bankingapp.services.BehavioralAuthenticationService
import com.example.bankingapp.ml.BayesianTrustModel
import kotlinx.coroutines.delay
import com.example.bankingapp.ui.theme.ABHEDColors

// Helper: define an orange constant because Compose doesn't provide Color.Orange
private val WarningOrange = Color(0xFFFFA500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBankingDashboard(
    onLogout: () -> Unit,
    onUnregisterDevice: () -> Unit,
    userId: String = "user123",
    deviceUUID: String = "device456"
) {
    var currentScreen by remember { mutableStateOf<DashboardScreen?>(null) }
    var showBalance by remember { mutableStateOf(false) }
    
    // If a specific screen is selected, show it instead of the dashboard
    when (currentScreen) {
        DashboardScreen.SAVINGS_ACCOUNT -> {
            SavingsAccountScreen(
                onBackToDashboard = { currentScreen = null },
                onNavigateToTransfer = { currentScreen = DashboardScreen.FUND_TRANSFER },
                onNavigateToTransactionHistory = { /* TODO: Transaction History */ }
            )
            return
        }
        DashboardScreen.FUND_TRANSFER -> {
            FundTransferScreen(
                onBackToDashboard = { currentScreen = null },
                onTransferSuccess = { currentScreen = null }
            )
            return
        }
        DashboardScreen.QR_PAYMENT -> {
            QRPaymentScreen(
                onBackToDashboard = { currentScreen = null }
            )
            return
        }
        DashboardScreen.DEBIT_CARD -> {
            DebitCardScreen(
                onBackToDashboard = { currentScreen = null }
            )
            return
        }
        DashboardScreen.FIXED_DEPOSIT -> {
            FixedDepositScreen(
                onBackToDashboard = { currentScreen = null }
            )
            return
        }
        else -> {
            // Show main dashboard
            DashboardContent(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                showBalance = showBalance,
                onToggleBalance = { showBalance = !showBalance },
                onLogout = onLogout,
                onUnregisterDevice = onUnregisterDevice,
                userId = userId,
                deviceUUID = deviceUUID
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    currentScreen: DashboardScreen?,
    onScreenChange: (DashboardScreen) -> Unit,
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onLogout: () -> Unit,
    onUnregisterDevice: () -> Unit,
    userId: String,
    deviceUUID: String
) {
    var securityStatus by remember { mutableStateOf(SecurityStatus.SECURE) }
    var trustScore by remember { mutableStateOf(0.85f) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var securityMessage by remember { mutableStateOf("") }

    // Simulate continuous authentication monitoring
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Check every 5 seconds
            // Simulate trust score fluctuation
            val newTrustScore = (0.6f + Math.random().toFloat() * 0.4f)
            trustScore = newTrustScore

            securityStatus = when {
                newTrustScore >= 0.8f -> SecurityStatus.SECURE
                newTrustScore >= 0.6f -> SecurityStatus.MEDIUM_RISK
                newTrustScore >= 0.4f -> SecurityStatus.HIGH_RISK
                else -> SecurityStatus.CRITICAL_RISK
            }

            // Show security alerts for medium, high, and critical risk
            if (securityStatus != SecurityStatus.SECURE) {
                securityMessage = when (securityStatus) {
                    SecurityStatus.MEDIUM_RISK -> "Enhanced security monitoring active. Please provide secondary biometric verification."
                    SecurityStatus.HIGH_RISK -> "High risk detected. Please verify identity with both biometric methods."
                    SecurityStatus.CRITICAL_RISK -> "Critical security alert! Session will be locked for your protection."
                    else -> ""
                }
                showSecurityDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = ABHEDColors.Lavender,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation Icon
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(
                            Icons.Default.Menu,
                            "Menu",
                            tint = ABHEDColors.DeftBlue
                        )
                    }
                    
                    // Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ABHED",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ABHEDColors.LapisLazuli
                        )
                        Text(
                            text = " Banking",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Normal,
                            color = ABHEDColors.Charcoal
                        )
                    }
                    
                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security Status Indicator
                        SecurityStatusIndicator(
                            securityStatus = securityStatus,
                            trustScore = trustScore
                        )
                        IconButton(onClick = { /* Search */ }) {
                            Icon(
                                Icons.Default.Search,
                                "Search",
                                tint = ABHEDColors.DeftBlue
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.Default.Logout,
                                "Logout",
                                tint = ABHEDColors.DeftBlue
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = ABHEDColors.Lavender,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarItem(Icons.Default.Home, "Home", true)
                    BottomBarItem(Icons.Default.CreditCard, "Cards", false)
                    // QR Scanner Button (Center)
                    FloatingActionButton(
                        onClick = { onScreenChange(DashboardScreen.QR_PAYMENT) },
                        modifier = Modifier.size(56.dp),
                        containerColor = ABHEDColors.LightSeaGreen,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.QrCodeScanner, "Scan QR", modifier = Modifier.size(24.dp))
                    }
                    BottomBarItem(Icons.Default.LocalOffer, "Offers", false)
                    BottomBarItem(Icons.Default.Emergency, "Emergency", false)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ABHEDColors.Lavender.copy(alpha = 0.3f))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Security Status Banner (if needed)
            if (securityStatus != SecurityStatus.SECURE) {
                SecurityBanner(securityStatus, trustScore)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Account Balances Section
            AccountBalancesSection(
                showBalance = showBalance,
                onToggleBalance = onToggleBalance,
                onScreenChange = onScreenChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Behavioral Authentication Status
            BehavioralAuthSection(trustScore, securityStatus)

            Spacer(modifier = Modifier.height(16.dp))

            // Stroke Capture Testing Section
            StrokeCaptureTestingSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Section
            QuickActionsSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Transfers Section
            TransfersSection(onScreenChange = onScreenChange)

            Spacer(modifier = Modifier.height(16.dp))

            // Cards Section
            CardsSection(onScreenChange = onScreenChange)

            Spacer(modifier = Modifier.height(16.dp))

            // Loans Section
            LoansSection(onScreenChange = onScreenChange)

            Spacer(modifier = Modifier.height(16.dp))

            // Testing Controls
            TestingControls(onUnregisterDevice)
        }
    }

    // Security Alert Dialog
    if (showSecurityDialog) {
        SecurityAlertDialog(
            securityStatus = securityStatus,
            message = securityMessage,
            onDismiss = { showSecurityDialog = false },
            onVerifyIdentity = {
                // Handle identity verification based on security level
                showSecurityDialog = false
            },
            onSecondaryBiometric = {
                // Handle secondary biometric (fingerprint/face) for medium risk
                showSecurityDialog = false
            },
            onDualBiometric = {
                // Handle both biometrics for high risk
                showSecurityDialog = false
            },
            onLockSession = {
                // Handle session lock for critical risk
                onLogout()
            }
        )
    }
}

@Composable
private fun AccountBalancesSection(
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onScreenChange: (DashboardScreen) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Account Balances",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountCard(
                    title = "Savings Account",
                    balance = if (showBalance) "₹45,678.90" else "••••••••",
                    accountNumber = "XXXX1234",
                    modifier = Modifier.weight(1f),
                    onToggleBalance = onToggleBalance,
                    onClick = { onScreenChange(DashboardScreen.SAVINGS_ACCOUNT) }
                )

                AccountCard(
                    title = "Current Account",
                    balance = if (showBalance) "₹1,23,456.78" else "••••••••",
                    accountNumber = "XXXX5678",
                    modifier = Modifier.weight(1f),
                    onToggleBalance = onToggleBalance,
                    onClick = { onScreenChange(DashboardScreen.SAVINGS_ACCOUNT) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Additional Accounts
            AccountRow("Deposit Account", "₹5,00,000.00", "XXXX9012")
            AccountRow("Linked Accounts", "3 accounts", "View all")
        }
    }
}

@Composable
private fun AccountCard(
    title: String,
    balance: String,
    accountNumber: String,
    modifier: Modifier = Modifier,
    onToggleBalance: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.let { if (onClick != null) it.clickable { onClick() } else it },
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.Periwinkle.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ABHEDColors.LapisLazuli.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ABHEDColors.DeftBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = balance,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LapisLazuli
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleBalance,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (balance.contains("•")) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle balance visibility",
                        modifier = Modifier.size(16.dp),
                        tint = ABHEDColors.GlaucousMoonstone
                    )
                }
            }

            Text(
                text = accountNumber,
                style = MaterialTheme.typography.bodySmall,
                color = ABHEDColors.GlaucousMoonstone
            )
        }
    }
}

@Composable
private fun AccountRow(
    title: String,
    value: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ABHEDColors.LightSeaGreen
        )
    }
}

@Composable
private fun QuickActionsSection() {
    SectionCard(title = "Quick Actions") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(
                ServiceItem(Icons.Default.AccountBalance, "mPassbook"),
                ServiceItem(Icons.Default.Analytics, "Spend Analyzer"),
                ServiceItem(Icons.Default.Bolt, "Recharge"),
                ServiceItem(Icons.Default.ShoppingCart, "ABHED Shoppe"),
                ServiceItem(Icons.Default.TrendingUp, "Demat & Trading")
            )) { item ->
                ServiceCard(service = item)
            }
        }
    }
}

@Composable
private fun TransfersSection(onScreenChange: (DashboardScreen) -> Unit) {
    SectionCard(title = "Transfers") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(
                ServiceItem(Icons.Default.ArrowForward, "Fund Transfer"),
                ServiceItem(Icons.Default.Phone, "Pay to Mobile"),
                ServiceItem(Icons.Default.Receipt, "Bill Pay"),
                ServiceItem(Icons.Default.People, "Manage Beneficiaries"),
                ServiceItem(Icons.Default.Payment, "UPI"),
                ServiceItem(Icons.Default.Atm, "Cashless Withdrawals")
            )) { item ->
                ServiceCard(
                    service = item,
                    onClick = when (item.title) {
                        "Fund Transfer" -> { { onScreenChange(DashboardScreen.FUND_TRANSFER) } }
                        "UPI" -> { { onScreenChange(DashboardScreen.QR_PAYMENT) } }
                        else -> null
                    }
                )
            }
        }
    }
}

@Composable
private fun CardsSection(onScreenChange: (DashboardScreen) -> Unit) {
    SectionCard(title = "Cards") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServiceCard(
                service = ServiceItem(Icons.Default.CreditCard, "Debit Card"),
                modifier = Modifier.weight(1f),
                onClick = { onScreenChange(DashboardScreen.DEBIT_CARD) }
            )
            ServiceCard(
                service = ServiceItem(Icons.Default.CreditCard, "Credit Card"),
                modifier = Modifier.weight(1f),
                onClick = { onScreenChange(DashboardScreen.DEBIT_CARD) }
            )
        }
    }
}

@Composable
private fun LoansSection(onScreenChange: (DashboardScreen) -> Unit) {
    SectionCard(title = "Loans") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(
                ServiceItem(Icons.Default.CheckCircle, "Pre Approved"),
                ServiceItem(Icons.Default.School, "Educational"),
                ServiceItem(Icons.Default.AccountBalance, "Loans against FD/RD")
            )) { item ->
                ServiceCard(
                    service = item,
                    onClick = when (item.title) {
                        "Loans against FD/RD" -> { { onScreenChange(DashboardScreen.FIXED_DEPOSIT) } }
                        else -> null
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ABHEDColors.Periwinkle.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.LapisLazuli
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun ServiceCard(
    service: ServiceItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.Lavender.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ABHEDColors.GlaucousMoonstone.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = service.title,
                modifier = Modifier.size(24.dp),
                tint = ABHEDColors.LightSeaGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = service.title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = ABHEDColors.Charcoal
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) ABHEDColors.LightSeaGreen else ABHEDColors.GlaucousMoonstone
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) ABHEDColors.LightSeaGreen else ABHEDColors.GlaucousMoonstone
        )
    }
}

@Composable
private fun TestingControls(onUnregisterDevice: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ABHEDColors.Periwinkle.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ABHEDColors.SavoyBlue.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Testing Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.SavoyBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onUnregisterDevice,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ABHEDColors.MarianBlue
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ABHEDColors.MarianBlue)
            ) {
                Text("Unregister Device (Testing)")
            }
        }
    }
}



// Security Status Enum
enum class SecurityStatus {
    SECURE, MEDIUM_RISK, HIGH_RISK, CRITICAL_RISK
}

@Composable
private fun SecurityStatusIndicator(
    securityStatus: SecurityStatus,
    trustScore: Float
) {
    val (color, icon) = when (securityStatus) {
        SecurityStatus.SECURE -> Pair(Color.Green, Icons.Default.Security)
        SecurityStatus.MEDIUM_RISK -> Pair(WarningOrange, Icons.Default.Warning)
        SecurityStatus.HIGH_RISK -> Pair(Color.Red, Icons.Default.Error)
        SecurityStatus.CRITICAL_RISK -> Pair(Color.Red, Icons.Default.Block)
    }

    IconButton(onClick = { /* Show security details */ }) {
        Icon(
            imageVector = icon,
            contentDescription = "Security Status",
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SecurityBanner(
    securityStatus: SecurityStatus,
    trustScore: Float
) {
    val (backgroundColor, textColor, message) = when (securityStatus) {
        SecurityStatus.MEDIUM_RISK -> Triple(
            WarningOrange.copy(alpha = 0.2f),
            WarningOrange.copy(alpha = 0.8f),
            "Enhanced monitoring - Secondary biometric required"
        )
        SecurityStatus.HIGH_RISK -> Triple(
            Color.Red.copy(alpha = 0.2f),
            Color.Red.copy(alpha = 0.8f),
            "High risk - Both biometric verifications required"
        )
        SecurityStatus.CRITICAL_RISK -> Triple(
            Color.Red.copy(alpha = 0.3f),
            Color.Red,
            "Critical alert - Session will be locked"
        )
        else -> Triple(Color.Transparent, Color.Transparent, "")
    }

    if (message.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Warning",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BehavioralAuthSection(
    trustScore: Float,
    securityStatus: SecurityStatus
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ABHED Security Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ABHEDColors.LapisLazuli
                )

                val statusColor = when (securityStatus) {
                    SecurityStatus.SECURE -> Color.Green
                    SecurityStatus.MEDIUM_RISK -> WarningOrange
                    SecurityStatus.HIGH_RISK -> Color.Red
                    SecurityStatus.CRITICAL_RISK -> Color.Red
                }

                Text(
                    text = when (securityStatus) {
                        SecurityStatus.SECURE -> "SECURE"
                        SecurityStatus.MEDIUM_RISK -> "MEDIUM RISK"
                        SecurityStatus.HIGH_RISK -> "HIGH RISK"
                        SecurityStatus.CRITICAL_RISK -> "CRITICAL"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trust Score Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trust Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ABHEDColors.Charcoal
                    )
                    Text(
                        text = "${(trustScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ABHEDColors.LightSeaGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = trustScore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        trustScore >= 0.8f -> Color.Green
                        trustScore >= 0.6f -> WarningOrange
                        else -> Color.Red
                    },
                    trackColor = ABHEDColors.Lavender
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Authentication Methods Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AuthMethodStatus("Touch", true)
                AuthMethodStatus("Navigation", true)
                AuthMethodStatus("Location", trustScore > 0.7f)
                AuthMethodStatus("Face", trustScore > 0.6f)
            }
        }
    }
}

@Composable
private fun AuthMethodStatus(
    method: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = "$method Status",
            tint = if (isActive) Color.Green else Color.Red,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = method,
            style = MaterialTheme.typography.bodySmall,
            color = ABHEDColors.Charcoal
        )
    }
}

@Composable
private fun SecurityAlertDialog(
    securityStatus: SecurityStatus,
    message: String,
    onDismiss: () -> Unit,
    onVerifyIdentity: () -> Unit,
    onSecondaryBiometric: () -> Unit,
    onDualBiometric: () -> Unit,
    onLockSession: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (securityStatus) {
                        SecurityStatus.HIGH_RISK -> Icons.Default.Warning
                        SecurityStatus.CRITICAL_RISK -> Icons.Default.Error
                        else -> Icons.Default.Info
                    },
                    contentDescription = "Security Alert",
                    tint = when (securityStatus) {
                        SecurityStatus.HIGH_RISK -> WarningOrange
                        SecurityStatus.CRITICAL_RISK -> Color.Red
                        else -> ABHEDColors.LapisLazuli
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Security Alert",
                    color = ABHEDColors.LapisLazuli
                )
            }
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Show biometric requirements based on security level
                when (securityStatus) {
                    SecurityStatus.MEDIUM_RISK -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = WarningOrange.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Required: Secondary Biometric",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Fingerprint OR Face recognition",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.Charcoal
                                )
                            }
                        }
                    }
                    SecurityStatus.HIGH_RISK -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Required: Dual Biometric Verification",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                                Text(
                                    text = "• Fingerprint AND Face recognition",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ABHEDColors.Charcoal
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (securityStatus) {
                SecurityStatus.MEDIUM_RISK -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onSecondaryBiometric,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = ABHEDColors.LightSeaGreen
                            )
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fingerprint")
                        }

                        TextButton(
                            onClick = onSecondaryBiometric,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = ABHEDColors.LightSeaGreen
                            )
                        ) {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Face ID")
                        }
                    }
                }
                SecurityStatus.HIGH_RISK -> {
                    TextButton(
                        onClick = onDualBiometric,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Both Biometrics")
                    }
                }
                SecurityStatus.CRITICAL_RISK -> {
                    TextButton(
                        onClick = onLockSession,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Session")
                    }
                }
                else -> {
                    TextButton(
                        onClick = onVerifyIdentity,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ABHEDColors.LightSeaGreen
                        )
                    ) {
                        Text("Verify Identity")
                    }
                }
            }
        },
        dismissButton = {
            if (securityStatus != SecurityStatus.CRITICAL_RISK) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ABHEDColors.GlaucousMoonstone
                    )
                ) {
                    Text("Continue")
                }
            }
        },
        containerColor = Color.White,
        titleContentColor = ABHEDColors.LapisLazuli,
        textContentColor = ABHEDColors.Charcoal
    )
}

@Composable
private fun StrokeCaptureTestingSection() {
    val context = LocalContext.current
    
    SectionCard(title = "Stroke Capture Testing") {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Test the new stroke capture system for behavioral authentication",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = ABHEDColors.Charcoal,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = {
                    val intent = android.content.Intent(context, StrokeCaptureTestActivity::class.java)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ABHEDColors.LightSeaGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Stroke Capture Test")
            }
            
            Text(
                text = "This will open a new screen where you can draw strokes and see them sent to the server in real-time",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = ABHEDColors.GlaucousMoonstone,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

data class ServiceItem(
    val icon: ImageVector,
    val title: String
)

enum class DashboardScreen {
    SAVINGS_ACCOUNT,
    FUND_TRANSFER,
    QR_PAYMENT,
    DEBIT_CARD,
    FIXED_DEPOSIT
}
