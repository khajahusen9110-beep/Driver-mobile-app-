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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DriverViewModel
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun UnderReviewScreen(viewModel: DriverViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DriverWarningAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = DriverWarningAmber,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Application Under Review",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Thank you for submitting your driver onboarding details. Our compliance team is verifying your Driving License, RC, and Insurance. You'll receive a notification as soon as you're approved!",
                textAlign = TextAlign.Center,
                color = TextSecondaryDark,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.loginDemo() // or refresh
                },
                colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("review_screen_refresh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Application Status", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SuspendedScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsState()
    val reason = profile?.suspension_reason ?: "3+ trip cancellations within 24 hours"
    val until = profile?.suspended_until ?: "24 hours from incident"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DriverOfflineRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = DriverOfflineRed,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Account Suspended",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DriverOfflineRed
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Suspension Details", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Reason: $reason", color = TextSecondaryDark, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Suspended Until: $until", color = DriverWarningAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "During suspension, going online or accepting trip requests is blocked. Contact support if you believe this was an error.",
                textAlign = TextAlign.Center,
                color = TextSecondaryDark,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Log Out", color = TextPrimaryDark)
            }
        }
    }
}

@Composable
fun RejectedScreen(viewModel: DriverViewModel) {
    val profile by viewModel.profile.collectAsState()
    val reason = profile?.rejection_reason ?: "Document photo was blurry or expired. Please upload clear photos."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DriverOfflineRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DriverOfflineRed,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Application Rejected",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DriverOfflineRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Rejection Reason", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = reason, color = TextSecondaryDark, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.resubmitApplication() },
                colors = ButtonDefaults.buttonColors(containerColor = DriverPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resubmit_application_button")
            ) {
                Text("Resubmit Application", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
