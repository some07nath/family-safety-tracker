package com.familysafety.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_location_points")
data class OfflineLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val activityType: String,
    val timestamp: Long,
    val isSynced: Boolean = false
)
