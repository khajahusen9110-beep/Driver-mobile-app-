package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.Ride
import com.example.data.RideStatus
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.Field
import com.example.ui.components.Format
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusChip
import com.example.ui.components.Tone
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ProfileScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val earnings by viewModel.earnings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { DriverTopBar("Profile") },
        bottomBar = { DriverBottomNav(Screen.Profile, viewModel::navigate) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).background(DriverPrimary, CircleShape), contentAlignment = Alignment.Center) {
                            Text(
                                profile?.fullName?.firstOrNull()?.uppercase() ?: "D",
                                color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile?.fullName ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(profile?.phone ?: "", color = TextSecondaryDark)
                            earnings.ratingSummary?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = DriverWarningAmber, modifier = Modifier.size(16.dp))
                                    Text(String.format(java.util.Locale.US, " %.1f · %d ratings", it.average, it.total), color = TextSecondaryDark)
                                }
                            }
                        }
                        IconButton(onClick = { editName = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit name") }
                    }
                }
            }
            profile?.referralCode?.let { code ->
                item {
                    SectionCard(onClick = {
                        val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "Join GoRide with my referral code $code")
                        context.startActivity(Intent.createChooser(share, "Share referral code"))
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Referral code", color = TextSecondaryDark)
                                Text(code, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            }
            item {
                SectionCard {
                    MenuRow(Icons.Default.DirectionsCar, "My vehicles") { viewModel.navigate(Screen.Vehicles) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MenuRow(Icons.Default.Description, "Documents") { viewModel.navigate(Screen.Documents) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MenuRow(Icons.Default.EmojiEvents, "Incentives") { viewModel.navigate(Screen.Incentives) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MenuRow(Icons.Default.ReportProblem, "Help & complaints") { viewModel.navigate(Screen.Complaints) }
                }
            }
            item {
                SectionCard {
                    MenuRow(Icons.AutoMirrored.Filled.Logout, "Log out") { confirmLogout = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MenuRow(Icons.Default.DeleteForever, "Delete account", tint = DriverOfflineRed) { showDelete = true }
                }
            }
            item {
                Text("Version ${BuildConfig.VERSION_NAME}", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp))
            }
        }
    }

    if (editName) {
        var name by rememberSaveable { mutableStateOf(profile?.fullName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Your name") },
            text = { Field(name, { name = it }, "Full name") },
            confirmButton = {
                TextButton(onClick = { viewModel.updateName(name) { editName = false } }, enabled = busy == null) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } },
        )
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Log out?",
            message = "You will go offline and stop receiving ride requests.",
            confirmText = "Log out",
            onConfirm = viewModel::logout,
            onDismiss = { confirmLogout = false },
        )
    }
    if (showDelete) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Withdraw your wallet balance first. After review, your account and personal data will be removed.")
                    Field(reason, { reason = it.take(300) }, "Reason (optional)", singleLine = false, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.requestAccountDeletion(reason) { showDelete = false } }, enabled = busy == null) {
                    Text("Request deletion", color = DriverOfflineRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(title, color = tint, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondaryDark)
    }
}

// ---------- Vehicles ----------

@Composable
fun VehiclesScreen(viewModel: DriverViewModel) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val types by viewModel.vehicleTypes.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { DriverTopBar("My vehicles", onBack = { viewModel.navigate(Screen.Profile) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add vehicle") },
                containerColor = DriverPrimary,
                contentColor = Color.Black,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vehicles.isEmpty()) {
                item { EmptyState(Icons.Default.DirectionsCar, "No vehicles", "Add the vehicle you drive. It is verified with its RC and insurance.") }
            }
            items(vehicles, key = { it.id }) { v ->
                VehicleCard(v, selected = v.isDefault, trailing = {
                    if (!v.isDefault) TextButton(onClick = { viewModel.setDefaultVehicle(v) }, enabled = busy == null) { Text("Make default") }
                })
            }
            item {
                Text(
                    "Only verified vehicles can be used to go online. Changing a vehicle's type or number sends it for verification again.",
                    color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (showAdd) {
        AddVehicleDialog(
            types = types,
            busy = busy != null,
            onSave = { typeId, make, model, plate, seats, kg, ac ->
                viewModel.addVehicle(typeId, make, model, plate, seats, kg, ac) { showAdd = false }
            },
            onDismiss = { showAdd = false },
        )
    }
}

// ---------- Documents ----------

@Composable
fun DocumentsScreen(viewModel: DriverViewModel) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    Scaffold(topBar = { DriverTopBar("Documents", onBack = { viewModel.navigate(Screen.Profile) }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                DocumentIconHeader("Keep your documents up to date", "Expired documents can stop you from going online.")
                Spacer(Modifier.height(8.dp))
                DocumentList(viewModel, documents, busy = busy != null, warnOnReupload = true)
                busy?.let {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.padding(horizontal = 4.dp), color = DriverPrimary)
                    Text("$it…", color = TextSecondaryDark)
                }
            }
        }
    }
}

// ---------- Incentives ----------

@Composable
fun IncentivesScreen(viewModel: DriverViewModel) {
    val incentives by viewModel.incentives.collectAsStateWithLifecycle()
    Scaffold(topBar = { DriverTopBar("Incentives", onBack = { viewModel.navigate(Screen.Profile) }) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (incentives.isEmpty()) {
                item { EmptyState(Icons.Default.EmojiEvents, "No active incentives", "New bonus programs will appear here.") }
            }
            items(incentives, key = { it.id }) { inc ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(inc.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (inc.achieved) StatusChip("Achieved", Tone.Good) else Text(Format.money(inc.rewardAmount), color = DriverPrimary, fontWeight = FontWeight.Bold)
                    }
                    inc.description?.let { Text(it, color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { inc.progress }, modifier = Modifier.padding(vertical = 2.dp), color = DriverPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("${inc.completedRides} of ${inc.targetRides} trips · ends ${Format.date(inc.validUntil)}",
                        color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ---------- Complaints ----------

private val complaintCategories = listOf(
    "payment" to "Payment or fare",
    "customer_behaviour" to "Customer behaviour",
    "safety" to "Safety",
    "app_issue" to "App problem",
    "other" to "Other",
)

@Composable
fun ComplaintsScreen(viewModel: DriverViewModel) {
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val earnings by viewModel.earnings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var showNew by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { DriverTopBar("Help & complaints", onBack = { viewModel.navigate(Screen.Profile) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New complaint") },
                containerColor = DriverPrimary,
                contentColor = Color.Black,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (complaints.isEmpty()) {
                item { EmptyState(Icons.Default.ReportProblem, "No complaints", "If something went wrong on a trip, tell us here.") }
            }
            items(complaints, key = { it.id }) { c ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(complaintCategories.firstOrNull { it.first == c.category }?.second ?: Format.title(c.category),
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        val tone = when (c.status) {
                            "resolved", "closed" -> Tone.Good
                            "in_progress" -> Tone.Info
                            else -> Tone.Warn
                        }
                        StatusChip(Format.title(c.status), tone)
                    }
                    Text(c.description, color = TextSecondaryDark)
                    Text(Format.dateTime(c.createdAt), color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                    c.adminResponse?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Support: $it", color = DriverPrimary)
                    }
                }
            }
        }
    }
    if (showNew) {
        NewComplaintDialog(
            trips = earnings.history.take(10),
            busy = busy != null,
            onDismiss = { showNew = false },
        ) { ride, category, text ->
            viewModel.raiseComplaint(ride, category, text) { showNew = false }
        }
    }
}

@Composable
private fun NewComplaintDialog(
    trips: List<Ride>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Ride?, String, String) -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(complaintCategories.first().first) }
    var rideId by rememberSaveable { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New complaint") },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                item { Text("Category", color = TextSecondaryDark, style = MaterialTheme.typography.labelMedium) }
                items(complaintCategories) { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = category == key, onClick = { category = key })
                        Text(label)
                    }
                }
                if (trips.isNotEmpty()) {
                    item { Text("Related trip (optional)", color = TextSecondaryDark, style = MaterialTheme.typography.labelMedium) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = rideId == null, onClick = { rideId = null })
                            Text("Not about a trip")
                        }
                    }
                    items(trips, key = { it.id }) { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = rideId == r.id, onClick = { rideId = r.id })
                            Column {
                                Text(Format.dateTime(r.completedAt ?: r.requestedAt))
                                Text(
                                    (if (r.status == RideStatus.COMPLETED) Format.money(r.fare) + " · " else "Cancelled · ") + (r.dropAddress ?: ""),
                                    color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Field(text, { text = it.take(1000) }, "What happened?", singleLine = false, minLines = 3)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(trips.firstOrNull { it.id == rideId }, category, text) }, enabled = !busy) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
