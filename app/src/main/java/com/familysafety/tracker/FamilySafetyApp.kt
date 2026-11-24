package com.familysafety.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.familysafety.tracker.data.repository.LocationRepository

class FamilySafetyApp : Application() {

    lateinit var repository: LocationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = LocationRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Location Tracking Channel
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                getString(R.string.tracking_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.tracking_notification_text)
                setShowBadge(false)
            }

            // Emergency SOS High Priority Channel
            val sosChannel = NotificationChannel(
                CHANNEL_ID_SOS,
                getString(R.string.sos_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical SOS alert notifications with alarm"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(sosChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_TRACKING = "channel_location_tracking"
        const val CHANNEL_ID_SOS = "channel_sos_emergency"

        lateinit var instance: FamilySafetyApp
            private set
    }
}
