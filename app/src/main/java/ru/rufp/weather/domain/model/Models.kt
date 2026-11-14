package ru.rufp.weather.domain.model

import java.time.Instant

enum class PrecipType(val isPrecipitation: Boolean) {
    NONE(false),
    DRIZZLE(true),
    RAIN(true),
    HEAVY_RAIN(true),
    SNOW(true),
    WET_SNOW(true),
    FREEZING_RAIN(true),
    HAIL(true),
    THUNDERSTORM(true)
}

data class GeoPoint(val latitude: Double, val longitude: Double)

data class SavedLocation(
    val id: Long = 0,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false
)

data class HourPoint(
    val time: Instant,
    val precipProbability: Double?,
    val precipIntensity: Double?,
    val precipType: PrecipType?,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val windSpeed: Double?,
    val humidity: Double?,
    val pressure: Double?
)

data class ProviderForecast(
    val sourceId: String,
    val issuedAt: Instant,
    val hours: List<HourPoint>,
    val rawJson: String? = null
)

enum class SourceStatus { OK, ERROR, STALE }

data class SourceState(
    val sourceId: String,
    val enabled: Boolean,
    val status: SourceStatus,
    val lastSuccessAt: Instant?
)

data class SourceAccuracy(
    val sourceId: String,
    val leadBucket: Int,
    val brier: Double,
    val maeTemperature: Double,
    val hitRate: Double,
    val falseAlarmRate: Double,
    val csi: Double,
    val sampleCount: Int
)

data class AggregatedHour(
    val time: Instant,
    val probability: Double,
    val minProbability: Double,
    val maxProbability: Double,
    val spread: Double,
    val intensity: Double,
    val type: PrecipType,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val windSpeed: Double?,
    val humidity: Double?,
    val pressure: Double?
)

data class AggregatedForecast(
    val location: SavedLocation,
    val issuedAt: Instant,
    val hours: List<AggregatedHour>,
    val interval: PrecipInterval?,
    val sourcesDisagree: Boolean
)

data class PrecipInterval(
    val start: Instant,
    val end: Instant,
    val peakTime: Instant,
    val peakProbability: Double,
    val type: PrecipType
)
