package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DocType
import com.example.data.DriverDocument
import com.example.data.Vehicle
import com.example.data.VehicleType
import com.example.ui.DriverViewModel
import com.example.ui.components.Field
import com.example.ui.components.Format
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusChip
import com.example.ui.components.Tone
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextSecondaryDark
import java.time.LocalDate

/** Upload state for one document type, shared by onboarding and the Documents screen. */
@Composable
fun DocumentList(
    viewModel: DriverViewModel,
    documents: List<DriverDocument>,
    busy: Boolean,
    warnOnReupload: Boolean,
) {
    val context = LocalContext.current
    var pendingType by rememberSaveable { mutableStateOf<DocType?>(null) }
    var pendingExpiry by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmType by remember { mutableStateOf<DocType?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val type = pendingType
        if (uri != null && type != null) viewModel.uploadDocument(type, uri, pendingExpiry?.let(LocalDate::parse))
        pendingType = null
        pendingExpiry = null
    }

    fun startUpload(type: DocType) {
        pendingType = type
        if (type.hasExpiry) {
            val today = LocalDate.now()
            DatePickerDialog(context, { _, y, m, d ->
                pendingExpiry = LocalDate.of(y, m + 1, d).toString()
                picker.launch(arrayOf("image/*", "application/pdf"))
            }, today.year + 1, today.monthValue - 1, today.dayOfMonth).apply {
                setTitle("Expiry date on ${type.label.lowercase()}")
                datePicker.minDate = System.currentTimeMillis()
            }.show()
        } else {
            pendingExpiry = null
            picker.launch(arrayOf("image/*", "application/pdf"))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DocType.entries.forEach { type ->
            val doc = documents.firstOrNull { it.docType == type.apiValue }
            DocumentRow(
                type = type,
                document = doc,
                enabled = !busy,
                onUpload = { if (doc != null && warnOnReupload && doc.isApproved) confirmType = type else startUpload(type) },
                onView = {
                    if (doc != null) viewModel.openDocument(doc) { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )
        }
    }

    confirmType?.let { type ->
        AlertDialog(
            onDismissRequest = { confirmType = null },
            title = { Text("Replace ${type.label.lowercase()}?") },
            text = { Text("The new file goes for review again. Until it's approved, it won't count as verified.") },
            confirmButton = { TextButton(onClick = { confirmType = null; startUpload(type) }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { confirmType = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DocumentRow(
    type: DocType,
    document: DriverDocument?,
    enabled: Boolean,
    onUpload: () -> Unit,
    onView: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, tint) = when {
                document == null -> Icons.Default.UploadFile to TextSecondaryDark
                document.isApproved -> Icons.Default.CheckCircle to DriverOnlineGreen
                document.isRejected -> Icons.Default.ErrorOutline to DriverOfflineRed
                else -> Icons.Default.Schedule to DriverWarningAmber
            }
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type.label + if (type.required) "" else " (optional)", fontWeight = FontWeight.SemiBold)
                val subtitle = when {
                    document == null -> if (type.required) "Required" else "Not uploaded"
                    document.isApproved -> "Verified"
                    document.isRejected -> "Rejected: ${document.rejectionReason ?: "please upload again"}"
                    else -> "Under review"
                }
                Text(subtitle, color = if (document?.isRejected == true) DriverOfflineRed else TextSecondaryDark,
                    style = MaterialTheme.typography.bodySmall)
                document?.expiryDate?.let {
                    val expired = !it.isAfter(LocalDate.now())
                    Text(
                        (if (expired) "Expired " else "Valid till ") + Format.date(it),
                        color = if (expired) DriverOfflineRed else TextSecondaryDark,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (document != null) TextButton(onClick = onView, enabled = enabled) { Text("View") }
            TextButton(onClick = onUpload, enabled = enabled) {
                Text(if (document == null) "Upload" else "Replace", color = DriverPrimary)
            }
        }
    }
}

@Composable
fun VehicleCard(vehicle: Vehicle, selected: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (selected) DriverPrimary else TextSecondaryDark)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vehicle.title, fontWeight = FontWeight.SemiBold)
                    if (vehicle.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Star, contentDescription = "Default", tint = DriverWarningAmber, modifier = Modifier.size(16.dp))
                    }
                }
                Text(listOfNotNull(vehicle.plateNumber, vehicle.typeName).joinToString(" · "), color = TextSecondaryDark,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (vehicle.isVerified) StatusChip("Verified", Tone.Good) else StatusChip("Pending", Tone.Warn)
            trailing()
        }
    }
}

@Composable
fun AddVehicleDialog(
    types: List<VehicleType>,
    busy: Boolean,
    onSave: (typeId: String?, make: String, model: String, plate: String, seats: String, kg: String, ac: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var typeId by rememberSaveable { mutableStateOf<String?>(null) }
    var make by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var seats by rememberSaveable { mutableStateOf("4") }
    var kg by rememberSaveable { mutableStateOf("") }
    var ac by rememberSaveable { mutableStateOf(true) }
    val type = types.firstOrNull { it.id == typeId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add vehicle") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vehicle type", color = TextSecondaryDark, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(types, key = { it.id }) { t ->
                        FilterChip(
                            selected = t.id == typeId,
                            onClick = { typeId = t.id },
                            label = { Text(listOfNotNull(t.iconEmoji, t.name).joinToString(" ")) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DriverPrimary.copy(alpha = 0.2f)),
                        )
                    }
                }
                type?.capacityLabel?.let { Text(it, color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall) }
                Field(make, { make = it }, "Make", placeholder = "Maruti Suzuki")
                Field(model, { model = it }, "Model", placeholder = "Dzire")
                Field(plate, { plate = it.uppercase().take(13) }, "Registration number", placeholder = "KA01AB1234")
                if (type?.isGoods == true) {
                    Field(kg, { v -> kg = v.filter { it.isDigit() || it == '.' }.take(6) }, "Load capacity (kg)", keyboardType = KeyboardType.Decimal)
                } else {
                    Field(seats, { v -> seats = v.filter { it.isDigit() }.take(2) }, "Passenger seats", keyboardType = KeyboardType.Number)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ac, onCheckedChange = { ac = it })
                        Text("Air conditioned")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "New vehicles are checked by our team along with the RC and insurance before you can drive them.",
                    color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(typeId, make, model, plate, seats, kg, ac && type?.isGoods != true) }, enabled = !busy) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DocumentIconHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(Icons.Default.Description, contentDescription = null, tint = DriverPrimary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
        }
    }
}
