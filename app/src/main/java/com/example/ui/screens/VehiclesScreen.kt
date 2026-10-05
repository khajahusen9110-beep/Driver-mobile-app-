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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun VehiclesScreen(viewModel: DriverViewModel) {
    val vehicles by viewModel.vehicles.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showAddVehicleModal by remember { mutableStateOf(false) }
    var plateNumber by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var hasAc by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "My Fleet & Vehicles",
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
                    text = "Registered Vehicles (${vehicles.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                Button(
                    onClick = { showAddVehicleModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_vehicle_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Vehicle", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vehicles) { veh ->
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
                                    Text(text = veh.icon_emoji ?: "🚗", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${veh.make} ${veh.model}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = veh.plate_number,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DriverPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (veh.is_default) {
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
                                            Text("ACTIVE / DEFAULT", color = DriverOnlineGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.setDefaultVehicle(veh.id) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Set Default", color = TextPrimaryDark, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Features: " + if (veh.has_ac) "AC Available" else "Non-AC",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddVehicleModal) {
        Dialog(onDismissRequest = { showAddVehicleModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Add Another Vehicle",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = plateNumber,
                        onValueChange = { plateNumber = it.uppercase() },
                        label = { Text("Registration Plate") },
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
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make (e.g. Hyundai)") },
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
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model (e.g. Aura)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriverPrimary,
                            unfocusedBorderColor = DriverDarkSurfaceVariant,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddVehicleModal = false }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.submitVehicle(
                                    vehicleTypeId = "vt_sedan",
                                    plateNumber = plateNumber,
                                    make = make,
                                    model = model,
                                    capacityKg = null,
                                    hasAc = hasAc,
                                    onComplete = { showAddVehicleModal = false }
                                )
                            },
                            enabled = plateNumber.isNotBlank() && make.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary)
                        ) {
                            Text("Save Vehicle", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
