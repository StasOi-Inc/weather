package ru.rufp.weather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocationEntity::class,
        ForecastSnapshotEntity::class,
        ObservationEntity::class,
        SourceStateEntity::class,
        SourceMetricEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationsDao(): LocationsDao
    abstract fun snapshotsDao(): SnapshotsDao
    abstract fun observationsDao(): ObservationsDao
    abstract fun sourceStateDao(): SourceStateDao
    abstract fun sourceMetricsDao(): SourceMetricsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE forecast_snapshots ADD COLUMN apparentTemperature REAL")
                db.execSQL("ALTER TABLE forecast_snapshots ADD COLUMN windSpeed REAL")
                db.execSQL("ALTER TABLE forecast_snapshots ADD COLUMN humidity REAL")
                db.execSQL("ALTER TABLE forecast_snapshots ADD COLUMN pressure REAL")
                db.execSQL("DELETE FROM source_state WHERE sourceId IN ('yandex', 'gismeteo')")
            }
        }
    }
}
