package ru.rufp.weaser.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.rufp.weaser.data.local.LocationsDao
import ru.rufp.weaser.data.local.ObservationsDao
import ru.rufp.weaser.data.local.SnapshotsDao
import ru.rufp.weaser.data.local.SourceMetricsDao
import ru.rufp.weaser.data.local.SourceStateDao
import ru.rufp.weaser.data.local.WeaserDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeaserDatabase =
        Room.databaseBuilder(context, WeaserDatabase::class.java, "weaser.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLocationsDao(db: WeaserDatabase): LocationsDao = db.locationsDao()

    @Provides
    fun provideSnapshotsDao(db: WeaserDatabase): SnapshotsDao = db.snapshotsDao()

    @Provides
    fun provideObservationsDao(db: WeaserDatabase): ObservationsDao = db.observationsDao()

    @Provides
    fun provideSourceStateDao(db: WeaserDatabase): SourceStateDao = db.sourceStateDao()

    @Provides
    fun provideSourceMetricsDao(db: WeaserDatabase): SourceMetricsDao = db.sourceMetricsDao()
}
