package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSecondary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun WalletScreen(viewModel: DriverViewModel) {
    val walletBalance by viewModel.walletBalance.collectAsState()
    val transactions by viewModel.walletTransactions.collectAsState()
    val withdrawals by viewModel.withdrawalRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showWithdrawModal by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("1000") }
    var bankAccount by remember { mutableStateOf("50100412398412") }
    var ifscCode by remember { mutableStateOf("HDFC0000128") }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Withdrawals

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Driver Wallet",
                onNotificationClick = null
            )
        },
        bottomBar = {
            DriverBottomNav(
                currentScreen = Screen.Wallet,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

            // Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Withdrawable Balance",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = DriverPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "₹${String.format("%.2f", walletBalance)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryDark,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showWithdrawModal = true },
                        enabled = walletBalance > 100,
                        colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_withdrawal_button")
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Request Bank Withdrawal",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs for Transactions vs Withdrawals
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DriverDarkSurface,
                contentColor = DriverPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Transactions (${transactions.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Withdrawals (${withdrawals.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Transactions List
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No wallet transactions found", color = TextSecondaryDark)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            val isCredit = tx.type.lowercase() == "credit"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCredit) DriverOnlineGreen.copy(alpha = 0.15f) else DriverOfflineRed.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isCredit) DriverOnlineGreen else DriverOfflineRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tx.description,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = tx.created_at,
                                                color = TextSecondaryDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = (if (isCredit) "+" else "-") + "₹${String.format("%.2f", tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCredit) DriverOnlineGreen else DriverOfflineRed,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Withdrawals List
                if (withdrawals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No withdrawal requests yet", color = TextSecondaryDark)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(withdrawals) { wd ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "₹${String.format("%.2f", wd.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            fontSize = 16.sp
                                        )
                                        Surface(
                                            color = when (wd.status.lowercase()) {
                                                "processed" -> DriverOnlineGreen.copy(alpha = 0.2f)
                                                "rejected" -> DriverOfflineRed.copy(alpha = 0.2f)
                                                else -> DriverWarningAmber.copy(alpha = 0.2f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = wd.status.uppercase(),
                                                color = when (wd.status.lowercase()) {
                                                    "processed" -> DriverOnlineGreen
                                                    "rejected" -> DriverOfflineRed
                                                    else -> DriverWarningAmber
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To A/C: ${wd.bank_account} • IFSC: ${wd.ifsc}",
                                        color = TextSecondaryDark,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Requested: ${wd.created_at}",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Withdrawal Modal
    if (showWithdrawModal) {
        Dialog(onDismissRequest = { showWithdrawModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Request Withdrawal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Text(
                        text = "Direct NEFT / IMPS transfer to your bank",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bankAccount,
                        onValueChange = { bankAccount = it },
                        label = { Text("Bank Account Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it.uppercase() },
                        label = { Text("Bank IFSC Code") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showWithdrawModal = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                                viewModel.requestWithdrawal(
                                    amount = amt,
                                    bankAccount = bankAccount.trim(),
                                    ifsc = ifscCode.trim(),
                                    onSuccess = { showWithdrawModal = false }
                                )
                            },
                            enabled = !isLoading && withdrawAmount.isNotBlank() && bankAccount.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            modifier = Modifier.testTag("submit_withdrawal_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Submit Request", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
