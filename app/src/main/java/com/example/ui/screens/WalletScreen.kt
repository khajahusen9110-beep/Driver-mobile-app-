package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WalletTransaction
import com.example.data.Withdrawal
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.Field
import com.example.ui.components.Format
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusChip
import com.example.ui.components.Tone
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.TextSecondaryDark

@Composable
fun WalletScreen(viewModel: DriverViewModel) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var showWithdraw by rememberSaveable { mutableStateOf(false) }
    val openRequest = wallet.withdrawals.firstOrNull { it.isOpen }

    Scaffold(
        topBar = { DriverTopBar("Wallet") },
        bottomBar = { DriverBottomNav(Screen.Wallet, viewModel::navigate) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Text("Available balance", color = TextSecondaryDark)
                    Text(Format.money(wallet.balance), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = DriverPrimary)
                    Spacer(Modifier.height(12.dp))
                    if (openRequest != null) {
                        Text(
                            "Payout of ${Format.money(openRequest.amount)} is ${openRequest.status}. You can request another once it is paid.",
                            color = TextSecondaryDark,
                        )
                    } else {
                        PrimaryButton("Withdraw to bank", onClick = { showWithdraw = true }, enabled = wallet.balance > 0)
                    }
                }
            }
            if (wallet.withdrawals.isNotEmpty()) {
                item { SectionTitle("Payouts") }
                items(wallet.withdrawals, key = { it.id }) { WithdrawalRow(it) }
            }
            item { SectionTitle("Transactions") }
            if (wallet.transactions.isEmpty()) {
                item { EmptyState(Icons.Default.ReceiptLong, "No transactions yet", "Trip earnings and payouts will appear here.") }
            } else {
                items(wallet.transactions, key = { it.id }) { TransactionRow(it) }
            }
        }
    }

    if (showWithdraw) {
        WithdrawDialog(
            balance = wallet.balance,
            busy = busy != null,
            onDismiss = { showWithdraw = false },
            onSubmit = { amount, account, confirm, ifsc ->
                viewModel.requestWithdrawal(amount, account, confirm, ifsc) { showWithdraw = false }
            },
        )
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (tx.isCredit) DriverOnlineGreen else DriverOfflineRed,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.reason.ifBlank { if (tx.isCredit) "Credit" else "Debit" }, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(Format.dateTime(tx.createdAt), color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                (if (tx.isCredit) "+" else "−") + Format.money(tx.amount),
                color = if (tx.isCredit) DriverOnlineGreen else DriverOfflineRed,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WithdrawalRow(w: Withdrawal) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.money(w.amount), fontWeight = FontWeight.Bold)
                Text("${w.maskedAccount} · ${w.ifsc}", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                Text("Requested ${Format.dateTime(w.requestedAt)}", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                w.adminNote?.let { Text(it, color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall) }
            }
            val (label, tone) = when (w.status) {
                "paid" -> "Paid" to Tone.Good
                "approved" -> "Approved" to Tone.Info
                "rejected" -> "Rejected" to Tone.Bad
                else -> "Pending" to Tone.Warn
            }
            StatusChip(label, tone)
        }
    }
}

@Composable
private fun WithdrawDialog(
    balance: Double,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var ifsc by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw to bank") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Available: ${Format.money(balance)}", color = TextSecondaryDark)
                Field(amount, { v -> amount = v.filter { it.isDigit() || it == '.' }.take(9) }, "Amount (₹)", keyboardType = KeyboardType.Decimal)
                Field(account, { v -> account = v.filter { it.isDigit() }.take(18) }, "Account number", keyboardType = KeyboardType.NumberPassword)
                Field(confirm, { v -> confirm = v.filter { it.isDigit() }.take(18) }, "Re-enter account number", keyboardType = KeyboardType.Number)
                Field(ifsc, { v -> ifsc = v.uppercase().filter { it.isLetterOrDigit() }.take(11) }, "IFSC code", placeholder = "HDFC0001234")
                Text("Payouts are reviewed by our team and sent to this account.", color = TextSecondaryDark,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(amount, account, confirm, ifsc) }, enabled = !busy) { Text("Request payout") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
