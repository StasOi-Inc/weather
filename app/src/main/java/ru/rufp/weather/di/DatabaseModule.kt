package ru.rufp.weather.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.rufp.weather.data.local.LocationsDao
import ru.rufp.weather.data.local.ObservationsDao
import ru.rufp.weather.data.local.SnapshotsDao
import ru.rufp.weather.data.local.SourceMetricsDao
import ru.rufp.weather.data.local.SourceStateDao
import ru.rufp.weather.data.local.WeatherDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeatherDatabase =
        Room.databaseBuilder(context, WeatherDatabase::class.java, "weather.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLocationsDao(db: WeatherDatabase): LocationsDao = db.locationsDao()

    @Provides
    fun provideSnapshotsDao(db: WeatherDatabase): SnapshotsDao = db.snapshotsDao()

    @Provides
    fun provideObservationsDao(db: WeatherDatabase): ObservationsDao = db.observationsDao()

    @Provides
    fun provideSourceStateDao(db: WeatherDatabase): SourceStateDao = db.sourceStateDao()

    @Provides
    fun provideSourceMetricsDao(db: WeatherDatabase): SourceMetricsDao = db.sourceMetricsDao()
}
