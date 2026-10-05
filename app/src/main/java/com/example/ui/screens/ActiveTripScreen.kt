package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RideStatus
import com.example.ui.DriverViewModel
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.Format
import com.example.ui.components.InfoRow
import com.example.ui.components.LoadingBox
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionCard
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSosRed
import com.example.ui.theme.TextSecondaryDark

private val cancelReasons = listOf(
    "Customer not reachable",
    "Customer asked me to cancel",
    "Pickup location is too far",
    "Vehicle problem",
    "Personal emergency",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(viewModel: DriverViewModel) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ride = trip.ride
    var otp by rememberSaveable { mutableStateOf("") }
    var showCancel by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }
    var confirmSos by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ride?.status?.label ?: "Trip", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { confirmSos = true }) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DriverSosRed)
                        Spacer(Modifier.width(4.dp))
                        Text("SOS", color = DriverSosRed, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (ride == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val participants = trip.participants
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val step = when (ride.status) {
                RideStatus.ACCEPTED -> 1
                RideStatus.ARRIVED -> 2
                RideStatus.ONGOING -> 3
                else -> 4
            }
            LinearProgressIndicator(progress = { step / 4f }, modifier = Modifier.fillMaxWidth(), color = DriverPrimary)

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondaryDark)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(participants?.customerName ?: "Customer", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(Format.money(ride.fare) + " · " + Format.km(ride.distanceKm), color = TextSecondaryDark)
                    }
                    val phone = participants?.customerPhone
                    FilledTonalButton(onClick = { dial(context, phone) }, enabled = phone != null) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                }
            }

            SectionCard {
                AddressLine(DriverOnlineGreen, ride.pickupAddress ?: "Pickup point")
                Spacer(Modifier.height(8.dp))
                AddressLine(DriverOfflineRed, ride.dropAddress ?: "Drop point")
                Spacer(Modifier.height(12.dp))
                val toPickup = ride.status != RideStatus.ONGOING
                SecondaryButton(
                    if (toPickup) "Navigate to pickup" else "Navigate to drop",
                    onClick = {
                        if (toPickup) navigate(context, ride.pickupLat, ride.pickupLng)
                        else navigate(context, ride.dropLat, ride.dropLng)
                    },
                )
            }

            SectionCard {
                ride.passengerCount?.let { InfoRow("Passengers", "$it") }
                ride.goodsDescription?.let { InfoRow("Goods", it) }
                ride.goodsWeightKg?.let { InfoRow("Weight", "${it.toInt()} kg") }
                ride.durationMin?.let { InfoRow("Estimated time", "${it.toInt()} min") }
                ride.scheduledAt?.let { InfoRow("Pickup time", Format.dateTime(it)) }
                InfoRow("Accepted", Format.time(ride.acceptedAt))
                participants?.vehiclePlate?.let { InfoRow("Vehicle", it) }
            }

            when (ride.status) {
                RideStatus.ACCEPTED -> {
                    PrimaryButton("I've arrived at pickup", onClick = viewModel::markArrived, loading = busy == "Updating")
                }
                RideStatus.ARRIVED -> {
                    Text("Ask the customer for their 4-digit ride code", color = TextSecondaryDark)
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { v -> otp = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Ride code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 12.sp, textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton("Start trip", onClick = { viewModel.startTrip(otp) }, enabled = otp.length == 4,
                        loading = busy == "Starting trip")
                }
                RideStatus.ONGOING -> {
                    Text("Started ${Format.time(ride.startedAt)}", color = TextSecondaryDark)
                    PrimaryButton("Complete trip", onClick = { confirmComplete = true }, loading = busy == "Completing trip")
                }
                else -> Unit
            }
            if (ride.status == RideStatus.ACCEPTED || ride.status == RideStatus.ARRIVED) {
                TextButton(onClick = { showCancel = true }, enabled = busy == null, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancel trip", color = DriverOfflineRed)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCancel) {
        CancelDialog(onDismiss = { showCancel = false }) { reason ->
            showCancel = false
            viewModel.cancelTrip(reason)
        }
    }
    if (confirmComplete) {
        ConfirmDialog(
            title = "Complete this trip?",
            message = "Only complete the trip once the customer has reached the drop point.",
            confirmText = "Complete",
            onConfirm = viewModel::completeTrip,
            onDismiss = { confirmComplete = false },
        )
    }
    if (confirmSos) {
        AlertDialog(
            onDismissRequest = { confirmSos = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DriverSosRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Emergency?") },
            text = { Text("We'll alert the GoRide safety team with your live location. For immediate help, call 112.") },
            confirmButton = {
                TextButton(onClick = { confirmSos = false; viewModel.triggerSos() }) {
                    Text("Send SOS", color = DriverSosRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSos = false; dial(context, "112") }) { Text("Call 112") }
            },
        )
    }
}

@Composable
private fun CancelDialog(onDismiss: () -> Unit, onCancel: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why are you cancelling?") },
        text = {
            Column {
                cancelReasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == reason, onClick = { selected = reason })
                        Text(reason)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Frequent cancellations can affect your account.", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let(onCancel) }, enabled = selected != null) { Text("Cancel trip", color = DriverOfflineRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep trip") } },
    )
}

private fun dial(context: Context, phone: String?) {
    if (phone.isNullOrBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun navigate(context: Context, lat: Double, lng: Double) {
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=d")).setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(nav)
    } catch (e: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) }
    }
}
