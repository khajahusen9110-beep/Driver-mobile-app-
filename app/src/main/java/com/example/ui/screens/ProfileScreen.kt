package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun ProfileScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsState()
    val ratingsSummary by viewModel.ratingsSummary.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val activeReturnOffer by viewModel.activeReturnOffer.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showReturnOfferModal by remember { mutableStateOf(false) }
    var returnAddressInput by remember { mutableStateOf("Bangalore City Center / MG Road") }

    var showDeleteAccountModal by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("Switching to full-time employment") }

    var showLegalModal by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Driver Profile",
                onNotificationClick = null
            )
        },
        bottomBar = {
            DriverBottomNav(
                currentScreen = Screen.Profile,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

            // Profile Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DriverPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DriverPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = profile?.full_name ?: "Driver Partner",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 18.sp
                            )
                            Text(
                                text = profile?.phone ?: "+91 98765 00001",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = DriverOnlineGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "VERIFIED PARTNER",
                                    color = DriverOnlineGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratings & Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = DriverWarningAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.2f", ratingsSummary.avg_rating),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    fontSize = 16.sp
                                )
                            }
                            Text("Rating", fontSize = 11.sp, color = TextSecondaryDark)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ratingsSummary.total_reviews}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 16.sp
                            )
                            Text("Reviews", fontSize = 11.sp, color = TextSecondaryDark)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${profile?.total_trips ?: 328}",
                                fontWeight = FontWeight.Bold,
                                color = DriverPrimary,
                                fontSize = 16.sp
                            )
                            Text("Trips Taken", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Return Trip Offer (Outstation drops feature - Section 10)
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = DriverSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Return Ride Offer (Outstation)",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            color = DriverSecondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "30% DISCOUNT",
                                color = DriverSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Heading back home from an outstation drop? Post a return trip offer with a 30% discount to get matched on your return route.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeReturnOffer != null) {
                        Surface(
                            color = DriverOnlineGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("✓ Active Offer Posted to ${activeReturnOffer!!.to_address}", color = DriverOnlineGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Valid for ${activeReturnOffer!!.valid_minutes} minutes • Status: ${activeReturnOffer!!.status.uppercase()}", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showReturnOfferModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("Post Return Trip Offer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Management Navigation Menu List
            Text("Fleet & Account Management", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.DirectionsCar,
                        title = "Vehicles & Fleet",
                        subtitle = "Multi-vehicle support & default selection",
                        onClick = { viewModel.navigateTo(Screen.Vehicles) }
                    )
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    ProfileMenuItem(
                        icon = Icons.Default.Description,
                        title = "Compliance Documents",
                        subtitle = "DL, RC, Insurance & expiry alerts",
                        onClick = { viewModel.navigateTo(Screen.Documents) }
                    )
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    ProfileMenuItem(
                        icon = Icons.Default.CardGiftcard,
                        title = "Incentives & Referrals",
                        subtitle = "Referral code & active trip rush bonuses",
                        onClick = { viewModel.navigateTo(Screen.Incentives) }
                    )
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    ProfileMenuItem(
                        icon = Icons.Default.Report,
                        title = "Complaints & Support",
                        subtitle = "Raise fare dispute or passenger report",
                        onClick = { viewModel.navigateTo(Screen.Complaints) }
                    )
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    ProfileMenuItem(
                        icon = Icons.Default.Gavel,
                        title = "Legal Terms & Conditions",
                        subtitle = "Fleet partner agreement & compliance",
                        onClick = { showLegalModal = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Reviews List
            Text("Customer Compliments & Reviews (${reviews.size})", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reviews.forEach { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = r.customer_name, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 13.sp)
                                Row {
                                    repeat(r.rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = DriverWarningAmber, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "\"${r.review_text}\"", color = TextSecondaryDark, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Account & Logout Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDeleteAccountModal = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("delete_account_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DriverOfflineRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Account", color = DriverOfflineRed, fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = DriverDarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("logout_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Out", color = TextPrimaryDark, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Return Trip Offer Dialog
    if (showReturnOfferModal) {
        Dialog(onDismissRequest = { showReturnOfferModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Offer a Return Trip",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sets a 30% discounted return trip offer to passengers heading towards your destination.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = returnAddressInput,
                        onValueChange = { returnAddressInput = it },
                        label = { Text("Destination / Return Area") },
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
                        OutlinedButton(onClick = { showReturnOfferModal = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.offerReturnTrip(
                                    toAddress = returnAddressInput,
                                    discount = 30,
                                    validMinutes = 120,
                                    onSuccess = { showReturnOfferModal = false }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverSecondary)
                        ) {
                            Text("Publish Offer", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Delete Account Dialog
    if (showDeleteAccountModal) {
        Dialog(onDismissRequest = { showDeleteAccountModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Request Account Deletion",
                        fontWeight = FontWeight.Bold,
                        color = DriverOfflineRed,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Note: System validates that you have no active trips, zero wallet balance, and no pending withdrawals before deleting.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for Deletion") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverOfflineRed,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showDeleteAccountModal = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.requestAccountDeletion(deleteReason)
                                showDeleteAccountModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOfflineRed)
                        ) {
                            Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Legal Terms & Conditions Dialog
    if (showLegalModal) {
        Dialog(onDismissRequest = { showLegalModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Driver Partner Terms & Conditions",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Commission & Payouts: Driver partners receive 82% of net ride fares minus applicable taxes. Payouts can be requested via the Wallet screen at any time for balances over ₹100.\n\n" +
                                "2. Vehicle Compliance: You must maintain a valid Driving License, Registration Certificate (RC), and active Third-Party / Comprehensive Insurance at all times. Expired documents will block going online.\n\n" +
                                "3. Safety & Cancellations: Excessive trip cancellations (3+ in 24 hours) will trigger automatic review and suspension to protect customer reliability.\n\n" +
                                "4. Real-time Location: While online, the app broadcasts GPS coordinates every 20 seconds for dispatch and navigation accuracy.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showLegalModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DriverPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextSecondaryDark,
            modifier = Modifier.size(16.dp)
        )
    }
}
