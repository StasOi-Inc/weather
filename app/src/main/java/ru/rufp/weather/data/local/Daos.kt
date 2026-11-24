package ru.rufp.weather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationsDao {

    @Query("SELECT * FROM locations ORDER BY name")
    fun all(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isCurrent = 1 LIMIT 1")
    suspend fun current(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isCurrent = 1 LIMIT 1")
    fun currentFlow(): Flow<LocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long

    @Query("UPDATE locations SET isCurrent = 0")
    suspend fun clearCurrent()

    @Query("UPDATE locations SET isCurrent = 0 WHERE id != :id")
    suspend fun clearCurrentExcept(id: Long)

    @Query("UPDATE locations SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrent(id: Long)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int

    @Query("SELECT * FROM locations ORDER BY id LIMIT 1")
    suspend fun first(): LocationEntity?
}

@Dao
interface SnapshotsDao {

    @Insert
    suspend fun insertAll(snapshots: List<ForecastSnapshotEntity>)

    @Query(
        "SELECT * FROM forecast_snapshots WHERE locationId = :locationId " +
            "AND issuedAt >= :since ORDER BY issuedAt DESC"
    )
    suspend fun recent(locationId: Long, since: Long): List<ForecastSnapshotEntity>

    @Query(
        "DELETE FROM forecast_snapshots WHERE locationId = :locationId AND issuedAt < :before"
    )
    suspend fun pruneOlderThan(locationId: Long, before: Long): Int

    @Query(
        "SELECT * FROM forecast_snapshots WHERE locationId = :locationId " +
            "AND issuedAt >= :from AND issuedAt < :to"
    )
    suspend fun forRange(locationId: Long, from: Long, to: Long): List<ForecastSnapshotEntity>
}

@Dao
interface ObservationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(observations: List<ObservationEntity>)

    @Query(
        "SELECT * FROM observations WHERE locationId = :locationId " +
            "AND targetHour >= :from AND targetHour < :to"
    )
    suspend fun forRange(locationId: Long, from: Long, to: Long): List<ObservationEntity>
}

@Dao
interface SourceStateDao {

    @Query("SELECT * FROM source_state")
    fun all(): Flow<List<SourceStateEntity>>

    @Query("SELECT * FROM source_state")
    suspend fun allOnce(): List<SourceStateEntity>

    @Upsert
    suspend fun upsert(state: SourceStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(states: List<SourceStateEntity>)
}

@Dao
interface SourceMetricsDao {

    @Query("SELECT * FROM source_metrics WHERE locationId = :locationId")
    suspend fun forLocation(locationId: Long): List<SourceMetricEntity>

    @Query("SELECT COUNT(*) FROM observations")
    suspend fun observationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metrics: List<SourceMetricEntity>)

    @Query("DELETE FROM source_metrics WHERE locationId = :locationId")
    suspend fun clearForLocation(locationId: Long)
}
