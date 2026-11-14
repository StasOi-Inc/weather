package ru.rufp.weather.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.rufp.weather.data.repository.ForecastRepository
import ru.rufp.weather.data.repository.ForecastRepositoryImpl
import ru.rufp.weather.data.repository.LocationRepository
import ru.rufp.weather.data.repository.LocationRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindForecastRepository(impl: ForecastRepositoryImpl): ForecastRepository

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
