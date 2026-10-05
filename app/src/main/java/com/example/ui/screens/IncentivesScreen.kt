package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSecondary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun IncentivesScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsState()
    val incentives by viewModel.incentives.collectAsState()
    val referrals by viewModel.referrals.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()
    val context = LocalContext.current

    var applyCodeInput by remember { mutableStateOf("") }
    var codeAppliedSuccess by remember { mutableStateOf(false) }

    val myReferralCode = profile?.referral_code ?: "ARJUN500"

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Incentives & Referrals",
                onBack = { viewModel.navigateTo(Screen.Profile) }
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

            Spacer(modifier = Modifier.height(6.dp))

            // My Referral Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Driver Referral Code", fontSize = 13.sp, color = TextSecondaryDark)
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = DriverPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myReferralCode,
                                fontWeight = FontWeight.ExtraBold,
                                color = DriverPrimary,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Referral Code", myReferralCode)
                                    clipboard.setPrimaryClip(clip)
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondaryDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Earn ₹250 for each driver friend who signs up and completes 10 trips.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apply Referral Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Apply Referral Code", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = applyCodeInput,
                            onValueChange = { applyCodeInput = it.uppercase() },
                            placeholder = { Text("Enter code (e.g. FRIEND250)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.applyReferral(applyCodeInput) {
                                    codeAppliedSuccess = true
                                }
                            },
                            enabled = applyCodeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (codeAppliedSuccess) {
                        Text(
                            text = "✓ Referral bonus of ₹250 applied successfully!",
                            color = DriverOnlineGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Active Incentive Programs (${incentives.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(incentives) { inc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = DriverWarningAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = inc.program_title,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 15.sp
                                    )
                                }

                                Text(
                                    text = "₹${String.format("%.0f", inc.reward_amount)} Bonus",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DriverOnlineGreen,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress Bar
                            val progress = (inc.completed_trips.toFloat() / inc.target_trips.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = DriverPrimary,
                                trackColor = DriverDarkSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${inc.completed_trips} of ${inc.target_trips} rides completed",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (inc.is_completed) "CLAIMED" else "${(progress * 100).toInt()}% Done",
                                    color = if (inc.is_completed) DriverOnlineGreen else DriverPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
