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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ComplaintsScreen(viewModel: DriverViewModel) {
    val complaints by viewModel.complaints.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showRaiseModal by remember { mutableStateOf(false) }
    var rideIdInput by remember { mutableStateOf("ride-101") }
    var againstInput by remember { mutableStateOf("Customer") }
    var categoryInput by remember { mutableStateOf("Fare Dispute / Toll Reimbursement") }
    var descriptionInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Complaints & Support",
                onBack = { viewModel.navigateTo(Screen.Profile) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Past Complaints (${complaints.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                Button(
                    onClick = { showRaiseModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DriverOfflineRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raise Ticket", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (complaints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛡️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No complaints raised", color = TextSecondaryDark, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(complaints) { c ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Report, contentDescription = null, tint = DriverWarningAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = c.category,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Surface(
                                        color = if (c.status == "resolved") DriverOnlineGreen.copy(alpha = 0.2f) else DriverWarningAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = c.status.uppercase(),
                                            color = if (c.status == "resolved") DriverOnlineGreen else DriverWarningAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = c.description,
                                    color = TextSecondaryDark,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Against: ${c.against} • Ride: ${c.ride_id} • ${c.created_at}",
                                    color = TextSecondaryDark.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRaiseModal) {
        Dialog(onDismissRequest = { showRaiseModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Raise Support Complaint",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Complaint Category") },
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
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Detailed Description") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showRaiseModal = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.raiseComplaint(
                                    rideId = rideIdInput,
                                    against = againstInput,
                                    category = categoryInput,
                                    description = descriptionInput,
                                    onSuccess = { showRaiseModal = false }
                                )
                            },
                            enabled = descriptionInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOfflineRed)
                        ) {
                            Text("Submit Ticket", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
