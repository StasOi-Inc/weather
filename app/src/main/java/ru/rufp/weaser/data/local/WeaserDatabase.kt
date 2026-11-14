package ru.rufp.weaser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntity::class,
        ForecastSnapshotEntity::class,
        ObservationEntity::class,
        SourceStateEntity::class,
        SourceMetricEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WeaserDatabase : RoomDatabase() {
    abstract fun locationsDao(): LocationsDao
    abstract fun snapshotsDao(): SnapshotsDao
    abstract fun observationsDao(): ObservationsDao
    abstract fun sourceStateDao(): SourceStateDao
    abstract fun sourceMetricsDao(): SourceMetricsDao
}
