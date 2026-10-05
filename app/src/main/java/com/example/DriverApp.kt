package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.DriverRepository
import com.example.data.RealtimeClient
import com.example.data.SessionStore
import com.example.data.SupabaseClient
import com.example.push.RideAlert

/** Builds the shared app objects once, for the activity, the location service and the push service. */
class DriverApp : Application() {

    lateinit var session: SessionStore
        private set
    lateinit var repository: DriverRepository
        private set
    lateinit var realtime: RealtimeClient
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionStore(this)
        val client = SupabaseClient(session)
        repository = DriverRepository(client, session)
        realtime = RealtimeClient(client.http) { client.validAccessToken() }
        createNotificationChannels()
        RideAlert.createChannels(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RIDES, getString(R.string.channel_rides), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = getString(R.string.channel_rides_desc)
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_GENERAL, getString(R.string.channel_general), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_ONLINE, getString(R.string.channel_online), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.channel_online_desc)
                    setShowBadge(false)
                },
            )
        )
    }

    companion object {
        const val CHANNEL_RIDES = "ride_requests"
        const val CHANNEL_GENERAL = "general"
        const val CHANNEL_ONLINE = "online_status"
    }
}
