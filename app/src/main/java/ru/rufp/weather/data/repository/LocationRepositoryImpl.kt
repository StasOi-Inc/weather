package ru.rufp.weather.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.rufp.weather.data.local.LocationsDao
import ru.rufp.weather.data.local.LocationEntity
import ru.rufp.weather.data.remote.OpenMeteoGeocodingApi
import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.SavedLocation
import javax.inject.Inject
import javax.inject.Singleton

data class GeoSearchResult(val name: String, val country: String, val latitude: Double, val longitude: Double)

interface LocationRepository {
    fun locations(): Flow<List<SavedLocation>>
    fun currentLocation(): Flow<SavedLocation?>
    suspend fun currentLocationOnce(): SavedLocation?
    suspend fun search(query: String): List<GeoSearchResult>
    suspend fun addLocation(result: GeoSearchResult)
    suspend fun addCurrentLocation(): Boolean
    suspend fun setCurrent(id: Long)
    suspend fun delete(id: Long)
    suspend fun ensureDefaultLocation(): Boolean
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LocationsDao,
    private val geocodingApi: OpenMeteoGeocodingApi
) : LocationRepository {

    override fun locations(): Flow<List<SavedLocation>> =
        dao.all().map { list -> list.map { it.toDomain() } }

    override fun currentLocation(): Flow<SavedLocation?> =
        dao.currentFlow().map { it?.toDomain() }

    override suspend fun currentLocationOnce(): SavedLocation? = dao.current()?.toDomain()

    override suspend fun search(query: String): List<GeoSearchResult> {
        val response = geocodingApi.search(query.trim())
        return response.results.map {
            GeoSearchResult(it.name, it.country ?: "", it.latitude, it.longitude)
        }
    }

    override suspend fun addLocation(result: GeoSearchResult) {
        val isFirst = dao.count() == 0
        val id = dao.insert(
            LocationEntity(
                name = result.name,
                country = result.country,
                latitude = result.latitude,
                longitude = result.longitude,
                isCurrent = isFirst
            )
        )
        if (!isFirst) dao.clearCurrentExcept(id)
    }

    override suspend fun addCurrentLocation(): Boolean {
        val position = findDevicePosition() ?: return false
        val roundedLat = round2(position.latitude)
        val roundedLon = round2(position.longitude)
        val name = nameByCoords(roundedLat, roundedLon)
        val id = dao.insert(
            LocationEntity(
                name = name,
                country = "",
                latitude = roundedLat,
                longitude = roundedLon,
                isCurrent = true
            )
        )
        dao.clearCurrentExcept(id)
        return true
    }

    private suspend fun nameByCoords(lat: Double, lon: Double): String {
        return runCatching {
            geocodingApi.search(name = "%.2f,%.2f".format(lat, lon), count = 1)
                .results.firstOrNull()?.name
                ?: "%.2f, %.2f".format(lat, lon)
        }.getOrDefault("%.2f, %.2f".format(lat, lon))
    }

    override suspend fun setCurrent(id: Long) {
        dao.clearCurrent()
        dao.setCurrent(id)
    }

    override suspend fun delete(id: Long) {
        val wasCurrent = dao.current()?.id == id
        dao.delete(id)
        if (wasCurrent) {
            if (dao.count() == 0) {
                ensureDefaultLocation()
            } else {
                dao.first()?.let { setCurrent(it.id) }
            }
        }
    }

    override suspend fun ensureDefaultLocation(): Boolean {
        if (dao.count() > 0) return true
        val added = runCatching {
            dao.insert(
                LocationEntity(
                    name = "Москва",
                    country = "Россия",
                    latitude = 55.75,
                    longitude = 37.62,
                    isCurrent = true
                )
            )
        }.isSuccess
        return added
    }

    private suspend fun findDevicePosition(): android.location.Location? = withContext(Dispatchers.IO) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        for (provider in providers) {
            val last = lm.getLastKnownLocation(provider)
            if (last != null) return@withContext last
        }
        null
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private fun LocationEntity.toDomain() = SavedLocation(
        id = id, name = name, country = country,
        latitude = latitude, longitude = longitude, isCurrent = isCurrent
    )
}
