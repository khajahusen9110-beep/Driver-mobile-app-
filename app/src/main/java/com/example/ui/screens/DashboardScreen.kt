package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.RideRequest
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.components.NotificationsDialog
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
fun DashboardScreen(viewModel: DriverViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    val openRides by viewModel.openRides.collectAsState()
    val activeRide by viewModel.activeRide.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val dailyEarnings by viewModel.dailyEarnings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var rejectDialogRide by remember { mutableStateOf<RideRequest?>(null) }
    var selectedRejectReason by remember { mutableStateOf("Too far from current location") }

    val todayEarn = dailyEarnings.firstOrNull()?.net_earnings ?: 1508.8
    val todayTrips = dailyEarnings.firstOrNull()?.rides_count ?: 6

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Rider Partner",
                onNotificationClick = { showNotificationsDialog = true },
                unreadNotifications = notifications.count { !it.is_read }
            )
        },
        bottomBar = {
            DriverBottomNav(
                currentScreen = Screen.Dashboard,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

            // Driver Header & Online Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = profile?.full_name ?: "Driver Partner",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 17.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = DriverWarningAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.2f", profile?.rating ?: 4.92),
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " (${profile?.total_trips ?: 328} trips)",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Big Online/Offline Button
                        Button(
                            onClick = { viewModel.toggleOnline(!isOnline) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) DriverOnlineGreen else DriverDarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("driver_online_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isOnline) Color.Black else TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "ONLINE" else "GO ONLINE",
                                color = if (isOnline) Color.Black else TextSecondaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Today's Net", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "₹${String.format("%.0f", todayEarn)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DriverOnlineGreen
                            )
                        }
                        Column {
                            Text("Trips Today", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "$todayTrips rides",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryDark
                            )
                        }
                        Column {
                            Text("Wallet Balance", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "₹${String.format("%.2f", walletBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DriverSecondary
                            )
                        }
                    }
                }
            }

            // Radar animation or offline prompt
            if (isOnline) {
                val infiniteTransition = rememberInfiniteTransition(label = "radar")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Surface(
                    color = DriverPrimary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .scale(pulseScale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DriverOnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS Live: Broadcasting location every 20s • Waiting for trips...",
                            fontSize = 12.sp,
                            color = DriverPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Surface(
                    color = DriverDarkSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You are currently OFFLINE. Tap 'GO ONLINE' to receive trips.",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // Open Ride Requests Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Ride Requests (${openRides.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )
                if (isOnline) {
                    IconButton(onClick = { viewModel.loadOpenRides() }) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Refresh",
                            tint = DriverPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (openRides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚗", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isOnline) "Searching for rides near you..." else "Go online to see open ride requests",
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(openRides, key = { it.id }) { ride ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ride_card_${ride.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top row: Category, Outstation chip, Fare
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = ride.vehicle_emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ride.vehicle_type_name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            fontSize = 14.sp
                                        )
                                        if (ride.is_outstation) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = DriverSecondary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "OUTSTATION",
                                                    color = DriverSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "₹${String.format("%.0f", ride.fare_amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DriverOnlineGreen,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Pickup address
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(DriverOnlineGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("PICKUP", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                        Text(ride.pickup_address, fontSize = 13.sp, color = TextPrimaryDark)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Drop address
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(DriverOfflineRed)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("DROP", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                        Text(ride.drop_address, fontSize = 13.sp, color = TextPrimaryDark)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Estimated trip distance: ${ride.distance_km} km",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action buttons: Accept & Reject
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { rejectDialogRide = ride },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("reject_button_${ride.id}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = DriverOfflineRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Decline", color = DriverOfflineRed, fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.acceptRide(ride) },
                                        colors = ButtonDefaults.buttonColors(containerColor = DriverOnlineGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.5f).height(44.dp).testTag("accept_button_${ride.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ACCEPT RIDE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    // Reject Reason Dialog
    if (rejectDialogRide != null) {
        val rideToReject = rejectDialogRide!!
        val reasons = listOf(
            "Too far from current location",
            "Heavy traffic on pickup route",
            "Fuel / charging stop required",
            "Vehicle issue or flat tire",
            "Personal break or emergency"
        )
        Dialog(onDismissRequest = { rejectDialogRide = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Decline Ride Request",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a reason for declining",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    reasons.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRejectReason = r }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRejectReason == r,
                                onClick = { selectedRejectReason = r },
                                colors = RadioButtonDefaults.colors(selectedColor = DriverPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = r, color = TextPrimaryDark, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { rejectDialogRide = null }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.rejectRide(rideToReject, selectedRejectReason)
                                rejectDialogRide = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOfflineRed)
                        ) {
                            Text("Confirm Decline", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
