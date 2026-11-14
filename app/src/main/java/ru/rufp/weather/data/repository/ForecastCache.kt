package ru.rufp.weather.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.WeatherDataStore by preferencesDataStore(name = "weather_cache")

@Serializable
data class CachedHour(
    val timeEpochMillis: Long,
    val probability: Double,
    val minProbability: Double,
    val maxProbability: Double,
    val spread: Double,
    val intensity: Double,
    val type: String,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val windSpeed: Double?,
    val humidity: Double?,
    val pressure: Double?
)

@Serializable
data class CachedForecast(
    val locationId: Long,
    val locationName: String,
    val issuedAtEpochMillis: Long,
    val intervalStart: Long?,
    val intervalEnd: Long?,
    val intervalPeakTime: Long?,
    val intervalPeakProbability: Double?,
    val intervalType: String?,
    val sourcesDisagree: Boolean,
    val hours: List<CachedHour>
)

@Singleton
class ForecastCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val key = stringPreferencesKey("last_forecast")
    private val memory = MutableStateFlow<CachedForecast?>(null)

    fun flow(): Flow<CachedForecast?> = context.WeatherDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { json.decodeFromString<CachedForecast>(raw) }.getOrNull()
        }
    }

    fun cached(): CachedForecast? = memory.value

    suspend fun save(forecast: CachedForecast) {
        memory.value = forecast
        context.WeatherDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(forecast)
        }
    }
}
