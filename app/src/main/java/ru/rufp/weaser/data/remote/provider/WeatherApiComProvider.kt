package ru.rufp.weaser.data.remote.provider

import ru.rufp.weaser.data.remote.PrecipMappers
import ru.rufp.weaser.data.remote.WeatherApiComApi
import ru.rufp.weaser.domain.model.GeoPoint
import ru.rufp.weaser.domain.model.HourPoint
import ru.rufp.weaser.domain.model.ProviderForecast
import ru.rufp.weaser.domain.provider.WeatherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherApiComProvider @Inject constructor(
    private val api: WeatherApiComApi
) : WeatherProvider {

    override val id: String = "weatherapi-com"
    override val displayName: String = "WeatherAPI.com"
    override val requiresKey: Boolean = true

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        val key = requireNotNull(apiKey?.takeIf { it.isNotBlank() }) { "WeatherAPI.com key required" }
        val response = api.forecast(key, "${location.latitude},${location.longitude}")
        val now = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val limit = now.plus(24, ChronoUnit.HOURS)
        val hours = response.forecast.forecastday.flatMap { it.hour }.mapNotNull { h ->
            val time = Instant.ofEpochSecond(h.time_epoch)
            if (time.isBefore(now) || time.isAfter(limit)) return@mapNotNull null
            val snowChance = h.chance_of_snow ?: 0
            val rainChance = h.chance_of_rain ?: 0
            val probability = maxOf(rainChance, snowChance).coerceAtMost(100) / 100.0
            HourPoint(
                time = time,
                precipProbability = probability,
                precipIntensity = h.precip_mm,
                precipType = PrecipMappers.fromWeatherApiCode(h.condition.code),
                temperature = h.temp_c,
                apparentTemperature = h.feelslike_c,
                windSpeed = h.wind_kph?.div(3.6),
                humidity = h.humidity,
                pressure = h.pressure_mb?.times(0.750062)
            )
        }
        return ProviderForecast(id, Instant.now(), hours)
    }
}
