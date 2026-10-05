package com.example.ui.screens

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Ride
import com.example.data.ReturnTripOffer
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.Field
import com.example.ui.components.Format
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusChip
import com.example.ui.components.Tone
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val state by viewModel.dashboard.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val location by viewModel.lastLocation.collectAsStateWithLifecycle()

    var showNotifications by remember { mutableStateOf(false) }
    var showReturnTrip by remember { mutableStateOf(false) }
    var confirmOffline by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.goOnline()
    }
    val online = state.status?.isOnline == true

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Hi, ${profile?.fullName?.substringBefore(' ')?.ifBlank { null } ?: "Driver"}",
                unreadNotifications = notifications.count { !it.isRead },
                onNotifications = { showNotifications = true },
            )
        },
        bottomBar = { DriverBottomNav(Screen.Dashboard, viewModel::navigate) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refreshDashboardNow,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    OnlineCard(
                        online = online,
                        working = state.goingOnline || busy == "Going offline",
                        onToggle = {
                            if (online) confirmOffline = true
                            else locationPermission.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                    )
                }
                if (vehicles.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vehicles, key = { it.id }) { v ->
                                FilterChip(
                                    selected = v.id == selectedVehicleId,
                                    onClick = { viewModel.selectVehicle(v) },
                                    label = { Text("${v.plateNumber}${if (v.isVerified) "" else " · pending"}") },
                                    enabled = v.isVerified && busy == null,
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DriverPrimary.copy(alpha = 0.2f)),
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Today's earnings", Format.money(state.today?.net ?: 0.0), Modifier.weight(1f))
                        StatTile("Trips today", "${state.today?.rides ?: 0}", Modifier.weight(1f))
                    }
                }
                if (online) {
                    item { ReturnTripCard(state.returnOffer, onCreate = { showReturnTrip = true }, onCancel = viewModel::cancelReturnOffer) }
                    item { SectionTitle("Ride requests near you") }
                    if (state.requests.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Default.SearchOff,
                                "Looking for rides",
                                "New requests near you will appear here and as a notification. Stay in a busy area to get more trips.",
                            )
                        }
                    } else {
                        items(state.requests, key = { it.id }) { ride ->
                            RideRequestCard(
                                ride = ride,
                                driverLocation = location,
                                busy = busy != null,
                                accepting = busy == "Accepting",
                                onAccept = { viewModel.acceptRide(ride) },
                                onSkip = { viewModel.skipRide(ride) },
                            )
                        }
                    }
                } else {
                    item {
                        EmptyState(
                            Icons.Default.PowerSettingsNew,
                            "You're offline",
                            "Go online to start receiving ride requests near you.",
                        )
                    }
                }
            }
        }
    }

    if (showNotifications) {
        LaunchedEffect(Unit) { viewModel.loadNotifications() }
        NotificationsDialog(notifications) {
            showNotifications = false
            viewModel.markNotificationsRead()
        }
    }
    if (confirmOffline) {
        AlertDialog(
            onDismissRequest = { confirmOffline = false },
            title = { Text("Go offline?") },
            text = { Text("You will stop receiving ride requests.") },
            confirmButton = { TextButton(onClick = { confirmOffline = false; viewModel.goOffline() }) { Text("Go offline", color = DriverOfflineRed) } },
            dismissButton = { TextButton(onClick = { confirmOffline = false }) { Text("Stay online") } },
        )
    }
    if (showReturnTrip) {
        ReturnTripDialog(busy = busy != null, onDismiss = { showReturnTrip = false }) { place, discount, hours ->
            viewModel.offerReturnTrip(place, discount, hours) { showReturnTrip = false }
        }
    }
    trip.finished?.let { finished ->
        TripSummaryDialog(
            ride = finished,
            busy = busy != null,
            onRate = viewModel::rateCustomer,
            onSkip = viewModel::dismissTripSummary,
        )
    }
}

