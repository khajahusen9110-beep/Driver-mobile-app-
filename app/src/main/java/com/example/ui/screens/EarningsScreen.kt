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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.theme.DriverDarkSurface
import com.example.ui.theme.DriverDarkSurfaceVariant
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverSecondary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun EarningsScreen(viewModel: DriverViewModel) {
    val dailyEarnings by viewModel.dailyEarnings.collectAsState()

    val totalGross = dailyEarnings.sumOf { it.gross_earnings }
    val totalNet = dailyEarnings.sumOf { it.net_earnings }
    val totalCommission = dailyEarnings.sumOf { it.commission_deducted }
    val totalRides = dailyEarnings.sumOf { it.rides_count }

    Scaffold(
        topBar = {
            DriverTopBar(
                title = "Driver Earnings",
                onNotificationClick = null
            )
        },
        bottomBar = {
            DriverBottomNav(
                currentScreen = Screen.Earnings,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Net Earnings Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("earnings_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Net Payout (This Week)",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            color = DriverOnlineGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = DriverOnlineGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("82% Take-home", color = DriverOnlineGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "₹${String.format("%.2f", totalNet)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = DriverOnlineGreen,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DriverDarkSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gross Bookings", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "₹${String.format("%.0f", totalGross)}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Platform Fee (18%)", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "-₹${String.format("%.0f", totalCommission)}",
                                fontWeight = FontWeight.Bold,
                                color = DriverWarningAmber,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Total Trips", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(
                                text = "$totalRides rides",
                                fontWeight = FontWeight.Bold,
                                color = DriverSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Daily Breakdown (driver_earnings_daily)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dailyEarnings) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DriverDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = DriverPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = DriverPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.day,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${item.rides_count} rides completed",
                                        color = TextSecondaryDark,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format("%.1f", item.net_earnings)}",
                                    fontWeight = FontWeight.Bold,
                                    color = DriverOnlineGreen,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Gross: ₹${String.format("%.0f", item.gross_earnings)}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
