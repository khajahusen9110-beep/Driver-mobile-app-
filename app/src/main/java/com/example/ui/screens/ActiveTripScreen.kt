package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.DriverSosRed
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ActiveTripScreen(viewModel: DriverViewModel) {
    val activeRide by viewModel.activeRide.collectAsState()
    val participantInfo by viewModel.participantInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()
    val context = LocalContext.current

    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("Passenger no-show after waiting 5 mins") }

    var showSosConfirmation by remember { mutableStateOf(false) }
    var showReturnTripDialog by remember { mutableStateOf(false) }
    var returnDestination by remember { mutableStateOf("Bangalore Central") }

    LaunchedEffect(activeRide?.id) {
        activeRide?.let { viewModel.fetchParticipantInfo(it.id) }
    }

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Active Trip",
                actions = {
                    // Persistent Emergency SOS Button
                    Button(
                        onClick = { showSosConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DriverSosRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("active_trip_sos_button")
                    ) {
                        Text("🚨 SOS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            )
        },
        bottomBar = {
            DriverBottomNav(
                currentScreen = Screen.ActiveTrip,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (activeRide == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛣️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Active Trip in Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Go online on the Home screen to accept incoming ride requests.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Home Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val ride = activeRide!!
            val status = ride.status // accepted, arrived, ongoing

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

                // Status Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
                            "arrived" -> DriverWarningAmber.copy(alpha = 0.2f)
                            "ongoing" -> DriverSecondary.copy(alpha = 0.2f)
                            else -> DriverOnlineGreen.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (status) {
                                    "ongoing" -> Icons.Default.DirectionsCar
                                    "arrived" -> Icons.Default.LocationOn
                                    else -> Icons.Default.Navigation
                                },
                                contentDescription = null,
                                tint = when (status) {
                                    "arrived" -> DriverWarningAmber
                                    "ongoing" -> DriverSecondary
                                    else -> DriverOnlineGreen
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (status) {
                                    "accepted" -> "ON THE WAY TO PICKUP"
                                    "arrived" -> "ARRIVED AT PICKUP LOCATION"
                                    "ongoing" -> "TRIP IN PROGRESS"
                                    else -> status.uppercase()
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "₹${String.format("%.0f", ride.fare_amount)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = DriverOnlineGreen,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Customer & Vehicle Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(DriverPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = DriverPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = participantInfo?.customer_name ?: ride.customer_name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Assigned Vehicle: ${participantInfo?.vehicle_plate ?: "KA-01-MJ-4029"}",
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            // Tap to call button
                            IconButton(
                                onClick = {
                                    val phoneNum = participantInfo?.customer_phone ?: ride.customer_phone
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNum"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DriverOnlineGreen)
                                    .testTag("tap_to_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Customer",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DriverDarkSurfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Pickup
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DriverOnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("PICKUP LOCATION", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                Text(ride.pickup_address, fontSize = 13.sp, color = TextPrimaryDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Drop
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DriverOfflineRed)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("DROP DESTINATION", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                Text(ride.drop_address, fontSize = 13.sp, color = TextPrimaryDark)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Navigation Launch Button
                Button(
                    onClick = {
                        val lat = if (status == "accepted") ride.pickup_lat else ride.drop_lat
                        val lng = if (status == "accepted") ride.pickup_lng else ride.drop_lng
                        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            // Fallback to browser or generic geo uri
                            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                            context.startActivity(fallback)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DriverSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_navigation_button")
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status == "accepted") "NAVIGATE TO PICKUP (GOOGLE MAPS)" else "NAVIGATE TO DROP LOCATION",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trip Lifecycle Action Buttons
                when (status) {
                    "accepted" -> {
                        Button(
                            onClick = { viewModel.updateRideStatus("arrived") },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DriverWarningAmber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("arrived_button")
                        ) {
                            Text("I HAVE ARRIVED AT PICKUP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    "arrived" -> {
                        Button(
                            onClick = { showOtpDialog = true },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOnlineGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_trip_otp_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START TRIP (ENTER OTP)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    "ongoing" -> {
                        Button(
                            onClick = {
                                viewModel.updateRideStatus("completed")
                                if (ride.is_outstation || ride.distance_km > 15) {
                                    showReturnTripDialog = true
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOnlineGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_trip_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMPLETE TRIP & COLLECT FARE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel Trip
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("cancel_trip_button")
                ) {
                    Text("Cancel Ride Request", color = DriverOfflineRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Suspension Warning Notice
                Surface(
                    color = DriverDarkSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DriverWarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notice: 3 or more cancellations in a 24-hour period triggers automatic account suspension.",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // OTP Input Dialog (Customer tells driver the OTP)
    if (showOtpDialog) {
        Dialog(onDismissRequest = { showOtpDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Enter Customer 4-Digit OTP",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask customer for the 4-digit start OTP shown on their app.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { if (it.length <= 4) enteredOtp = it },
                        label = { Text("Trip OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("active_trip_otp_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showOtpDialog = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateRideStatus("ongoing", otp = enteredOtp)
                                showOtpDialog = false
                            },
                            enabled = enteredOtp.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            modifier = Modifier.testTag("submit_trip_otp_button")
                        ) {
                            Text("Verify & Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Cancel Dialog with Reason
    if (showCancelDialog) {
        Dialog(onDismissRequest = { showCancelDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Confirm Trip Cancellation",
                        fontWeight = FontWeight.Bold,
                        color = DriverOfflineRed,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Warning: Multiple cancellations can affect your driver compliance rating and trigger auto-suspension.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
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
                        OutlinedButton(onClick = { showCancelDialog = false }) {
                            Text("Go Back", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateRideStatus("cancelled", cancelReason = cancelReason)
                                showCancelDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverOfflineRed),
                            modifier = Modifier.testTag("confirm_cancel_trip_button")
                        ) {
                            Text("Confirm Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // SOS Emergency Confirmation
    if (showSosConfirmation) {
        Dialog(onDismissRequest = { showSosConfirmation = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DriverSosRed, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TRIGGER EMERGENCY SOS",
                            fontWeight = FontWeight.ExtraBold,
                            color = DriverSosRed,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This will immediately send your live GPS coordinates to our 24/7 Safety Command Center, police emergency response, and registered emergency contacts.",
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showSosConfirmation = false }) {
                            Text("Dismiss", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.triggerSos()
                                showSosConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverSosRed),
                            modifier = Modifier.testTag("confirm_sos_button")
                        ) {
                            Text("BROADCAST SOS NOW", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}
