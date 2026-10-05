package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EarningsDay
import com.example.data.Ride
import com.example.data.RideStatus
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.components.DriverBottomNav
import com.example.ui.components.DriverTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.Format
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusChip
import com.example.ui.components.Tone
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.DriverWarningAmber
import com.example.ui.theme.TextSecondaryDark
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(viewModel: DriverViewModel) {
    val state by viewModel.earnings.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableIntStateOf(1) } // 0 today, 1 week, 2 month
    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    val from = when (period) {
        0 -> today
        1 -> today.minusDays(6)
        else -> today.minusDays(29)
    }
    val inPeriod = state.days.filter { !it.day.isBefore(from) }

    Scaffold(
        topBar = { DriverTopBar("Earnings") },
        bottomBar = { DriverBottomNav(Screen.Earnings, viewModel::navigate) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Today", "7 days", "30 days").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = period == i,
                            onClick = { period = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                        ) { Text(label) }
                    }
                }
            }
            item {
                SectionCard {
                    Text("Net earnings", color = TextSecondaryDark)
                    Text(Format.money(inPeriod.sumOf { it.net }), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = DriverPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("Trips", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                            Text("${inPeriod.sumOf { it.rides }}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Fares collected", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                            Text(Format.money(inPeriod.sumOf { it.gross }), fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Commission", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                            Text(Format.money(inPeriod.sumOf { it.gross - it.net }), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { WeekChart(state.days, today) }
            state.ratingSummary?.let { summary ->
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = DriverWarningAmber)
                            Spacer(Modifier.width(8.dp))
                            Text(String.format(java.util.Locale.US, "%.1f", summary.average), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("from ${summary.total} ratings", color = TextSecondaryDark)
                        }
                        state.ratings.firstOrNull { !it.comment.isNullOrBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("“${it.comment}”", color = TextSecondaryDark)
                        }
                    }
                }
            }
            item { SectionTitle("Recent trips") }
            if (state.history.isEmpty()) {
                item { EmptyState(Icons.Default.History, "No trips yet", "Completed trips will show up here.") }
            } else {
                items(state.history, key = { it.id }) { TripHistoryRow(it) }
            }
        }
    }
}

@Composable
private fun WeekChart(days: List<EarningsDay>, today: LocalDate) {
    val week = (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        day to (days.firstOrNull { it.day == day }?.net ?: 0.0)
    }
    val max = week.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
    val barColor = DriverPrimary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    SectionCard {
        Text("Last 7 days", color = TextSecondaryDark)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / week.size
            val barWidth = slot * 0.55f
            week.forEachIndexed { i, (_, value) ->
                val h = if (value <= 0) 6f else (value / max).toFloat() * size.height
                drawRoundRect(
                    color = if (value <= 0) emptyColor else barColor,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(8f, 8f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            week.forEach { (day, _) ->
                Text(
                    Format.weekday(day),
                    modifier = Modifier.weight(1f),
                    color = TextSecondaryDark,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun TripHistoryRow(ride: Ride) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.dateTime(ride.completedAt ?: ride.cancelledAt ?: ride.requestedAt), fontWeight = FontWeight.SemiBold)
                Text(ride.dropAddress ?: "—", color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (ride.status == RideStatus.COMPLETED) {
                    Text(Format.money(ride.fare), fontWeight = FontWeight.Bold)
                    Text(Format.km(ride.distanceKm), color = TextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                } else {
                    StatusChip("Cancelled", Tone.Bad)
                }
            }
        }
    }
}
