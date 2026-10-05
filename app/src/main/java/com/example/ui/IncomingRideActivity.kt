package com.example.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DriverApp
import com.example.MainActivity
import com.example.data.ApiException
import com.example.data.Ride
import com.example.data.RideStatus
import com.example.location.LocationProvider
import com.example.push.RideAlert
import com.example.ui.components.Format
import com.example.ui.components.PrimaryButton
import com.example.ui.screens.AddressLine
import com.example.ui.theme.DriverOfflineRed
import com.example.ui.theme.DriverOnlineGreen
import com.example.ui.theme.DriverPrimary
import com.example.ui.theme.RiderDriverTheme
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Call-style screen shown over the lock screen when a new order arrives. */
class IncomingRideActivity : ComponentActivity() {

    private var rideId by mutableStateOf<String?>(null)
    private var alertedAt by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            RiderDriverTheme {
                val id = rideId
                if (id != null) {
                    IncomingRide(id, alertedAt, onDone = ::finishAndStop, onAccepted = ::openTrip)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent) {
        val id = intent.getStringExtra(RideAlert.EXTRA_RIDE_ID)
        if (id == null) {
            finish()
            return
        }
        rideId = id
        alertedAt = intent.getLongExtra(RideAlert.EXTRA_ALERTED_AT, System.currentTimeMillis())
    }

    private fun finishAndStop() {
        rideId?.let { RideAlert.cancel(this, it) }
        finish()
    }

    private fun openTrip() {
        rideId?.let { RideAlert.cancel(this, it) }
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}

@Composable
private fun IncomingRide(rideId: String, alertedAt: Long, onDone: () -> Unit, onAccepted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = (context.applicationContext as DriverApp).repository
    val scope = rememberCoroutineScope()
    var ride by remember(rideId) { mutableStateOf<Ride?>(null) }
    var message by remember(rideId) { mutableStateOf<String?>(null) }
    var working by remember(rideId) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val secondsLeft = ((alertedAt + RideAlert.ALERT_MS - now) / 1000).coerceAtLeast(0)

    LaunchedEffect(rideId) {
        val loaded = runCatching { repo.ride(rideId) }.getOrNull()
        if (loaded == null || loaded.status != RideStatus.REQUESTED) {
            message = "This order is no longer available."
            delay(2_000)
            onDone()
        } else {
            ride = loaded
        }
    }
    LaunchedEffect(rideId, alertedAt) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= alertedAt + RideAlert.ALERT_MS) {
                onDone()
                break
            }
            delay(500)
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text("New order", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { secondsLeft / (RideAlert.ALERT_MS / 1000f) },
                    modifier = Modifier.size(96.dp),
                    color = DriverPrimary,
                    strokeWidth = 6.dp,
                )
                Text("$secondsLeft", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }

        val r = ride
        Column(Modifier.fillMaxWidth()) {
            when {
                message != null -> Text(message!!, color = TextSecondaryDark, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                r == null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = DriverPrimary)
                else -> {
                    Text(Format.money(r.fare), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = DriverPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    val toPickup = LocationProvider.lastLocation.value?.let {
                        val out = FloatArray(1)
                        android.location.Location.distanceBetween(it.latitude, it.longitude, r.pickupLat, r.pickupLng, out)
                        out[0] / 1000.0
                    }
                    Text(
                        listOfNotNull(Format.km(r.distanceKm) + " trip", toPickup?.let { Format.km(it) + " to pickup" }).joinToString(" · "),
                        color = TextSecondaryDark, modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(20.dp))
                    AddressLine(DriverOnlineGreen, r.pickupAddress ?: "Pickup point")
                    Spacer(Modifier.height(8.dp))
                    AddressLine(DriverOfflineRed, r.dropAddress ?: "Drop point")
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    RideAlert.cancel(context, rideId)
                    scope.launch {
                        runCatching { repo.rejectRide(rideId, "Skipped by driver") }
                        onDone()
                    }
                },
                enabled = !working,
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("Skip") }
            PrimaryButton(
                "Accept",
                onClick = {
                    working = true
                    RideAlert.cancel(context, rideId)
                    scope.launch {
                        try {
                            repo.acceptRide(rideId)
                            onAccepted()
                        } catch (e: ApiException) {
                            message = e.message
                            ride = null
                            delay(2_500)
                            onDone()
                        } finally {
                            working = false
                        }
                    }
                },
                enabled = ride != null && message == null,
                loading = working,
                modifier = Modifier.weight(2f),
            )
        }
    }
}
