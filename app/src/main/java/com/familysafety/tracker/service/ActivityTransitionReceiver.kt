package com.familysafety.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.familysafety.tracker.data.model.ActivityType

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent) ?: return
            val mostProbableActivity = result.mostProbableActivity
            val activityType = ActivityType.fromDetectedActivity(mostProbableActivity.type)

            // Send to running LocationTrackingService
            val updateIntent = Intent(LocationTrackingService.ACTION_UPDATE_ACTIVITY).apply {
                setClass(context, LocationTrackingService::class.java)
                putExtra(LocationTrackingService.EXTRA_ACTIVITY_TYPE, activityType.name)
            }
            context.startService(updateIntent)
        }
    }
}
