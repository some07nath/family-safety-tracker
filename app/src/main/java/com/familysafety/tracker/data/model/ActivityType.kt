package com.familysafety.tracker.data.model

enum class ActivityType(val displayName: String) {
    STILL("Stationary"),
    WALKING("Walking"),
    RUNNING("Running"),
    ON_BICYCLE("Cycling"),
    IN_VEHICLE("Driving"),
    UNKNOWN("Unknown");

    companion object {
        fun fromDetectedActivity(type: Int): ActivityType {
            return when (type) {
                com.google.android.gms.location.DetectedActivity.STILL -> STILL
                com.google.android.gms.location.DetectedActivity.WALKING -> WALKING
                com.google.android.gms.location.DetectedActivity.RUNNING -> RUNNING
                com.google.android.gms.location.DetectedActivity.ON_BICYCLE -> ON_BICYCLE
                com.google.android.gms.location.DetectedActivity.IN_VEHICLE -> IN_VEHICLE
                else -> UNKNOWN
            }
        }
    }
}
