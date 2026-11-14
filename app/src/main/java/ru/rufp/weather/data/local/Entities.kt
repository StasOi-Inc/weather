package ru.rufp.weather.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false
)

@Entity(
    tableName = "forecast_snapshots",
    indices = [Index("sourceId", "locationId", "targetHour")]
)
data class ForecastSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val locationId: Long,
    val issuedAt: Long,
    val targetHour: Long,
    val precipProbability: Double?,
    val precipType: String?,
    val precipIntensity: Double?,
    val temperature: Double?,
    val rawJson: String? = null
)

@Entity(
    tableName = "observations",
    indices = [Index(value = ["locationId", "targetHour"], unique = true)]
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: Long,
    val targetHour: Long,
    val hadPrecipitation: Boolean,
    val precipType: String?,
    val precipIntensity: Double?,
    val temperature: Double?,
    val source: String,
    val recordedAt: Long
)

@Entity(tableName = "source_state")
data class SourceStateEntity(
    @PrimaryKey val sourceId: String,
    val enabled: Boolean,
    val lastStatus: String,
    val lastSuccessAt: Long?
)

@Entity(
    tableName = "source_metrics",
    indices = [Index(value = ["sourceId", "locationId", "leadBucket"], unique = true)]
)
data class SourceMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val locationId: Long,
    val leadBucket: Int,
    val brier: Double,
    val maeTemperature: Double,
    val hitRate: Double,
    val falseAlarmRate: Double,
    val csi: Double,
    val sampleCount: Int,
    val updatedAt: Long
)
