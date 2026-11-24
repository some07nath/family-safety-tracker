package com.familysafety.tracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.familysafety.tracker.FamilySafetyApp
import com.familysafety.tracker.R
import com.familysafety.tracker.data.model.ActivityType
import com.familysafety.tracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    private var currentActivity: ActivityType = ActivityType.STILL
    private var lastKnownBatteryLevel: Int = 100
    private var lastKnownIsCharging: Boolean = false
    private var lastLocation: Location? = null

    private var isTracking = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                lastKnownBatteryLevel = (level * 100) / scale
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            lastKnownIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                  status == BatteryManager.BATTERY_STATUS_FULL

            updateForegroundNotification()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            lastLocation = location

            serviceScope.launch {
                FamilySafetyApp.instance.repository.recordUserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    batteryLevel = lastKnownBatteryLevel,
                    isCharging = lastKnownIsCharging,
                    activityType = currentActivity
                )
            }

            updateForegroundNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        activityRecognitionClient = ActivityRecognition.getClient(this)

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_UPDATE_ACTIVITY -> {
                val activityName = intent.getStringExtra(EXTRA_ACTIVITY_TYPE)
                if (activityName != null) {
                    val newActivity = try {
                        ActivityType.valueOf(activityName)
                    } catch (e: Exception) {
                        ActivityType.UNKNOWN
                    }
                    if (newActivity != currentActivity) {
                        currentActivity = newActivity
                        restartLocationUpdatesWithDynamicInterval()
                        updateForegroundNotification()
                    }
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        requestLocationUpdates()
        requestActivityRecognitionUpdates()
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        removeActivityRecognitionUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = createAdaptiveLocationRequest(currentActivity)
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun restartLocationUpdatesWithDynamicInterval() {
        if (!isTracking) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        requestLocationUpdates()
    }

    private fun createAdaptiveLocationRequest(activity: ActivityType): LocationRequest {
        return when (activity) {
            ActivityType.IN_VEHICLE -> {
                // Driving: Higher accuracy & fast interval (5 sec)
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                    .setMinUpdateDistanceMeters(10f)
                    .setMinUpdateIntervalMillis(3_000L)
                    .build()
            }
            ActivityType.WALKING, ActivityType.RUNNING, ActivityType.ON_BICYCLE -> {
                // Moving moderately: Balanced accuracy (15 sec)
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000L)
                    .setMinUpdateDistanceMeters(5f)
                    .setMinUpdateIntervalMillis(8_000L)
                    .build()
            }
            else -> {
                // Still / Stationary: Battery saving (60 sec)
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
                    .setMinUpdateDistanceMeters(20f)
                    .setMinUpdateIntervalMillis(30_000L)
                    .build()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestActivityRecognitionUpdates() {
        val intent = Intent(this, ActivityTransitionReceiver::class.java).apply {
            action = "com.familysafety.tracker.ACTION_ACTIVITY_UPDATE"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 101, intent, flags)
        activityRecognitionClient.requestActivityUpdates(15_000L, pendingIntent)
    }

    private fun removeActivityRecognitionUpdates() {
        val intent = Intent(this, ActivityTransitionReceiver::class.java).apply {
            action = "com.familysafety.tracker.ACTION_ACTIVITY_UPDATE"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 101, intent, flags)
        if (pendingIntent != null) {
            activityRecognitionClient.removeActivityUpdates(pendingIntent)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flags)

        val chargingSymbol = if (lastKnownIsCharging) "⚡ " else ""
        val subtitle = "Battery: $chargingSymbol$lastKnownBatteryLevel% • Activity: ${currentActivity.displayName}"

        return NotificationCompat.Builder(this, FamilySafetyApp.CHANNEL_ID_TRACKING)
            .setContentTitle("Family Safety Active")
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_stat_location)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification() {
        if (!isTracking) return
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        stopTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_TRACKING = "com.familysafety.tracker.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.familysafety.tracker.STOP_TRACKING"
        const val ACTION_UPDATE_ACTIVITY = "com.familysafety.tracker.UPDATE_ACTIVITY"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
