package com.familysafety.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineLocationEntity): Long

    @Query("SELECT * FROM offline_location_points WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocations(): List<OfflineLocationEntity>

    @Query("UPDATE offline_location_points SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM offline_location_points WHERE isSynced = 1 AND timestamp < :olderThanTimestamp")
    suspend fun deleteOldSyncedLocations(olderThanTimestamp: Long)

    @Query("SELECT * FROM offline_location_points ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLocationsFlow(): Flow<List<OfflineLocationEntity>>

    @Query("SELECT * FROM offline_location_points WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getHistoryBetween(startTime: Long, endTime: Long): List<OfflineLocationEntity>

    @Query("SELECT COUNT(*) FROM offline_location_points WHERE isSynced = 0")
    fun getUnsyncedCountFlow(): Flow<Int>
}
