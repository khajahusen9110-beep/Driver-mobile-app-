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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DocType
import com.example.ui.DriverViewModel
import com.example.ui.components.DriverTopBar
import com.example.ui.components.Field
import com.example.ui.components.Format
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionTitle
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextSecondaryDark

@Composable
fun OnboardingScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val types by viewModel.vehicleTypes.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var name by rememberSaveable(profile?.fullName) {
        mutableStateOf(profile?.fullName?.takeUnless { it == "New User" }.orEmpty())
    }
    var showAddVehicle by rememberSaveable { mutableStateOf(false) }

    val nameDone = profile?.fullName?.let { it.isNotBlank() && it != "New User" } == true
    val vehicleDone = vehicles.isNotEmpty()
    val requiredDocs = DocType.entries.filter { it.required }
    val docsDone = requiredDocs.all { t -> documents.any { it.docType == t.apiValue && !it.isRejected } }
    val stepsDone = listOf(nameDone, vehicleDone, docsDone).count { it }

    Scaffold(
        topBar = {
            DriverTopBar("Become a driver", actions = {
                IconButton(onClick = viewModel::logout) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out") }
            })
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Complete these steps and our team will review your application.", color = TextSecondaryDark)
            LinearProgressIndicator(
                progress = { stepsDone / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = DriverPrimary,
            )

            StepHeader(1, "Your name", nameDone)
            SectionCard {
                Field(name, { name = it }, "Full name", placeholder = "As on your driving licence")
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    if (nameDone) "Update name" else "Save name",
                    onClick = { viewModel.saveName(name) },
                    enabled = busy == null && name.trim() != profile?.fullName,
                )
            }

            StepHeader(2, "Your vehicle", vehicleDone)
            vehicles.forEach { VehicleCard(it) }
            SecondaryButton(if (vehicleDone) "Add another vehicle" else "Add vehicle", onClick = { showAddVehicle = true }, enabled = busy == null)

            StepHeader(3, "Documents", docsDone)
            Text("Clear photos or PDFs, up to 10 MB each.", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
            DocumentList(viewModel, documents, busy = busy != null, warnOnReupload = false)

            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                "Submit for review",
                onClick = viewModel::submitApplication,
                enabled = nameDone && vehicleDone && docsDone,
                loading = busy == "Submitting",
            )
            busy?.let { Text("$it…", color = TextSecondaryDark, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddVehicle) {
        AddVehicleDialog(
            types = types,
            busy = busy != null,
            onSave = { typeId, make, model, plate, seats, kg, ac ->
                viewModel.addVehicle(typeId, make, model, plate, seats, kg, ac) { showAddVehicle = false }
            },
            onDismiss = { showAddVehicle = false },
        )
    }
}

@Composable
private fun StepHeader(number: Int, title: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(
            Modifier.size(28.dp).background(if (done) DriverOnlineGreen else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(if (done) "✓" else "$number", color = if (done) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        SectionTitle(title)
    }
}

@Composable
fun UnderReviewScreen(viewModel: DriverViewModel) {
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    StatusPage(
        icon = Icons.Default.HourglassTop,
        tint = DriverWarningAmber,
        title = "Application under review",
        message = "Our team is checking your documents and vehicle. This usually takes up to 24 hours. We'll notify you as soon as you're approved.",
        primary = "Check status" to viewModel::refreshProfile,
        loading = refreshing,
        onLogout = viewModel::logout,
    )
}

@Composable
fun RejectedScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadOnboarding() }
    val rejectedDocs = documents.filter { it.isRejected }
    StatusPage(
        icon = Icons.Default.Cancel,
        tint = DriverOfflineRed,
        title = "Application not approved",
        message = buildString {
            append(profile?.rejectionReason ?: "Some details need to be corrected.")
            if (rejectedDocs.isNotEmpty()) {
                append("\n\nPlease upload again:\n")
                rejectedDocs.forEach { d ->
                    append("• ${DocType.from(d.docType)?.label ?: d.docType}")
                    d.rejectionReason?.let { append(": $it") }
                    append('\n')
                }
            }
        },
        primary = "Fix and resubmit" to viewModel::resubmitApplication,
        loading = busy != null,
        onLogout = viewModel::logout,
    )
}

@Composable
fun SuspendedScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    StatusPage(
        icon = Icons.Default.Block,
        tint = DriverOfflineRed,
        title = "Account suspended",
        message = buildString {
            append(profile?.suspensionReason ?: "Your account has been suspended.")
            profile?.suspendedUntil?.let { append("\n\nSuspended until ${Format.dateTime(it)}.") }
            append("\n\nContact support if you think this is a mistake.")
        },
        primary = "Check again" to viewModel::refreshProfile,
        loading = refreshing,
        onLogout = viewModel::logout,
    )
}

@Composable
fun BlockedScreen(viewModel: DriverViewModel, message: String) {
    StatusPage(
        icon = Icons.Default.Block,
        tint = DriverOfflineRed,
        title = "Can't use this account",
        message = message,
        primary = "Log out" to viewModel::logout,
        loading = false,
        onLogout = null,
    )
}

@Composable
private fun StatusPage(
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String,
    primary: Pair<String, () -> Unit>,
    loading: Boolean,
    onLogout: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(96.dp).background(tint.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextSecondaryDark, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(primary.first, onClick = primary.second, loading = loading)
        if (onLogout != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLogout) { Text("Log out") }
        }
    }
}
