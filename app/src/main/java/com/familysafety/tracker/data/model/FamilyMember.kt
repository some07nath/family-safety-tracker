package com.familysafety.tracker.data.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val activityType: ActivityType = ActivityType.STILL
)

data class FamilyMember(
    val id: String,
    val name: String,
    val relationship: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speedKmh: Float,
    val bearing: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val activityType: ActivityType,
    val lastUpdated: Long,
    val isOnline: Boolean,
    val isCurrentUser: Boolean = false
)
