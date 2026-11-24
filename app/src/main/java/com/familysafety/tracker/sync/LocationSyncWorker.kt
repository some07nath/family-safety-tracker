package com.familysafety.tracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familysafety.tracker.FamilySafetyApp

class LocationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = FamilySafetyApp.instance.repository
        return try {
            val syncedCount = repository.syncPendingLocations()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