@Composable
private fun OnlineCard(online: Boolean, working: Boolean, onToggle: () -> Unit) {
    val color = if (online) DriverOnlineGreen else DriverOfflineRed
    SectionCard(onClick = if (working) null else onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                if (working) CircularProgressIndicator(Modifier.size(28.dp), color = color, strokeWidth = 3.dp)
                else Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (online) "You're online" else "You're offline", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        working -> "Please wait…"
                        online -> "Receiving requests. Tap to go offline."
                        else -> "Tap to go online"
                    },
                    color = TextSecondaryDark,
                )
            }
            Icon(Icons.Default.Circle, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Text(label, color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RideRequestCard(
    ride: Ride,
    driverLocation: Location?,
    busy: Boolean,
    accepting: Boolean,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
) {
    val toPickupKm = driverLocation?.let {
        val out = FloatArray(1)
        Location.distanceBetween(it.latitude, it.longitude, ride.pickupLat, ride.pickupLng, out)
        out[0] / 1000.0
    }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Format.money(ride.fare), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DriverPrimary)
            Spacer(Modifier.weight(1f))
            if (ride.isScheduled) StatusChip("Scheduled ${Format.time(ride.scheduledAt)}", Tone.Info)
            else Text(Format.ago(ride.requestedAt), color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            listOfNotNull(
                Format.km(ride.distanceKm) + " trip",
                toPickupKm?.let { Format.km(it) + " to pickup" },
                ride.passengerCount?.let { "$it passenger${if (it > 1) "s" else ""}" },
                ride.goodsWeightKg?.let { "${it.toInt()} kg goods" },
            ).joinToString(" · "),
            color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        AddressLine(DriverOnlineGreen, ride.pickupAddress ?: "Pickup point")
        Spacer(Modifier.height(6.dp))
        AddressLine(DriverOfflineRed, ride.dropAddress ?: "Drop point")
        ride.goodsDescription?.let {
            Spacer(Modifier.height(6.dp))
            Text("Goods: $it", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Skip") }
            PrimaryButton("Accept", onClick = onAccept, enabled = !busy, loading = accepting, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
fun AddressLine(color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReturnTripCard(offer: ReturnTripOffer?, onCreate: () -> Unit, onCancel: () -> Unit) {
    SectionCard(onClick = if (offer == null) onCreate else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = DriverWarningAmber)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (offer == null) {
                    Text("Going back empty?", fontWeight = FontWeight.SemiBold)
                    Text("Offer a discounted return trip to customers on your route.", color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Return trip to ${offer.toAddress ?: "destination"}", fontWeight = FontWeight.SemiBold)
                    Text("${offer.discountPercent.toInt()}% off · until ${Format.time(offer.expiresAt)}", color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (offer != null) TextButton(onClick = onCancel) { Text("Cancel", color = DriverOfflineRed) }
        }
    }
}

@Composable
private fun ReturnTripDialog(busy: Boolean, onDismiss: () -> Unit, onSubmit: (String, Int, Int) -> Unit) {
    var place by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableIntStateOf(30) }
    var hours by rememberSaveable { mutableIntStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offer a return trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(place, { place = it }, "Heading back to", placeholder = "City or area")
                Text("Discount for customers", color = TextSecondaryDark, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 30, 40).forEach { d ->
                        FilterChip(selected = discount == d, onClick = { discount = d }, label = { Text("$d%") })
                    }
                }
                Text("Available for", color = TextSecondaryDark, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4).forEach { h ->
                        FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text("$h h") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(place, discount, hours) }, enabled = !busy) { Text("Post offer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TripSummaryDialog(ride: Ride, busy: Boolean, onRate: (Int, String) -> Unit, onSkip: () -> Unit) {
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var comment by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Trip completed") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Collect / confirm fare", color = TextSecondaryDark)
                Text(Format.money(ride.fare), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = DriverPrimary)
                Text(Format.km(ride.distanceKm), color = TextSecondaryDark)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Rate the customer")
                Row {
                    (1..5).forEach { i ->
                        IconButton(onClick = { stars = i }) {
                            Icon(
                                if (i <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i stars",
                                tint = DriverWarningAmber,
                            )
                        }
                    }
                }
                Field(comment, { comment = it.take(300) }, "Comment (optional)")
            }
        },
        confirmButton = { TextButton(onClick = { onRate(stars, comment) }, enabled = !busy) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } },
    )
}

