package com.example.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.DriverApp
import com.example.R
import com.example.ui.IncomingRideActivity
import java.util.Collections

/**
 * The "new order" alert: a full-screen, call-style notification that rings (or only vibrates, per the
 * driver's setting) for up to [ALERT_MS] while the driver decides.
 */
object RideAlert {
    const val ALERT_MS = 60_000L
    const val EXTRA_RIDE_ID = "ride_id"
    const val EXTRA_ALERTED_AT = "alerted_at"

    private const val CHANNEL_RING = "new_order_ring"
    private const val CHANNEL_VIBRATE = "new_order_vibrate"
    private const val TAG = "new_order"
    private val VIBRATION = longArrayOf(0, 800, 600, 800, 600)

    private val alerted = Collections.synchronizedSet(mutableSetOf<String>())

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val ring = NotificationChannel(CHANNEL_RING, context.getString(R.string.channel_new_order_ring), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_new_order_desc)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(false)
        }
        val vibrate = NotificationChannel(CHANNEL_VIBRATE, context.getString(R.string.channel_new_order_vibrate), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_new_order_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(listOf(ring, vibrate))
    }

    /** Shows the alert once per ride. Safe to call from push, realtime and polling for the same ride. */
    @SuppressLint("MissingPermission")
    fun show(context: Context, rideId: String, title: String, body: String) {
        val app = context.applicationContext as DriverApp
        if (!app.session.isLoggedIn || !alerted.add(rideId)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val now = System.currentTimeMillis()
        val intent = Intent(context, IncomingRideActivity::class.java)
            .putExtra(EXTRA_RIDE_ID, rideId)
            .putExtra(EXTRA_ALERTED_AT, now)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val pending = PendingIntent.getActivity(
            context, rideId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val ring = app.session.rideAlertSound
        val notification = NotificationCompat.Builder(context, if (ring) CHANNEL_RING else CHANNEL_VIBRATE)
            .setSmallIcon(R.drawable.ic_stat_driver)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setTimeoutAfter(ALERT_MS)
            .setVibrate(VIBRATION)
            .apply { if (ring) setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }
            .build()
        // Repeat the sound and vibration until the driver responds or the alert times out.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(TAG, rideId.hashCode(), notification)
    }

    fun cancel(context: Context, rideId: String) {
        NotificationManagerCompat.from(context).cancel(TAG, rideId.hashCode())
    }

    /** Android 14+ lets the driver turn off full-screen alerts; then the alert falls back to a heads-up banner. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
}
