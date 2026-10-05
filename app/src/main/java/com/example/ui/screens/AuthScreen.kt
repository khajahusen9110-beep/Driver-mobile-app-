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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DriverViewModel
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSecondary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AuthScreen(viewModel: DriverViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var phone by remember { mutableStateOf("+91 98765 00001") }
    var fullName by remember { mutableStateOf("Arjun Kumar") }
    var otpToken by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero Icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(DriverPrimary, DriverSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Driver Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rider Partner",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            )

            Text(
                text = "Drive, Deliver & Earn on Your Terms",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondaryDark
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error Banner
            ErrorBanner(
                message = errorBanner,
                onDismiss = { viewModel.clearError() }
            )

            // Auth Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (!otpSent) {
                        Text(
                            text = "Driver Login / Sign Up",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        )
                        Text(
                            text = "We'll send an SMS OTP for authentication",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = DriverPrimary)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number (with country code)") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = DriverPrimary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (phone.isNotBlank()) {
                                    viewModel.sendOtp(phone.trim(), fullName.trim())
                                }
                            },
                            enabled = !isLoading && phone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_send_otp_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Send OTP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        // OTP Input State
                        Text(
                            text = "Verify OTP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        )
                        Text(
                            text = "Enter 6-digit code sent to $phone",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = otpToken,
                            onValueChange = { if (it.length <= 6) otpToken = it },
                            label = { Text("6-Digit OTP") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = DriverPrimary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DriverPrimary,
                                unfocusedBorderColor = DriverDarkSurfaceVariant,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_otp_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (otpToken.length >= 4) {
                                    viewModel.verifyOtp(phone.trim(), otpToken.trim())
                                }
                            },
                            enabled = !isLoading && otpToken.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_verify_otp_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Verify & Continue",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.sendOtp(phone.trim(), fullName.trim())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Resend OTP", color = DriverSecondary, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = DriverDarkSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instant Test / Demo Driver Account Button
                    OutlinedButton(
                        onClick = { viewModel.loginDemo() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DriverPrimary.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_demo_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DriverPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instant Demo Driver Login",
                            color = DriverPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = DriverDarkSurface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted Supabase Auth & Row Level Security",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
