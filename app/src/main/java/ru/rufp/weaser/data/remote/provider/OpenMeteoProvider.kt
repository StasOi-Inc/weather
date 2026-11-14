package ru.rufp.weaser.data.remote.provider

import ru.rufp.weaser.data.remote.OpenMeteoApi
import ru.rufp.weaser.data.remote.PrecipMappers
import ru.rufp.weaser.data.remote.TimeParsers
import ru.rufp.weaser.domain.model.GeoPoint
import ru.rufp.weaser.domain.model.HourPoint
import ru.rufp.weaser.domain.model.PrecipType
import ru.rufp.weaser.domain.model.ProviderForecast
import ru.rufp.weaser.domain.provider.WeatherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoProvider @Inject constructor(
    private val api: OpenMeteoApi
) : WeatherProvider {

    override val id: String = "open-meteo"
    override val displayName: String = "Open-Meteo"
    override val requiresKey: Boolean = false

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        val response = api.forecast(
            latitude = location.latitude,
            longitude = location.longitude,
            hourly = "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,relative_humidity_2m,wind_speed_10m,surface_pressure",
            forecastDays = 2
        )
        val h = response.hourly
        val offset = response.utcOffsetSeconds
        val now = Instant.now()
        val hours = h.time.indices.mapNotNull { i ->
            val time = TimeParsers.openMeteoTime(h.time[i], offset)
            if (time.isBefore(now.truncatedTo(ChronoUnit.HOURS))) return@mapNotNull null
            HourPoint(
                time = time,
                precipProbability = h.precipitation_probability.getOrNull(i)?.div(100.0),
                precipIntensity = h.precipitation.getOrNull(i),
                precipType = PrecipMappers.fromWmoCode(h.weather_code.getOrNull(i)),
                temperature = h.temperature_2m.getOrNull(i),
                apparentTemperature = h.apparent_temperature.getOrNull(i),
                windSpeed = h.wind_speed_10m.getOrNull(i),
                humidity = h.relative_humidity_2m.getOrNull(i)?.toDouble(),
                pressure = h.surface_pressure.getOrNull(i)?.times(0.750062)
            )
        }
        return ProviderForecast(id, Instant.now(), hours, rawJson = null)
    }
}
