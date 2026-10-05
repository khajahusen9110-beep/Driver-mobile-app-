package com.example.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.DriverApp
import com.example.MainActivity
import com.example.R
import com.example.data.ApiException
import com.example.data.SessionExpiredException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs while the driver is online: keeps a "You're online" notification and sends the GPS position
 * to the backend so dispatch can match nearby rides and the customer can track the car.
 */
class OnlineLocationService : LifecycleService() {

    private lateinit var fused: FusedLocationProviderClient
    private var lastSentAt = 0L
    private var vehicleId: String? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        vehicleId = intent?.getStringExtra(EXTRA_VEHICLE_ID) ?: (application as DriverApp).session.selectedVehicleId
        if (!LocationProvider.hasPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        startUpdates()
        _running.value = true
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .build()
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun onLocation(location: Location) {
        LocationProvider.publish(location)
        val now = System.currentTimeMillis()
        if (now - lastSentAt < MIN_INTERVAL_MS) return
        lastSentAt = now
        val repository = (application as DriverApp).repository
        lifecycleScope.launch {
            try {
                repository.updateStatus(true, location.latitude, location.longitude, vehicleId)
            } catch (e: SessionExpiredException) {
                stopSelf()
            } catch (e: ApiException) {
                // A rule now blocks going online (suspended, vehicle unverified). Stop sharing location.
                if (e.httpCode in 400..499) {
                    Log.w(TAG, "Backend refused location update: ${e.message}")
                    _stoppedReason.value = e.message
                    stopSelf()
                }
            }
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, DriverApp.CHANNEL_ONLINE)
        .setSmallIcon(R.drawable.ic_stat_driver)
        .setContentTitle(getString(R.string.online_notification_title))
        .setContentText(getString(R.string.online_notification_text))
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        )
        .build()

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        _running.value = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OnlineLocation"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 10_000L
        private const val MIN_INTERVAL_MS = 8_000L
        private const val MIN_DISTANCE_M = 10f
        private const val ACTION_STOP = "com.example.location.STOP"
        private const val EXTRA_VEHICLE_ID = "vehicle_id"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        private val _stoppedReason = MutableStateFlow<String?>(null)

        /** Set when the backend refused a location update and the service stopped itself. */
        val stoppedReason: StateFlow<String?> = _stoppedReason

        fun clearStoppedReason() { _stoppedReason.value = null }

        fun start(context: Context, vehicleId: String?) {
            _stoppedReason.value = null
            val intent = Intent(context, OnlineLocationService::class.java).putExtra(EXTRA_VEHICLE_ID, vehicleId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OnlineLocationService::class.java))
        }
    }
}
