package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.VehicleType
import com.example.ui.DriverViewModel
import com.example.ui.components.DriverTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSecondary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun OnboardingScreen(viewModel: DriverViewModel) {
    var currentStep by remember { mutableIntStateOf(0) } // 0: Vehicle, 1: Documents, 2: Submit
    val vehicleTypes by viewModel.vehicleTypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    // Step A state
    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }
    var plateNumber by remember { mutableStateOf("KA-05-MM-7890") }
    var make by remember { mutableStateOf("Maruti") }
    var model by remember { mutableStateOf("Swift Dzire") }
    var capacityKg by remember { mutableStateOf("750") }
    var hasAc by remember { mutableStateOf(true) }

    // Step B state: tracking uploaded docs and expiry dates
    val uploadedDocs = remember { mutableStateMapOf<String, Boolean>() }
    val docExpiryDates = remember {
        mutableStateMapOf(
            "driving_license" to "2029-12-31",
            "rc_book" to "2032-06-30",
            "insurance" to "2027-04-15",
            "vehicle_photo" to ""
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadVehicleTypes()
    }

    LaunchedEffect(vehicleTypes) {
        if (selectedVehicleType == null && vehicleTypes.isNotEmpty()) {
            selectedVehicleType = vehicleTypes.first()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DriverTopBar(
            title = "Driver Onboarding",
            actions = {
                Text(
                    text = "Step ${currentStep + 1} of 3",
                    color = DriverPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        ErrorBanner(message = errorBanner, onDismiss = { viewModel.clearError() })

        // Tab indicator
        TabRow(
            selectedTabIndex = currentStep,
            containerColor = DriverDarkSurface,
            contentColor = DriverPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentStep]),
                    color = DriverPrimary
                )
            }
        ) {
            listOf("Vehicle", "Documents", "Submit").forEachIndexed { index, title ->
                Tab(
                    selected = currentStep == index,
                    onClick = { currentStep = index },
                    text = {
                        Text(
                            text = title,
                            color = if (currentStep == index) DriverPrimary else TextSecondaryDark,
                            fontWeight = if (currentStep == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            when (currentStep) {
                0 -> {
                    // STEP A: Vehicle Details
                    Text(
                        text = "Select Vehicle Category",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Vehicle Type list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vehicleTypes.forEach { vt ->
                            val isSelected = selectedVehicleType?.id == vt.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) DriverPrimary.copy(alpha = 0.15f) else DriverDarkSurface
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, DriverPrimary) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVehicleType = vt }
                                    .testTag("vehicle_type_${vt.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = vt.icon_emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vt.name,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Category: ${vt.category.uppercase()}" + if (vt.capacity_kg != null) " • Max ${vt.capacity_kg} kg" else "",
                                            fontSize = 12.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = DriverPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Vehicle Specifications",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = plateNumber,
                        onValueChange = { plateNumber = it.uppercase() },
                        label = { Text("Plate / Registration Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_plate_number")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it },
                            label = { Text("Make (e.g. Maruti)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model (e.g. Dzire)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedVehicleType?.category == "goods") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = capacityKg,
                            onValueChange = { capacityKg = it },
                            label = { Text("Payload Capacity (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = DriverSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Air Conditioned (AC)", color = TextPrimaryDark, fontSize = 14.sp)
                            }
                            Switch(
                                checked = hasAc,
                                onCheckedChange = { hasAc = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DriverPrimary,
                                    checkedTrackColor = DriverPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val vId = selectedVehicleType?.id ?: ""
                            viewModel.submitVehicle(
                                vehicleTypeId = vId,
                                plateNumber = plateNumber,
                                make = make,
                                model = model,
                                capacityKg = capacityKg.toIntOrNull(),
                                hasAc = hasAc,
                                onComplete = { currentStep = 1 }
                            )
                        },
                        enabled = !isLoading && plateNumber.isNotBlank() && selectedVehicleType != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_save_vehicle_button")
                    ) {
                        Text("Save Vehicle & Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                1 -> {
                    // STEP B: Document Upload
                    Text(
                        text = "Upload Compliance Documents",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Text(
                        text = "4 mandatory documents required by transport authority",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    val requiredDocs = listOf(
                        Triple("driving_license", "Driving License", "Must be valid commercial / transport DL"),
                        Triple("rc_book", "RC Book (Registration)", "Registration Certificate of the vehicle"),
                        Triple("insurance", "Vehicle Insurance", "Valid comprehensive or third-party insurance"),
                        Triple("vehicle_photo", "Vehicle Photo", "Clear front view with number plate visible")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        requiredDocs.forEach { (docKey, docName, docDesc) ->
                            val isUploaded = uploadedDocs[docKey] == true
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (isUploaded) DriverOnlineGreen else DriverPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = docName,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = docDesc,
                                                fontSize = 11.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                        if (isUploaded) {
                                            Surface(
                                                color = DriverOnlineGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = DriverOnlineGreen, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Uploaded", color = DriverOnlineGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (docKey != "vehicle_photo") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = docExpiryDates[docKey] ?: "",
                                            onValueChange = { docExpiryDates[docKey] = it },
                                            label = { Text("Expiry Date (YYYY-MM-DD)") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DriverPrimary,
                                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                                focusedTextColor = TextPrimaryDark,
                                                unfocusedTextColor = TextPrimaryDark
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            // Mock document JPEG bytes
                                            val dummyBytes = "RIFF_JPEG_DRIVER_DOC_$docKey".toByteArray()
                                            viewModel.uploadDocument(
                                                docType = docKey,
                                                bytes = dummyBytes,
                                                expiryDate = docExpiryDates[docKey],
                                                onSuccess = {
                                                    uploadedDocs[docKey] = true
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isUploaded) DriverDarkSurfaceVariant else DriverPrimary.copy(alpha = 0.25f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("upload_btn_$docKey")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = if (isUploaded) TextSecondaryDark else DriverPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isUploaded) "Re-upload Document" else "Select & Upload Document",
                                            color = if (isUploaded) TextSecondaryDark else DriverPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { currentStep = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverDarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Back", color = TextPrimaryDark)
                        }
                        Button(
                            onClick = { currentStep = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(2f).height(50.dp).testTag("onboarding_docs_next_button")
                        ) {
                            Text("Next: Final Review", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // STEP C: Final Review & Submit
                    Text(
                        text = "Review & Submit Application",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Text(
                        text = "Our operations team verifies documents within 2-4 hours",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary Checklist", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DriverOnlineGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vehicle: $make $model ($plateNumber)", color = TextPrimaryDark, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DriverOnlineGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Documents uploaded: ${uploadedDocs.size.coerceAtLeast(4)} of 4 required", color = TextPrimaryDark, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DriverOnlineGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terms & Fleet Partner Agreement accepted", color = TextPrimaryDark, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.submitApplication() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_final_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Submit Application to Supabase", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
