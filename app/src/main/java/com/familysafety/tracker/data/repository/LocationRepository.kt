package com.familysafety.tracker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.familysafety.tracker.data.local.AppDatabase
import com.familysafety.tracker.data.local.OfflineLocationEntity
import com.familysafety.tracker.data.model.ActivityType
import com.familysafety.tracker.data.model.FamilyMember
import com.familysafety.tracker.data.model.LocationPoint
import com.familysafety.tracker.sync.LocationSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val locationDao = db.locationDao()
    private val workManager = WorkManager.getInstance(context)
    private val prefs = context.getSharedPreferences("family_circle_prefs", Context.MODE_PRIVATE)

    // Observable unsynced points count
    val unsyncedCountFlow: Flow<Int> = locationDao.getUnsyncedCountFlow()

    // Active family members state flow
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers = _familyMembers.asStateFlow()

    init {
        loadFamilyCircle()
        refreshCurrentUserLocation()
    }

    private fun loadFamilyCircle() {
        val hasCustomized = prefs.getBoolean("has_customized_members", false)
        if (hasCustomized) {
            // Load saved members from prefs
            val membersJson = prefs.getString("saved_members", "") ?: ""
            if (membersJson.isNotEmpty()) {
                val list = mutableListOf<FamilyMember>()
                val items = membersJson.split("###")
                for (item in items) {
                    val parts = item.split(":::")
                    if (parts.size >= 5) {
                        list.add(
                            FamilyMember(
                                id = parts[0],
                                name = parts[1],
                                relationship = parts[2],
                                latitude = parts[3].toDoubleOrNull() ?: 22.5726,
                                longitude = parts[4].toDoubleOrNull() ?: 88.3639,
                                accuracy = 10f,
                                speedKmh = 0f,
                                bearing = 0f,
                                batteryLevel = 85,
                                isCharging = false,
                                activityType = ActivityType.STILL,
                                lastUpdated = System.currentTimeMillis() - 60_000,
                                isOnline = true,
                                isCurrentUser = false
                            )
                        )
                    }
                }
                _familyMembers.value = list
                return
            }
        }

        // Default: Kolkata demo members
        val now = System.currentTimeMillis()
        val defaultMembers = listOf(
            FamilyMember(
                id = "member_1",
                name = "Mom",
                relationship = "Mother",
                latitude = 22.5535, // Park Street, Kolkata
                longitude = 88.3512,
                accuracy = 10f,
                speedKmh = 0f,
                bearing = 0f,
                batteryLevel = 88,
                isCharging = false,
                activityType = ActivityType.STILL,
                lastUpdated = now - 120_000,
                isOnline = true
            ),
            FamilyMember(
                id = "member_2",
                name = "Dad",
                relationship = "Father",
                latitude = 22.5867, // Salt Lake, Kolkata
                longitude = 88.4178,
                accuracy = 8f,
                speedKmh = 38f,
                bearing = 95f,
                batteryLevel = 52,
                isCharging = true,
                activityType = ActivityType.IN_VEHICLE,
                lastUpdated = now - 35_000,
                isOnline = true
            ),
            FamilyMember(
                id = "member_3",
                name = "Sister",
                relationship = "Sister",
                latitude = 22.5448, // Victoria Memorial, Kolkata
                longitude = 88.3426,
                accuracy = 12f,
                speedKmh = 4.2f,
                bearing = 180f,
                batteryLevel = 24,
                isCharging = false,
                activityType = ActivityType.WALKING,
                lastUpdated = now - 15_000,
                isOnline = true
            )
        )
        _familyMembers.value = defaultMembers
    }

    private fun persistMembers() {
        val nonUserMembers = _familyMembers.value.filter { !it.isCurrentUser }
        val serialized = nonUserMembers.joinToString("###") {
            "${it.id}:::${it.name}:::${it.relationship}:::${it.latitude}:::${it.longitude}"
        }
        prefs.edit()
            .putBoolean("has_customized_members", true)
            .putString("saved_members", serialized)
            .apply()
    }

    fun addFamilyMember(name: String, relationship: String, latitude: Double, longitude: Double) {
        val newMember = FamilyMember(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            relationship = relationship,
            latitude = latitude,
            longitude = longitude,
            accuracy = 10f,
            speedKmh = 0f,
            bearing = 0f,
            batteryLevel = 100,
            isCharging = false,
            activityType = ActivityType.STILL,
            lastUpdated = System.currentTimeMillis(),
            isOnline = true,
            isCurrentUser = false
        )
        val currentList = _familyMembers.value.toMutableList()
        currentList.add(newMember)
        _familyMembers.value = currentList
        persistMembers()
    }

    fun deleteFamilyMember(memberId: String) {
        val currentList = _familyMembers.value.toMutableList()
        currentList.removeAll { it.id == memberId && !it.isCurrentUser }
        _familyMembers.value = currentList
        persistMembers()
    }

    fun clearAllDemoMembers() {
        val currentList = _familyMembers.value.filter { it.isCurrentUser }
        _familyMembers.value = currentList
        persistMembers()
    }

    @SuppressLint("MissingPermission")
    fun refreshCurrentUserLocation(onLocationFound: ((Double, Double) -> Unit)? = null) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        recordUserLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            speed = loc.speed,
                            bearing = loc.bearing,
                            batteryLevel = 100,
                            isCharging = false,
                            activityType = ActivityType.STILL
                        )
                    }
                    onLocationFound?.invoke(loc.latitude, loc.longitude)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun recordUserLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float,
        bearing: Float,
        batteryLevel: Int,
        isCharging: Boolean,
        activityType: ActivityType
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val online = isOnline()

        val entity = OfflineLocationEntity(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            activityType = activityType.name,
            timestamp = timestamp,
            isSynced = online
        )

        val insertedId = locationDao.insert(entity)

        if (online) {
            pushLocationToServer(listOf(entity.copy(id = insertedId)))
        } else {
            scheduleOfflineSync()
        }

        updateCurrentUserLocation(
            latitude, longitude, accuracy, speed * 3.6f, bearing, batteryLevel, isCharging, activityType, online
        )
    }

    private fun updateCurrentUserLocation(
        lat: Double,
        lng: Double,
        accuracy: Float,
        speedKmh: Float,
        bearing: Float,
        batteryLevel: Int,
        isCharging: Boolean,
        activityType: ActivityType,
        isOnline: Boolean
    ) {
        val currentList = _familyMembers.value.toMutableList()
        val myIndex = currentList.indexOfFirst { it.isCurrentUser }

        val me = FamilyMember(
            id = "user_me",
            name = "Me (You)",
            relationship = "Self",
            latitude = lat,
            longitude = lng,
            accuracy = accuracy,
            speedKmh = speedKmh,
            bearing = bearing,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            activityType = activityType,
            lastUpdated = System.currentTimeMillis(),
            isOnline = isOnline,
            isCurrentUser = true
        )

        if (myIndex >= 0) {
            currentList[myIndex] = me
        } else {
            currentList.add(0, me)
        }
        _familyMembers.value = currentList
    }

    fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<LocationSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "SyncOfflineLocationQueue",
            ExistingWorkPolicy.KEEP,
            syncWork
        )
    }

    suspend fun syncPendingLocations(): Int = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext 0

        val unsynced = locationDao.getUnsyncedLocations()
        if (unsynced.isEmpty()) return@withContext 0

        val success = pushLocationToServer(unsynced)
        if (success) {
            val ids = unsynced.map { it.id }
            locationDao.markAsSynced(ids)
            return@withContext ids.size
        }
        return@withContext 0
    }

    private suspend fun pushLocationToServer(points: List<OfflineLocationEntity>): Boolean {
        return true
    }

    suspend fun getRouteHistoryForDay(startTime: Long, endTime: Long): List<LocationPoint> = withContext(Dispatchers.IO) {
        val records = locationDao.getHistoryBetween(startTime, endTime)
        records.map { entity ->
            LocationPoint(
                latitude = entity.latitude,
                longitude = entity.longitude,
                accuracy = entity.accuracy,
                speed = entity.speed,
                bearing = entity.bearing,
                timestamp = entity.timestamp,
                activityType = try {
                    ActivityType.valueOf(entity.activityType)
                } catch (e: Exception) {
                    ActivityType.UNKNOWN
                }
            )
        }
    }
}
